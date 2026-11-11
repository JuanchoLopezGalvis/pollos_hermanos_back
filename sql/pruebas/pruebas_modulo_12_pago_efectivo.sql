-- =====================================================================
-- PRUEBAS DEL MÓDULO 12: pago en efectivo de una venta ya registrada.
-- Corre como el rol de la aplicación dentro de una transacción que se
-- deshace al final:
--   psql -h localhost -U pollos_app -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/pruebas/pruebas_modulo_12_pago_efectivo.sql
-- Las reglas viven en el trigger de pago, por eso se prueban sobre la tabla.
-- =====================================================================

BEGIN;

SET client_min_messages = notice;
\pset tuples_only on
\pset format unaligned

CREATE FUNCTION pg_temp.fn_esperar_ok(p_caso TEXT, p_sql TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

CREATE FUNCTION pg_temp.fn_esperar_error(p_caso TEXT, p_sql TEXT, p_fragmento TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    vv_mensaje TEXT;
BEGIN
    BEGIN
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        vv_mensaje := SQLERRM;
    END;
    IF vv_mensaje IS NULL THEN
        RAISE EXCEPTION 'FALLÓ [%]: la operación debía ser rechazada.', p_caso;
    END IF;
    IF position(lower(p_fragmento) IN lower(vv_mensaje)) = 0 THEN
        RAISE EXCEPTION 'FALLÓ [%]: mensaje inesperado: %', p_caso, vv_mensaje;
    END IF;
    RAISE NOTICE 'OK   [%] rechazado: %', p_caso, vv_mensaje;
END;
$$;

CREATE FUNCTION pg_temp.fn_verificar(p_caso TEXT, p_condicion BOOLEAN) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF p_condicion IS NOT TRUE THEN
        RAISE EXCEPTION 'FALLÓ [%]', p_caso;
    END IF;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

CREATE FUNCTION pg_temp.fn_cli(p_valor TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_cliente FROM cliente WHERE documento = p_valor $$;
CREATE FUNCTION pg_temp.fn_metodo(p_valor TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_metodo_pago FROM metodo_pago WHERE codigo = p_valor $$;
CREATE FUNCTION pg_temp.fn_venta(p_doc TEXT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT v.id_venta FROM venta v JOIN pedido p ON p.id_pedido = v.id_pedido WHERE p.id_cliente = pg_temp.fn_cli(p_doc) $$;
CREATE FUNCTION pg_temp.fn_transportista() RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_empleado FROM empleado WHERE documento = 'E-TRA12' $$;

-- Escenario: un lote listo, dos clientes (uno sin dirección) y una venta para cada uno.
INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA EFECTIVO', 1000);
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-E1', id_galpon, CURRENT_DATE - 44, 100, 2500, CURRENT_DATE - 4 FROM galpon WHERE nombre = 'PRUEBA EFECTIVO';
UPDATE lote SET estado = 'LISTO_PARA_VENTA' WHERE codigo = 'PRU-E1';
INSERT INTO cliente (id_tipo_cliente, documento, nombre, direccion, correo)
SELECT id_tipo_cliente, 'C-DIR', 'Con dirección', 'Calle 10 # 20-30', 'con@prueba.co' FROM tipo_cliente WHERE codigo = 'TIENDA';
INSERT INTO cliente (id_tipo_cliente, documento, nombre, correo)
SELECT id_tipo_cliente, 'C-SIN', 'Sin dirección', 'sin@prueba.co' FROM tipo_cliente WHERE codigo = 'TIENDA';
INSERT INTO empleado (documento, nombres, apellidos, id_cargo, fecha_ingreso)
SELECT 'E-TRA12', 'Tomás', 'Ruta', id_cargo, CURRENT_DATE - 100 FROM cargo WHERE codigo = 'TRANSPORTISTA';

INSERT INTO pedido (id_cliente, fecha_estimada_entrega) VALUES (pg_temp.fn_cli('C-DIR'), CURRENT_DATE + 1), (pg_temp.fn_cli('C-SIN'), CURRENT_DATE + 1);
INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves)
SELECT p.id_pedido, l.id_lote, 10 FROM pedido p, lote l
WHERE p.id_cliente IN (pg_temp.fn_cli('C-DIR'), pg_temp.fn_cli('C-SIN')) AND l.codigo = 'PRU-E1';
UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_cliente IN (pg_temp.fn_cli('C-DIR'), pg_temp.fn_cli('C-SIN'));
INSERT INTO venta (id_pedido, peso_total_libras, precio_libra, fecha_limite_pago)
SELECT id_pedido, 60, 9000, CURRENT_DATE + 30 FROM pedido WHERE id_cliente IN (pg_temp.fn_cli('C-DIR'), pg_temp.fn_cli('C-SIN'));

-- =====================================================================
-- Pago en efectivo del saldo
-- =====================================================================
SELECT pg_temp.fn_verificar('el efectivo está activo como medio de pago',
    (SELECT activo FROM metodo_pago WHERE codigo = 'EFECTIVO'));
SELECT pg_temp.fn_esperar_error('un pago en efectivo no lleva referencia de la pasarela', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 1000, 'TX-EFE') $$,
    pg_temp.fn_venta('C-DIR'), pg_temp.fn_metodo('EFECTIVO')), 'no lleva referencia');
SELECT pg_temp.fn_esperar_error('el efectivo no supera el saldo', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 999999999) $$,
    pg_temp.fn_venta('C-DIR'), pg_temp.fn_metodo('EFECTIVO')), 'supera el saldo');
SELECT pg_temp.fn_esperar_error('el crédito sigue sin aceptarse', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1000) $$,
    pg_temp.fn_venta('C-DIR'), pg_temp.fn_metodo('CREDITO')), 'en línea con Wompi o en efectivo');
SELECT pg_temp.fn_esperar_ok('el saldo completo se paga en efectivo', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, fn_saldo_venta(%s)) $$,
    pg_temp.fn_venta('C-DIR'), pg_temp.fn_metodo('EFECTIVO'), pg_temp.fn_venta('C-DIR')));
SELECT pg_temp.fn_verificar('pagado el saldo, la venta queda pagada',
    (SELECT estado = 'PAGADA' FROM venta WHERE id_venta = pg_temp.fn_venta('C-DIR')));
SELECT pg_temp.fn_esperar_error('una venta pagada no recibe otro pago en efectivo', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1000) $$,
    pg_temp.fn_venta('C-DIR'), pg_temp.fn_metodo('EFECTIVO')), 'ya está pagada');
SELECT pg_temp.fn_esperar_ok('un abono en efectivo deja saldo pendiente', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 100000) $$,
    pg_temp.fn_venta('C-SIN'), pg_temp.fn_metodo('EFECTIVO')));
SELECT pg_temp.fn_verificar('con saldo pendiente la venta sigue por pagar',
    (SELECT estado = 'PENDIENTE_PAGO' FROM venta WHERE id_venta = pg_temp.fn_venta('C-SIN')));
SELECT pg_temp.fn_verificar('el pago en efectivo aparece en la vista de pagos',
    (SELECT count(*) = 1 FROM v_pago WHERE id_venta = pg_temp.fn_venta('C-DIR') AND referencia_pasarela IS NULL));

ROLLBACK;
