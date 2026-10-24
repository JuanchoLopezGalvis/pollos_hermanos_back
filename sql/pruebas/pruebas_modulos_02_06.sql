-- =====================================================================
-- PRUEBAS DE LOS MÓDULOS 2 A 6: personal, alimentación, sanidad, gastos y
-- beneficio, y comercial.
--
-- Recorre un lote desde la compra de alimento hasta la venta pagada y
-- entregada, probando cada regla con un caso válido y uno inválido. Corre como
-- el rol de la aplicación dentro de una transacción que se deshace al final:
--   psql -h localhost -U pollos_app -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/pruebas/pruebas_modulos_02_06.sql
-- Los procedimientos hacen COMMIT, por eso las reglas se prueban sobre las
-- tablas: viven en sus triggers y restricciones.
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

-- Atajos para encontrar los datos de prueba.
CREATE FUNCTION pg_temp.lote(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_lote FROM lote WHERE codigo = p $$;
CREATE FUNCTION pg_temp.emp(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_empleado FROM empleado WHERE documento = p $$;
CREATE FUNCTION pg_temp.cargo(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_cargo FROM cargo WHERE codigo = p $$;
CREATE FUNCTION pg_temp.prov(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_proveedor FROM proveedor WHERE nit = p $$;
CREATE FUNCTION pg_temp.alim(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_tipo_alimento FROM tipo_alimento WHERE nombre = p $$;
CREATE FUNCTION pg_temp.prod(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_producto FROM producto_sanitario WHERE nombre = p $$;
CREATE FUNCTION pg_temp.cat(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_categoria_gasto FROM categoria_gasto WHERE codigo = p $$;
CREATE FUNCTION pg_temp.cli(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_cliente FROM cliente WHERE documento = p $$;
CREATE FUNCTION pg_temp.metodo(p TEXT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_metodo_pago FROM metodo_pago WHERE codigo = p $$;

-- Escenario base: el lote PRU-A tiene 41 días (finalización) y el PRU-B 11 (iniciación).
INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA GA', 2000), ('PRUEBA GB', 2000);
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-A', id_galpon, CURRENT_DATE - 40, 1000, 2500, CURRENT_DATE + 5 FROM galpon WHERE nombre = 'PRUEBA GA';
INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
SELECT 'PRU-B', id_galpon, CURRENT_DATE - 10, 500, 2500, CURRENT_DATE + 35 FROM galpon WHERE nombre = 'PRUEBA GB';
INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (pg_temp.lote('PRU-A'), CURRENT_DATE - 3, 10);

-- =====================================================================
-- Módulo 2: personal
-- =====================================================================
SELECT pg_temp.esperar_ok('empleados de cada cargo', format($$
    INSERT INTO empleado (documento, nombres, apellidos, id_cargo, fecha_ingreso) VALUES
      ('E-SAN', 'Sara', 'Sanidad', %s, CURRENT_DATE - 100),
      ('E-TRA', 'Tomás', 'Transporte', %s, CURRENT_DATE - 100),
      ('E-BEN1', 'Berta', 'Uno', %s, CURRENT_DATE - 100),
      ('E-BEN2', 'Blanca', 'Dos', %s, CURRENT_DATE - 100),
      ('E-BEN3', 'Beatriz', 'Tres', %s, CURRENT_DATE - 100),
      ('E-OPE', 'Óscar', 'Operario', %s, CURRENT_DATE - 100) $$,
    pg_temp.cargo('ENCARGADO_SANIDAD'), pg_temp.cargo('TRANSPORTISTA'), pg_temp.cargo('BENEFICIADORA'),
    pg_temp.cargo('BENEFICIADORA'), pg_temp.cargo('BENEFICIADORA'), pg_temp.cargo('OPERARIO_GALPON')));
SELECT pg_temp.esperar_error('documento de empleado repetido', format($$
    INSERT INTO empleado (documento, nombres, apellidos, id_cargo, fecha_ingreso)
    VALUES ('E-SAN', 'Otra', 'Persona', %s, CURRENT_DATE) $$, pg_temp.cargo('OPERARIO_GALPON')), 'uq_empleado_documento');
SELECT pg_temp.esperar_error('retiro sin fecha de retiro',
    $$ UPDATE empleado SET activo = FALSE WHERE documento = 'E-OPE' $$, 'ck_empleado_retiro');
SELECT pg_temp.esperar_ok('retiro de un empleado (S27)',
    $$ UPDATE empleado SET activo = FALSE, fecha_retiro = CURRENT_DATE WHERE documento = 'E-OPE' $$);
SELECT pg_temp.esperar_error('un retirado no se reactiva',
    $$ UPDATE empleado SET activo = TRUE, fecha_retiro = NULL WHERE documento = 'E-OPE' $$, 'no puede reactivarse');
SELECT pg_temp.esperar_error('un retirado no se asigna a nuevos registros',
    format($$ SELECT fn_validar_empleado(%s, 'OPERARIO_GALPON') $$, pg_temp.emp('E-OPE')), 'retirado');

-- =====================================================================
-- Módulo 3: proveedores y alimentación
-- =====================================================================
SELECT pg_temp.esperar_ok('proveedores de alimento y de sanidad',
    $$ INSERT INTO proveedor (nit, nombre, suministra_alimento, suministra_sanidad) VALUES
       ('P-ALI', 'Concentrados', TRUE, FALSE), ('P-SAN', 'Veterinaria', FALSE, TRUE) $$);
SELECT pg_temp.esperar_error('proveedor que no suministra nada (S21)',
    $$ INSERT INTO proveedor (nit, nombre) VALUES ('P-NADA', 'Ninguno') $$, 'ck_proveedor_suministra');
SELECT pg_temp.esperar_error('compra de alimento a un proveedor de sanidad', format($$
    INSERT INTO compra_alimento (id_proveedor, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
    VALUES (%s, %s, CURRENT_DATE, 10, 100000) $$, pg_temp.prov('P-SAN'), pg_temp.alim('Finalizador')), 'no suministra alimento');
SELECT pg_temp.esperar_ok('compras de alimento (S15)', format($$
    INSERT INTO compra_alimento (id_proveedor, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario) VALUES
      (%1$s, %2$s, CURRENT_DATE - 5, 100, 100000), (%1$s, %2$s, CURRENT_DATE - 1, 50, 130000) $$,
    pg_temp.prov('P-ALI'), pg_temp.alim('Finalizador')));
SELECT pg_temp.verificar('costo promedio ponderado del bulto = 110.000',
    fn_costo_promedio_alimento(pg_temp.alim('Finalizador'), CURRENT_DATE) = 110000);
SELECT pg_temp.esperar_ok('consumo del alimento de su etapa', format($$
    INSERT INTO consumo_alimento (id_lote, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
    VALUES (%s, %s, CURRENT_DATE, 10, 0) $$, pg_temp.lote('PRU-A'), pg_temp.alim('Finalizador')));
SELECT pg_temp.verificar('el consumo queda valorado al promedio ponderado (D8)',
    (SELECT costo_unitario = 110000 FROM consumo_alimento WHERE id_lote = pg_temp.lote('PRU-A')));
SELECT pg_temp.verificar('inventario = comprados - consumidos (S16)',
    fn_inventario_alimento(pg_temp.alim('Finalizador')) = 140);
SELECT pg_temp.esperar_error('alimento que no corresponde a la edad del lote', format($$
    INSERT INTO consumo_alimento (id_lote, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
    VALUES (%s, %s, CURRENT_DATE, 1, 0) $$, pg_temp.lote('PRU-B'), pg_temp.alim('Finalizador')), 'no corresponde');
SELECT pg_temp.esperar_error('consumo mayor que el inventario (S16)', format($$
    INSERT INTO consumo_alimento (id_lote, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
    VALUES (%s, %s, CURRENT_DATE - 1, 141, 0) $$, pg_temp.lote('PRU-A'), pg_temp.alim('Finalizador')), 'supera el inventario');
SELECT pg_temp.esperar_error('la aplicación no modifica consumos registrados',
    $$ UPDATE consumo_alimento SET cantidad_bultos = 1 $$, 'permission denied');

-- =====================================================================
-- Módulo 4: sanidad
-- =====================================================================
SELECT pg_temp.esperar_error('producto de un proveedor que no es de sanidad (S18)', format($$
    INSERT INTO producto_sanitario (nombre, tipo, presentacion, unidad_medida, id_proveedor)
    VALUES ('Vitamina X', 'VITAMINA', 'Frasco 1 L', 'ml', %s) $$, pg_temp.prov('P-ALI')), 'no suministra insumos sanitarios');
SELECT pg_temp.esperar_ok('producto sanitario', format($$
    INSERT INTO producto_sanitario (nombre, tipo, presentacion, unidad_medida, id_proveedor)
    VALUES ('Vitamina AD3E', 'VITAMINA', 'Frasco 1 L', 'ml', %s) $$, pg_temp.prov('P-SAN')));
SELECT pg_temp.esperar_error('aplicación de un producto sin compras', format($$
    INSERT INTO aplicacion_sanitaria (id_lote, id_producto, id_empleado, fecha, dosis, costo_unitario)
    VALUES (%s, %s, %s, CURRENT_DATE, 2, 0) $$, pg_temp.lote('PRU-A'), pg_temp.prod('Vitamina AD3E'), pg_temp.emp('E-SAN')),
    'No hay compras de ese producto');
SELECT pg_temp.esperar_ok('compra de vitamina', format($$
    INSERT INTO compra_sanitaria (id_producto, fecha, cantidad, costo_unitario) VALUES (%s, CURRENT_DATE - 2, 10, 5000) $$,
    pg_temp.prod('Vitamina AD3E')));
SELECT pg_temp.esperar_ok('aplicación por el encargado de sanidad (S19, S25)', format($$
    INSERT INTO aplicacion_sanitaria (id_lote, id_producto, id_empleado, fecha, dosis, costo_unitario)
    VALUES (%s, %s, %s, CURRENT_DATE, 2, 0) $$, pg_temp.lote('PRU-A'), pg_temp.prod('Vitamina AD3E'), pg_temp.emp('E-SAN')));
SELECT pg_temp.esperar_error('aplicación registrada por un transportista (S25)', format($$
    INSERT INTO aplicacion_sanitaria (id_lote, id_producto, id_empleado, fecha, dosis, costo_unitario)
    VALUES (%s, %s, %s, CURRENT_DATE, 1, 0) $$, pg_temp.lote('PRU-A'), pg_temp.prod('Vitamina AD3E'), pg_temp.emp('E-TRA')),
    'no tiene el cargo de encargado de sanidad');

-- =====================================================================
-- Módulo 5: gastos, beneficio y costo
-- =====================================================================
SELECT pg_temp.esperar_ok('gasto del lote y gasto general (S22)', format($$
    INSERT INTO gasto (id_categoria_gasto, id_lote, fecha, monto, descripcion) VALUES
      (%s, %s, CURRENT_DATE, 200000, 'Jornal de limpieza'), (%s, NULL, CURRENT_DATE, 50000, 'Energía de la finca') $$,
    pg_temp.cat('MANO_OBRA'), pg_temp.lote('PRU-A'), pg_temp.cat('SERVICIOS_PUBLICOS')));
SELECT pg_temp.esperar_error('gasto en cero', format($$
    INSERT INTO gasto (id_categoria_gasto, fecha, monto, descripcion) VALUES (%s, CURRENT_DATE, 0, 'x') $$,
    pg_temp.cat('OTROS')), 'ck_gasto_monto');
SELECT pg_temp.esperar_error('gasto anterior al ingreso del lote', format($$
    INSERT INTO gasto (id_categoria_gasto, id_lote, fecha, monto, descripcion) VALUES (%s, %s, CURRENT_DATE - 60, 1000, 'x') $$,
    pg_temp.cat('OTROS'), pg_temp.lote('PRU-A')), 'anterior al ingreso');
SELECT pg_temp.esperar_error('beneficio de un lote en engorde', format($$
    INSERT INTO jornada_beneficio (id_lote, fecha) VALUES (%s, CURRENT_DATE) $$, pg_temp.lote('PRU-B')), 'listos para venta');

UPDATE lote SET estado = 'LISTO_PARA_VENTA' WHERE codigo = 'PRU-A';
INSERT INTO jornada_beneficio (id_lote, fecha) VALUES (pg_temp.lote('PRU-A'), CURRENT_DATE);
CREATE FUNCTION pg_temp.jornada() RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT id_jornada FROM jornada_beneficio WHERE id_lote = pg_temp.lote('PRU-A') $$;

SELECT pg_temp.esperar_ok('beneficiadoras pagadas por pollo', format($$
    INSERT INTO detalle_beneficio (id_jornada, id_empleado, aves_beneficiadas, tarifa_por_ave) VALUES
      (%1$s, %2$s, 300, 900), (%1$s, %3$s, 200, 900) $$, pg_temp.jornada(), pg_temp.emp('E-BEN1'), pg_temp.emp('E-BEN2')));
SELECT pg_temp.esperar_error('una encargada de sanidad no aparece como beneficiadora', format($$
    INSERT INTO detalle_beneficio (id_jornada, id_empleado, aves_beneficiadas, tarifa_por_ave) VALUES (%s, %s, 10, 900) $$,
    pg_temp.jornada(), pg_temp.emp('E-SAN')), 'no tiene el cargo de beneficiadora');
SELECT pg_temp.esperar_error('más aves beneficiadas que aves vivas', format($$
    INSERT INTO detalle_beneficio (id_jornada, id_empleado, aves_beneficiadas, tarifa_por_ave) VALUES (%s, %s, 491, 900) $$,
    pg_temp.jornada(), pg_temp.emp('E-BEN3')), 'aves vivas');
SELECT pg_temp.verificar('costo del beneficio = suma de los pagos (450.000)',
    fn_costo_beneficio_lote(pg_temp.lote('PRU-A')) = 450000);
SELECT pg_temp.verificar('costo del lote con sus 5 componentes (D7: sin el gasto general)',
    (SELECT costo_pollitos = 2500000 AND costo_alimento = 1100000 AND costo_sanidad = 10000
            AND costo_beneficio = 450000 AND otros_gastos = 200000 AND costo_total = 4260000
     FROM fn_costo_lote(pg_temp.lote('PRU-A'))));

-- =====================================================================
-- Módulo 6: comercial
-- =====================================================================
SELECT pg_temp.esperar_ok('cliente con contacto (S28, S29)', format($$
    INSERT INTO cliente (id_tipo_cliente, documento, nombre, direccion, telefono)
    SELECT id_tipo_cliente, 'C-001', 'Asadero El Pollo', 'Calle 1 # 2-3', '3000000001' FROM tipo_cliente WHERE codigo = 'RESTAURANTE' $$));
SELECT pg_temp.esperar_error('cliente sin teléfono ni correo (S29)',
    $$ INSERT INTO cliente (id_tipo_cliente, documento, nombre)
       SELECT id_tipo_cliente, 'C-002', 'Sin contacto' FROM tipo_cliente WHERE codigo = 'TIENDA' $$, 'ck_cliente_contacto');
SELECT pg_temp.esperar_error('cliente con teléfono repetido (S29)',
    $$ INSERT INTO cliente (id_tipo_cliente, documento, nombre, telefono)
       SELECT id_tipo_cliente, 'C-003', 'Otro', '3000000001' FROM tipo_cliente WHERE codigo = 'TIENDA' $$, 'uq_cliente_telefono');

INSERT INTO pedido (id_cliente, fecha_estimada_entrega) VALUES (pg_temp.cli('C-001'), CURRENT_DATE + 2), (pg_temp.cli('C-001'), CURRENT_DATE + 3);
CREATE FUNCTION pg_temp.pedido(n INT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT id_pedido FROM pedido WHERE id_cliente = pg_temp.cli('C-001') ORDER BY id_pedido OFFSET n - 1 LIMIT 1 $$;

SELECT pg_temp.esperar_ok('pedido con aves de un lote listo (S32)', format($$
    INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves) VALUES (%s, %s, 600) $$, pg_temp.pedido(1), pg_temp.lote('PRU-A')));
SELECT pg_temp.esperar_error('pedido de un lote en engorde', format($$
    INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves) VALUES (%s, %s, 10) $$, pg_temp.pedido(1), pg_temp.lote('PRU-B')),
    'listos para venta');
SELECT pg_temp.esperar_error('venta de un pedido pendiente (S33)', format($$
    INSERT INTO venta (id_pedido, peso_total_libras, precio_libra, fecha_limite_pago) VALUES (%s, 1500, 9000, CURRENT_DATE + 30) $$,
    pg_temp.pedido(1)), 'pedido confirmado');
SELECT pg_temp.esperar_ok('confirmar el pedido', format($$ UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_pedido = %s $$, pg_temp.pedido(1)));
SELECT pg_temp.verificar('las aves del pedido confirmado quedan comprometidas',
    fn_aves_comprometidas(pg_temp.lote('PRU-A')) = 600);
SELECT pg_temp.esperar_error('otro pedido que supera las aves libres', format($$
    INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves) VALUES (%s, %s, 391) $$, pg_temp.pedido(2), pg_temp.lote('PRU-A')),
    'aves libres');
SELECT pg_temp.esperar_ok('otro pedido con las aves libres restantes', format($$
    INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves) VALUES (%s, %s, 390) $$, pg_temp.pedido(2), pg_temp.lote('PRU-A')));

SELECT pg_temp.esperar_ok('venta del pedido confirmado (S33)', format($$
    INSERT INTO venta (id_pedido, peso_total_libras, precio_libra, fecha_limite_pago) VALUES (%s, 1500, 9000, CURRENT_DATE + 30) $$,
    pg_temp.pedido(1)));
CREATE FUNCTION pg_temp.venta(n INT) RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_venta FROM venta WHERE id_pedido = pg_temp.pedido(n) $$;
SELECT pg_temp.verificar('valor total = libras x precio por libra (13.500.000)',
    (SELECT valor_total = 13500000 FROM venta WHERE id_venta = pg_temp.venta(1)));
SELECT pg_temp.verificar('aves disponibles descuentan las vendidas (S11)', fn_aves_disponibles(pg_temp.lote('PRU-A')) = 390);
SELECT pg_temp.esperar_error('segunda venta del mismo pedido (D5)', format($$
    INSERT INTO venta (id_pedido, peso_total_libras, precio_libra, fecha_limite_pago) VALUES (%s, 1, 1, CURRENT_DATE) $$,
    pg_temp.pedido(1)), 'ya tiene una venta registrada');
SELECT pg_temp.esperar_error('el precio por libra no cambia después de la venta (S34)', format($$
    UPDATE venta SET precio_libra = 10000 WHERE id_venta = %s $$, pg_temp.venta(1)), 'no se modifica');
SELECT pg_temp.esperar_error('no se cancela un pedido con venta', format($$
    UPDATE pedido SET estado = 'CANCELADO' WHERE id_pedido = %s $$, pg_temp.pedido(1)), 'ya tiene una venta');

SELECT pg_temp.esperar_ok('pago parcial en efectivo (S35)', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 5000000) $$, pg_temp.venta(1), pg_temp.metodo('EFECTIVO')));
SELECT pg_temp.verificar('saldo = valor total - pagos (S37)', fn_saldo_venta(pg_temp.venta(1)) = 8500000);
SELECT pg_temp.esperar_error('pago mayor que el saldo', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 8500001) $$, pg_temp.venta(1), pg_temp.metodo('EFECTIVO')),
    'supera el saldo');
SELECT pg_temp.esperar_error('pago en línea sin referencia de la pasarela', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 100) $$, pg_temp.venta(1), pg_temp.metodo('EN_LINEA')),
    'referencia de la transacción');
SELECT pg_temp.esperar_ok('pago en línea aprobado con su referencia', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto, referencia_pasarela) VALUES (%s, %s, 8500000, 'TX-PRUEBA-1') $$,
    pg_temp.venta(1), pg_temp.metodo('EN_LINEA')));
SELECT pg_temp.verificar('con el saldo en cero la venta queda pagada',
    (SELECT estado = 'PAGADA' FROM venta WHERE id_venta = pg_temp.venta(1)));
SELECT pg_temp.esperar_error('pago de una venta ya pagada', format($$
    INSERT INTO pago (id_venta, id_metodo_pago, monto) VALUES (%s, %s, 1) $$, pg_temp.venta(1), pg_temp.metodo('EFECTIVO')),
    'ya está pagada');

SELECT pg_temp.esperar_error('entrega a cargo de quien no es transportista (S26)', format($$
    INSERT INTO entrega (id_venta, id_empleado, direccion) VALUES (%s, %s, 'Calle 1') $$, pg_temp.venta(1), pg_temp.emp('E-SAN')),
    'no tiene el cargo de transportista');
SELECT pg_temp.esperar_ok('entrega a cargo de un transportista', format($$
    INSERT INTO entrega (id_venta, id_empleado, direccion) VALUES (%s, %s, 'Calle 1 # 2-3') $$, pg_temp.venta(1), pg_temp.emp('E-TRA')));
SELECT pg_temp.esperar_error('segunda entrega de la misma venta (S39)', format($$
    INSERT INTO entrega (id_venta, id_empleado, direccion) VALUES (%s, %s, 'Calle 2') $$, pg_temp.venta(1), pg_temp.emp('E-TRA')),
    'uq_entrega_venta');
SELECT pg_temp.esperar_error('entrega realizada sin fecha', format($$
    UPDATE entrega SET estado = 'ENTREGADO' WHERE id_venta = %s $$, pg_temp.venta(1)), 'ck_entrega_fecha');
SELECT pg_temp.esperar_ok('entrega realizada (S41)', format($$
    UPDATE entrega SET estado = 'ENTREGADO', fecha_entrega = CURRENT_DATE WHERE id_venta = %s $$, pg_temp.venta(1)));
SELECT pg_temp.verificar('el pedido queda entregado', (SELECT estado = 'ENTREGADO' FROM pedido WHERE id_pedido = pg_temp.pedido(1)));

-- Segunda venta con las aves restantes: el lote se vende completo y vence.
UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_pedido = pg_temp.pedido(2);
SELECT pg_temp.esperar_ok('venta de las últimas aves del lote', format($$
    INSERT INTO venta (id_pedido, fecha_venta, peso_total_libras, precio_libra, fecha_limite_pago)
    VALUES (%s, CURRENT_DATE - 5, 975, 9000, CURRENT_DATE - 1) $$, pg_temp.pedido(2)));
SELECT pg_temp.verificar('el lote sin aves pasa a vendido (S12)', (SELECT estado = 'VENDIDO' FROM lote WHERE codigo = 'PRU-A'));
SELECT pg_temp.verificar('su galpón queda disponible', (SELECT estado = 'DISPONIBLE' FROM galpon WHERE nombre = 'PRUEBA GA'));
SELECT pg_temp.verificar('la venta con saldo y fecha límite pasada es cartera vencida (S38)',
    EXISTS (SELECT 1 FROM fn_cartera_vencida() WHERE id_venta = pg_temp.venta(2) AND dias_vencida = 1));
SELECT pg_temp.verificar('la venta pagada no aparece en cartera',
    NOT EXISTS (SELECT 1 FROM fn_cartera_vencida() WHERE id_venta = pg_temp.venta(1)));
SELECT pg_temp.esperar_ok('otro ingreso del lote (venta de gallinaza)', format($$
    INSERT INTO otro_ingreso (id_lote, fecha, monto, descripcion) VALUES (%s, CURRENT_DATE, 300000, 'Venta de gallinaza') $$,
    pg_temp.lote('PRU-A')));
SELECT pg_temp.esperar_error('otro ingreso anterior al ingreso del lote', format($$
    INSERT INTO otro_ingreso (id_lote, fecha, monto, descripcion) VALUES (%s, CURRENT_DATE - 60, 1, 'x') $$,
    pg_temp.lote('PRU-A')), 'debe estar entre el ingreso');
SELECT pg_temp.verificar('ganancia neta = ingresos (22.275.000 + 300.000) - costos (4.260.000)',
    (SELECT ingreso_ventas = 22275000 AND otros_ingresos = 300000 AND ganancia = 18315000 AND aves_vendidas = 990
     FROM v_rentabilidad_lote WHERE codigo = 'PRU-A'));
SELECT pg_temp.verificar('peso promedio de salida = libras vendidas / aves vendidas (2,5 lb)',
    (SELECT libras_vendidas = 2475 AND peso_promedio_libras = 2.5 FROM v_rentabilidad_lote WHERE codigo = 'PRU-A'));

-- =====================================================================
-- Seguridad: la aplicación no borra ventas, pagos ni empleados
-- =====================================================================
SELECT pg_temp.esperar_error('la aplicación no borra pagos', $$ DELETE FROM pago $$, 'permission denied');
SELECT pg_temp.esperar_error('la aplicación no borra ventas', $$ DELETE FROM venta $$, 'permission denied');
SELECT pg_temp.esperar_error('la aplicación no borra empleados', $$ DELETE FROM empleado $$, 'permission denied');

ROLLBACK;
