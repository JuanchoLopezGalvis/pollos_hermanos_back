-- =====================================================================
-- PRUEBAS DEL MÓDULO 8: portal de clientes.
--
-- Prueba el reparto del pedido por antigüedad (FIFO), el rechazo cuando no
-- hay existencias, que los pedidos pendientes aparten aves y la cuenta del
-- cliente. Corre como el rol de la aplicación dentro de una transacción que
-- se deshace al final:
--   psql -h localhost -U pollos_app -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/pruebas/pruebas_modulo_08_portal_clientes.sql
-- El procedimiento sp_crear_pedido_cliente hace COMMIT, por eso se prueba la
-- función fn_repartir_pedido_fifo, que es la que reparte.
-- Los lotes de la finca que ya estén listos para venta siguen contando: las
-- pruebas comparan contra las aves libres que había al empezar.
-- =====================================================================

BEGIN;

SET client_min_messages = notice;
\pset tuples_only on
\pset format unaligned

CREATE FUNCTION pg_temp.esperar_ok(p_caso TEXT, p_sql TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

CREATE FUNCTION pg_temp.esperar_error(p_caso TEXT, p_sql TEXT, p_fragmento TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_mensaje TEXT;
BEGIN
    BEGIN
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        v_mensaje := SQLERRM;
    END;
    IF v_mensaje IS NULL THEN
        RAISE EXCEPTION 'FALLÓ [%]: la operación debía ser rechazada.', p_caso;
    END IF;
    IF position(lower(p_fragmento) IN lower(v_mensaje)) = 0 THEN
        RAISE EXCEPTION 'FALLÓ [%]: mensaje inesperado: %', p_caso, v_mensaje;
    END IF;
    RAISE NOTICE 'OK   [%] rechazado: %', p_caso, v_mensaje;
END;
$$;

CREATE FUNCTION pg_temp.verificar(p_caso TEXT, p_condicion BOOLEAN) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF p_condicion IS NOT TRUE THEN
        RAISE EXCEPTION 'FALLÓ [%]', p_caso;
    END IF;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

CREATE FUNCTION pg_temp.lote(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_lote FROM lote WHERE codigo = p $$;
CREATE FUNCTION pg_temp.cli(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_cliente FROM cliente WHERE documento = p $$;
CREATE FUNCTION pg_temp.aves(p_pedido BIGINT, p_lote TEXT) RETURNS INTEGER LANGUAGE sql AS
    $$ SELECT cantidad_aves FROM detalle_pedido WHERE id_pedido = p_pedido AND id_lote = pg_temp.lote(p_lote) $$;

-- Aves libres de la finca antes de crear los lotes de prueba.
CREATE TEMP TABLE base AS SELECT fn_aves_libres_venta() AS libres;
CREATE FUNCTION pg_temp.base() RETURNS INTEGER LANGUAGE sql AS $$ SELECT libres FROM base $$;

-- Escenario: dos lotes listos para venta con fecha estimada de venta ya pasada,
-- así son los más antiguos de la finca. PRU-F1 es más antiguo que PRU-F2.
INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA FIFO', 2000);
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-F1', id_galpon, CURRENT_DATE - 44, 100, 2500, CURRENT_DATE - 4 FROM galpon WHERE nombre = 'PRUEBA FIFO';
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-F2', id_galpon, CURRENT_DATE - 42, 100, 2500, CURRENT_DATE - 2 FROM galpon WHERE nombre = 'PRUEBA FIFO';
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-F3', id_galpon, CURRENT_DATE - 20, 100, 2500, CURRENT_DATE + 20 FROM galpon WHERE nombre = 'PRUEBA FIFO';
UPDATE lote SET estado = 'LISTO_PARA_VENTA' WHERE codigo IN ('PRU-F1', 'PRU-F2');

INSERT INTO cliente (id_tipo_cliente, documento, nombre, correo)
SELECT id_tipo_cliente, 'C-PORTAL', 'Cliente del portal', 'portal@prueba.co' FROM tipo_cliente WHERE codigo = 'TIENDA';

-- =====================================================================
-- Aves libres para la venta
-- =====================================================================
SELECT pg_temp.verificar('las aves libres suman los lotes listos (no el que está en engorde)',
    fn_aves_libres_venta() = pg_temp.base() + 200);

-- =====================================================================
-- D22: reparto por antigüedad
-- =====================================================================
INSERT INTO pedido (id_cliente, fecha_estimada_entrega) VALUES
    (pg_temp.cli('C-PORTAL'), CURRENT_DATE + 2), (pg_temp.cli('C-PORTAL'), CURRENT_DATE + 3);
CREATE FUNCTION pg_temp.pedido(n INT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT id_pedido FROM pedido WHERE id_cliente = pg_temp.cli('C-PORTAL') ORDER BY id_pedido OFFSET n - 1 LIMIT 1 $$;

SELECT pg_temp.verificar('un pedido de 150 aves usa dos lotes',
    fn_repartir_pedido_fifo(pg_temp.pedido(1), 150) = 2);
SELECT pg_temp.verificar('primero se vacía el lote más antiguo (PRU-F1: 100 aves)',
    pg_temp.aves(pg_temp.pedido(1), 'PRU-F1') = 100);
SELECT pg_temp.verificar('el resto sale del siguiente lote (PRU-F2: 50 aves)',
    pg_temp.aves(pg_temp.pedido(1), 'PRU-F2') = 50);
SELECT pg_temp.verificar('el lote en engorde no se toca',
    NOT EXISTS (SELECT 1 FROM detalle_pedido WHERE id_lote = pg_temp.lote('PRU-F3')));

-- =====================================================================
-- D23: un pedido pendiente aparta sus aves
-- =====================================================================
SELECT pg_temp.verificar('el pedido sigue pendiente', (SELECT estado = 'PENDIENTE' FROM pedido WHERE id_pedido = pg_temp.pedido(1)));
SELECT pg_temp.verificar('las aves del pedido pendiente quedan apartadas (D23)',
    fn_aves_comprometidas(pg_temp.lote('PRU-F1')) = 100 AND fn_aves_libres_lote(pg_temp.lote('PRU-F1')) = 0);
SELECT pg_temp.verificar('quedan libres 50 aves de los lotes de prueba', fn_aves_libres_venta() = pg_temp.base() + 50);
SELECT pg_temp.esperar_error('otro pedido no puede tomar a mano las aves apartadas', format($$
    INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves) VALUES (%s, %s, 1) $$,
    pg_temp.pedido(2), pg_temp.lote('PRU-F1')), 'aves libres');

-- =====================================================================
-- Sin existencias suficientes: rechazo inmediato
-- =====================================================================
SELECT pg_temp.esperar_error('pedido mayor que las aves libres',
    format($$ SELECT fn_repartir_pedido_fifo(%s, %s) $$, pg_temp.pedido(2), pg_temp.base() + 51), 'Solo hay');
SELECT pg_temp.verificar('el pedido rechazado no aparta ninguna ave',
    NOT EXISTS (SELECT 1 FROM detalle_pedido WHERE id_pedido = pg_temp.pedido(2)));
SELECT pg_temp.esperar_error('pedido de cero aves',
    format($$ SELECT fn_repartir_pedido_fifo(%s, 0) $$, pg_temp.pedido(2)), 'mayor que cero');
SELECT pg_temp.verificar('el segundo pedido toma lo que quedó del lote PRU-F2',
    fn_repartir_pedido_fifo(pg_temp.pedido(2), 50) >= 1 AND pg_temp.aves(pg_temp.pedido(2), 'PRU-F2') = 50);

-- =====================================================================
-- Confirmar y cancelar
-- =====================================================================
SELECT pg_temp.esperar_ok('el trabajador aprueba el pedido', format($$
    UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_pedido = %s $$, pg_temp.pedido(1)));
SELECT pg_temp.verificar('el pedido aprobado sigue apartando sus aves',
    fn_aves_comprometidas(pg_temp.lote('PRU-F1')) = 100);
SELECT pg_temp.esperar_ok('el cliente cancela su pedido pendiente', format($$
    UPDATE pedido SET estado = 'CANCELADO' WHERE id_pedido = %s $$, pg_temp.pedido(2)));
SELECT pg_temp.verificar('al cancelar se liberan sus aves',
    fn_aves_libres_lote(pg_temp.lote('PRU-F2')) = 50);

-- =====================================================================
-- D21: cuenta del cliente
-- =====================================================================
INSERT INTO usuario (nombre_usuario, correo, contrasena) VALUES ('cliente_prueba', 'portal@prueba.co', 'x');
SELECT pg_temp.esperar_ok('vincular el usuario al cliente', $$
    UPDATE cliente SET id_usuario = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cliente_prueba')
    WHERE documento = 'C-PORTAL' $$);
SELECT pg_temp.verificar('la vista muestra la cuenta del cliente',
    (SELECT usuario = 'cliente_prueba' FROM v_cliente WHERE documento = 'C-PORTAL'));
SELECT pg_temp.esperar_error('un usuario no se vincula a dos clientes', $$
    INSERT INTO cliente (id_tipo_cliente, documento, nombre, telefono, id_usuario)
    SELECT t.id_tipo_cliente, 'C-OTRO', 'Otro', '3999999999', u.id_usuario
    FROM tipo_cliente t, usuario u WHERE t.codigo = 'TIENDA' AND u.nombre_usuario = 'cliente_prueba' $$, 'uq_cliente_usuario');
SELECT pg_temp.esperar_error('el usuario vinculado debe existir', $$
    UPDATE cliente SET id_usuario = -1 WHERE documento = 'C-PORTAL' $$, 'fk_cliente_usuario');
SELECT pg_temp.verificar('el mensaje del usuario repetido está en español',
    fn_mensaje_restriccion('uq_cliente_usuario', '') LIKE 'Ese usuario ya está vinculado%');

-- =====================================================================
-- D24: el rol Cliente solo entra al portal
-- =====================================================================
SELECT pg_temp.verificar('el rol Cliente tiene un único permiso: portal.cliente',
    (SELECT array_agg(p.codigo) = ARRAY['portal.cliente']::VARCHAR[]
     FROM rol r JOIN rol_permiso rp ON rp.id_rol = r.id_rol JOIN permiso p ON p.id_permiso = rp.id_permiso
     WHERE r.nombre = 'Cliente'));

ROLLBACK;
