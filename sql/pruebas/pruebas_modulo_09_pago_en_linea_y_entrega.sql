-- =====================================================================
-- PRUEBAS DEL MÓDULO 9: solo pago en línea y entrega en la dirección del
-- cliente. Corre como el rol de la aplicación dentro de una transacción que
-- se deshace al final:
--   psql -h localhost -U pollos_app -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/pruebas/pruebas_modulo_09_pago_en_linea_y_entrega.sql
-- El procedimiento sp_registrar_entrega hace COMMIT, por eso las reglas se
-- prueban sobre la tabla: viven en sus triggers.
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

CREATE FUNCTION pg_temp.cli(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_cliente FROM cliente WHERE documento = p $$;
CREATE FUNCTION pg_temp.metodo(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_metodo_pago FROM metodo_pago WHERE codigo = p $$;
CREATE FUNCTION pg_temp.venta(p_doc TEXT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT v.id_venta FROM venta v JOIN pedido p ON p.id_pedido = v.id_pedido WHERE p.id_cliente = pg_temp.cli(p_doc) $$;
CREATE FUNCTION pg_temp.transportista() RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_empleado FROM empleado WHERE documento = 'E-TRA9' $$;

-- Escenario: un lote listo, dos clientes (uno sin dirección) y una venta para cada uno.
INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA PAGOS', 1000);
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-P1', id_galpon, CURRENT_DATE - 44, 100, 2500, CURRENT_DATE - 4 FROM galpon WHERE nombre = 'PRUEBA PAGOS';
UPDATE lote SET estado = 'LISTO_PARA_VENTA' WHERE codigo = 'PRU-P1';
INSERT INTO cliente (id_tipo_cliente, documento, nombre, direccion, correo)
SELECT id_tipo_cliente, 'C-DIR', 'Con dirección', 'Calle 10 # 20-30', 'con@prueba.co' FROM tipo_cliente WHERE codigo = 'TIENDA';
INSERT INTO cliente (id_tipo_cliente, documento, nombre, correo)
SELECT id_tipo_cliente, 'C-SIN', 'Sin dirección', 'sin@prueba.co' FROM tipo_cliente WHERE codigo = 'TIENDA';
INSERT INTO empleado (documento, nombres, apellidos, id_cargo, fecha_ingreso)
SELECT 'E-TRA9', 'Tomás', 'Ruta', id_cargo, CURRENT_DATE - 100 FROM cargo WHERE codigo = 'TRANSPORTISTA';

INSERT INTO pedido (id_cliente, fecha_estimada_entrega) VALUES (pg_temp.cli('C-DIR'), CURRENT_DATE + 1), (pg_temp.cli('C-SIN'), CURRENT_DATE + 1);
INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves)
SELECT p.id_pedido, l.id_lote, 10 FROM pedido p, lote l
WHERE p.id_cliente IN (pg_temp.cli('C-DIR'), pg_temp.cli('C-SIN')) AND l.codigo = 'PRU-P1';
UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_cliente IN (pg_temp.cli('C-DIR'), pg_temp.cli('C-SIN'));
INSERT INTO venta (id_pedido, peso_total_libras, precio_libra, fecha_limite_pago)
SELECT id_pedido, 60, 9000, CURRENT_DATE + 30 FROM pedido WHERE id_cliente IN (pg_temp.cli('C-DIR'), pg_temp.cli('C-SIN'));

-- =====================================================================
-- D27: el único medio de pago es Wompi
-- =====================================================================
SELECT pg_temp.verificar('solo el pago en línea queda activo',
    (SELECT array_agg(codigo) = ARRAY['EN_LINEA']::VARCHAR[] FROM metodo_pago WHERE activo));
SELECT pg_temp.esperar_error('un pago en efectivo se rechaza', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1000) $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('EFECTIVO')), 'solo se reciben en línea');
SELECT pg_temp.esperar_error('un pago por transferencia se rechaza', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1000) $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('TRANSFERENCIA')), 'solo se reciben en línea');
SELECT pg_temp.esperar_error('un pago en efectivo con referencia inventada también se rechaza', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 1000, 'TX-FALSA') $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('EFECTIVO')), 'solo se reciben en línea');
SELECT pg_temp.esperar_error('un pago en línea sin la transacción de Wompi se rechaza', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1000) $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('EN_LINEA')), 'referencia de la transacción');
SELECT pg_temp.verificar('sin pagos aprobados la venta sigue por pagar',
    (SELECT estado = 'PENDIENTE_PAGO' FROM venta WHERE id_venta = pg_temp.venta('C-DIR')));
SELECT pg_temp.esperar_ok('un abono aprobado por Wompi', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 200000, 'TX-W-1') $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('EN_LINEA')));
SELECT pg_temp.verificar('con saldo pendiente la venta todavía no está pagada',
    (SELECT estado = 'PENDIENTE_PAGO' FROM venta WHERE id_venta = pg_temp.venta('C-DIR')));
SELECT pg_temp.esperar_ok('el resto aprobado por Wompi', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 340000, 'TX-W-2') $$,
    pg_temp.venta('C-DIR'), pg_temp.metodo('EN_LINEA')));
SELECT pg_temp.verificar('la venta queda pagada solo cuando los pagos aprobados cubren el valor',
    (SELECT estado = 'PAGADA' FROM venta WHERE id_venta = pg_temp.venta('C-DIR')));
SELECT pg_temp.esperar_error('la misma transacción de Wompi no se registra dos veces', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 1000, 'TX-W-1') $$,
    pg_temp.venta('C-SIN'), pg_temp.metodo('EN_LINEA')), 'uq_pago_referencia');

-- =====================================================================
-- D28: la entrega va a la dirección del cliente
-- =====================================================================
SELECT pg_temp.esperar_ok('entrega con otra dirección escrita a mano', format($$
    INSERT INTO entrega (id_venta, id_empleado, direccion) VALUES (%s, %s, 'Otra dirección cualquiera') $$,
    pg_temp.venta('C-DIR'), pg_temp.transportista()));
SELECT pg_temp.verificar('la entrega queda con la dirección del cliente',
    (SELECT direccion = 'Calle 10 # 20-30' FROM entrega WHERE id_venta = pg_temp.venta('C-DIR')));
SELECT pg_temp.esperar_error('la dirección de la entrega no se cambia', format($$
    UPDATE entrega SET direccion = 'Calle falsa 123' WHERE id_venta = %s $$, pg_temp.venta('C-DIR')), 'no se modifica');
SELECT pg_temp.esperar_ok('la entrega se marca entregada', format($$
    UPDATE entrega SET estado = 'ENTREGADO', fecha_entrega = CURRENT_DATE WHERE id_venta = %s $$, pg_temp.venta('C-DIR')));
SELECT pg_temp.esperar_error('no se programa la entrega de un cliente sin dirección', format($$
    INSERT INTO entrega (id_venta, id_empleado, direccion) VALUES (%s, %s, 'Calle inventada') $$,
    pg_temp.venta('C-SIN'), pg_temp.transportista()), 'no tiene dirección');
SELECT pg_temp.verificar('la venta muestra la dirección del cliente',
    (SELECT direccion_cliente = 'Calle 10 # 20-30' FROM v_venta WHERE id_venta = pg_temp.venta('C-DIR')));

ROLLBACK;
