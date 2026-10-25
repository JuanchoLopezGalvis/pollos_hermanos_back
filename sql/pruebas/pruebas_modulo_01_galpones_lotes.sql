-- =====================================================================
-- PRUEBAS DEL MÓDULO 1: GALPONES, LOTES Y MORTALIDAD
-- (con los ajustes del módulo 7: varios lotes por galpón y ciclo de 35 a 45 días)
--
-- Cada regla se prueba con un caso válido y uno inválido. Corre como el rol de
-- la aplicación, dentro de una transacción que se deshace al final, así que no
-- deja datos:
--   psql -h localhost -U pollos_app -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/pruebas/pruebas_modulo_01_galpones_lotes.sql
-- Cada caso imprime "OK"; si uno falla, el script se detiene con "FALLÓ".
--
-- Los procedimientos hacen COMMIT, por eso no se llaman aquí: las reglas que
-- aplican viven en los triggers y restricciones que sí se prueban.
-- =====================================================================

BEGIN;

SET client_min_messages = notice;
\pset tuples_only on
\pset format unaligned

-- Ejecuta una sentencia que debe funcionar.
CREATE FUNCTION pg_temp.esperar_ok(p_caso TEXT, p_sql TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    EXECUTE p_sql;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

-- Ejecuta una sentencia que debe ser rechazada con un mensaje que contenga p_fragmento.
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

-- Comprueba una condición.
CREATE FUNCTION pg_temp.verificar(p_caso TEXT, p_condicion BOOLEAN) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF p_condicion IS NOT TRUE THEN
        RAISE EXCEPTION 'FALLÓ [%]', p_caso;
    END IF;
    RAISE NOTICE 'OK   [%]', p_caso;
END;
$$;

-- Atajos para buscar los datos de prueba por su nombre o código.
CREATE FUNCTION pg_temp.galpon(p_nombre TEXT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT id_galpon FROM galpon WHERE nombre = p_nombre $$;
CREATE FUNCTION pg_temp.lote(p_codigo TEXT) RETURNS BIGINT LANGUAGE sql AS
    $$ SELECT id_lote FROM lote WHERE codigo = p_codigo $$;

-- ---------------------------------------------------------------------
-- Galpones (S6)
-- ---------------------------------------------------------------------
SELECT pg_temp.esperar_ok('galpón válido',
    $$ INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA G1', 1000), ('PRUEBA G2', 500), ('PRUEBA G3', 100) $$);
SELECT pg_temp.esperar_error('galpón con capacidad cero',
    $$ INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA G0', 0) $$, 'ck_galpon_capacidad');
SELECT pg_temp.esperar_error('galpón con nombre repetido',
    $$ INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA G1', 10) $$, 'uq_galpon_nombre');
SELECT pg_temp.esperar_error('galpón ocupado a mano sin lote',
    $$ UPDATE galpon SET estado = 'OCUPADO' WHERE nombre = 'PRUEBA G1' $$, 'no tiene lotes activos');

-- ---------------------------------------------------------------------
-- Lotes: creación, capacidad compartida del galpón, varios lotes por galpón, ciclo
-- ---------------------------------------------------------------------
SELECT pg_temp.esperar_ok('lote válido',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-001', %s, CURRENT_DATE - 10, 800, 2500, CURRENT_DATE + 35) $$, pg_temp.galpon('PRUEBA G1')));
SELECT pg_temp.verificar('el galpón queda ocupado al recibir el lote',
    (SELECT estado = 'OCUPADO' FROM galpon WHERE nombre = 'PRUEBA G1'));
SELECT pg_temp.esperar_error('lote que supera la capacidad del galpón',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-002', %s, CURRENT_DATE, 600, 2500, CURRENT_DATE + 40) $$, pg_temp.galpon('PRUEBA G2')),
    'no caben');
SELECT pg_temp.esperar_ok('segundo lote en el mismo galpón (producción escalonada)',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-003', %s, CURRENT_DATE - 3, 100, 2500, CURRENT_DATE + 37) $$, pg_temp.galpon('PRUEBA G1')));
SELECT pg_temp.verificar('aves alojadas = suma de los lotes activos del galpón',
    fn_aves_alojadas(pg_temp.galpon('PRUEBA G1')) = 900);
SELECT pg_temp.esperar_error('lote que supera la capacidad compartida del galpón',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-007', %s, CURRENT_DATE, 101, 2500, CURRENT_DATE + 40) $$, pg_temp.galpon('PRUEBA G1')),
    'no caben');
SELECT pg_temp.esperar_ok('galpón vacío pasa a mantenimiento',
    $$ UPDATE galpon SET estado = 'MANTENIMIENTO' WHERE nombre = 'PRUEBA G2' $$);
SELECT pg_temp.esperar_error('lote en galpón en mantenimiento',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-004', %s, CURRENT_DATE, 100, 2500, CURRENT_DATE + 45) $$, pg_temp.galpon('PRUEBA G2')),
    'mantenimiento');
SELECT pg_temp.esperar_error('venta estimada antes de 35 días',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-005', %s, CURRENT_DATE, 50, 2500, CURRENT_DATE + 30) $$, pg_temp.galpon('PRUEBA G3')),
    'ck_lote_fecha_estimada');
SELECT pg_temp.esperar_error('venta estimada después de 45 días',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-008', %s, CURRENT_DATE, 50, 2500, CURRENT_DATE + 46) $$, pg_temp.galpon('PRUEBA G3')),
    'ck_lote_fecha_estimada');
SELECT pg_temp.esperar_error('código de lote repetido',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-001', %s, CURRENT_DATE, 50, 2500, CURRENT_DATE + 40) $$, pg_temp.galpon('PRUEBA G3')),
    'uq_lote_codigo');
SELECT pg_temp.esperar_error('fecha de ingreso futura',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-006', %s, CURRENT_DATE + 1, 50, 2500, CURRENT_DATE + 41) $$, pg_temp.galpon('PRUEBA G3')),
    'no puede ser futura');
SELECT pg_temp.esperar_error('código del lote inmutable',
    $$ UPDATE lote SET codigo = 'PRU-999' WHERE codigo = 'PRU-001' $$, 'no se pueden modificar');
SELECT pg_temp.esperar_error('capacidad del galpón menor que las aves de sus lotes activos',
    $$ UPDATE galpon SET capacidad_maxima = 700 WHERE nombre = 'PRUEBA G1' $$, 'no puede ser menor');
SELECT pg_temp.esperar_error('galpón con lote activo a mantenimiento',
    $$ UPDATE galpon SET estado = 'MANTENIMIENTO' WHERE nombre = 'PRUEBA G1' $$, 'lote(s) activo(s)');
SELECT pg_temp.verificar('edad del lote: el día de ingreso es el día 1',
    fn_edad_lote(pg_temp.lote('PRU-001')) = 11);

-- ---------------------------------------------------------------------
-- Mortalidad diaria y alerta del 5%
-- ---------------------------------------------------------------------
SELECT pg_temp.esperar_ok('mortalidad válida',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 5, 10) $$, pg_temp.lote('PRU-001')));
SELECT pg_temp.verificar('aves disponibles = iniciales - mortalidad (S11)',
    fn_aves_disponibles(pg_temp.lote('PRU-001')) = 790);
SELECT pg_temp.verificar('sin alerta con 1,25% de mortalidad',
    (SELECT porcentaje_mortalidad = 1.25 AND NOT alerta_mortalidad FROM v_lote_resumen WHERE codigo = 'PRU-001'));
SELECT pg_temp.esperar_error('mortalidad repetida el mismo día',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 5, 3) $$, pg_temp.lote('PRU-001')),
    'uq_mortalidad_lote_fecha');
SELECT pg_temp.esperar_error('mortalidad antes del ingreso del lote',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 20, 3) $$, pg_temp.lote('PRU-001')),
    'debe estar entre el ingreso');
SELECT pg_temp.esperar_error('mortalidad con fecha futura',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE + 1, 3) $$, pg_temp.lote('PRU-001')),
    'debe estar entre el ingreso');
SELECT pg_temp.esperar_error('mortalidad mayor que las aves disponibles',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 4, 791) $$, pg_temp.lote('PRU-001')),
    'supera las aves disponibles');
SELECT pg_temp.esperar_error('mortalidad en cero',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 4, 0) $$, pg_temp.lote('PRU-001')),
    'ck_mortalidad_cantidad');
SELECT pg_temp.esperar_ok('segunda mortalidad que supera el 5%',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 4, 40) $$, pg_temp.lote('PRU-001')));
SELECT pg_temp.verificar('alerta cuando la mortalidad acumulada supera el 5%',
    (SELECT porcentaje_mortalidad = 6.25 AND alerta_mortalidad FROM v_lote_resumen WHERE codigo = 'PRU-001'));
SELECT pg_temp.esperar_error('cantidad inicial menor que la mortalidad registrada',
    $$ UPDATE lote SET cantidad_inicial = 40 WHERE codigo = 'PRU-001' $$, 'no puede ser menor que la mortalidad');

-- ---------------------------------------------------------------------
-- Transiciones de estado (S12) y liberación del galpón
-- ---------------------------------------------------------------------
SELECT pg_temp.esperar_error('de EN_ENGORDE no se salta a VENDIDO',
    $$ UPDATE lote SET estado = 'VENDIDO' WHERE codigo = 'PRU-001' $$, 'no puede pasar de');
SELECT pg_temp.esperar_error('no se cierra un lote con aves disponibles',
    $$ UPDATE lote SET estado = 'CERRADO' WHERE codigo = 'PRU-001' $$, 'todavía tiene');
SELECT pg_temp.esperar_ok('EN_ENGORDE pasa a LISTO_PARA_VENTA',
    $$ UPDATE lote SET estado = 'LISTO_PARA_VENTA' WHERE codigo = 'PRU-001' $$);
SELECT pg_temp.esperar_error('no se vende un lote con aves disponibles',
    $$ UPDATE lote SET estado = 'VENDIDO' WHERE codigo = 'PRU-001' $$, 'todavía tiene');

SELECT pg_temp.esperar_ok('mortalidad total del segundo lote del galpón G1',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE, 100) $$, pg_temp.lote('PRU-003')));
SELECT pg_temp.esperar_ok('el lote sin aves se cierra', $$ UPDATE lote SET estado = 'CERRADO' WHERE codigo = 'PRU-003' $$);
SELECT pg_temp.verificar('el galpón sigue ocupado mientras tenga otro lote activo',
    (SELECT estado = 'OCUPADO' FROM galpon WHERE nombre = 'PRUEBA G1'));

SELECT pg_temp.esperar_ok('lote pequeño con mortalidad total',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-010', %s, CURRENT_DATE - 3, 50, 2500, CURRENT_DATE + 40) $$, pg_temp.galpon('PRUEBA G3')));
SELECT pg_temp.esperar_ok('mortalidad de todas las aves',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE, 50) $$, pg_temp.lote('PRU-010')));
SELECT pg_temp.esperar_ok('lote sin aves pasa a CERRADO',
    $$ UPDATE lote SET estado = 'CERRADO' WHERE codigo = 'PRU-010' $$);
SELECT pg_temp.verificar('el galpón queda disponible al cerrar el lote',
    (SELECT estado = 'DISPONIBLE' FROM galpon WHERE nombre = 'PRUEBA G3'));
SELECT pg_temp.esperar_error('un lote cerrado no vuelve a engorde',
    $$ UPDATE lote SET estado = 'EN_ENGORDE' WHERE codigo = 'PRU-010' $$, 'no puede pasar de');
SELECT pg_temp.esperar_error('no se registra mortalidad en un lote cerrado',
    format($$ INSERT INTO mortalidad (id_lote, fecha, cantidad) VALUES (%s, CURRENT_DATE - 1, 1) $$, pg_temp.lote('PRU-010')),
    'lotes activos');
SELECT pg_temp.esperar_ok('el galpón liberado recibe un lote nuevo',
    format($$ INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
              VALUES ('PRU-011', %s, CURRENT_DATE, 100, 2500, CURRENT_DATE + 45) $$, pg_temp.galpon('PRUEBA G3')));
SELECT pg_temp.verificar('el panel muestra la ocupación del galpón',
    (SELECT lotes_activos = 1 AND aves_alojadas = 750 AND capacidad_libre = 250
     FROM v_galpon_ocupacion WHERE nombre = 'PRUEBA G1'));

-- ---------------------------------------------------------------------
-- Seguridad: la aplicación no puede borrar lotes ni crear tablas
-- ---------------------------------------------------------------------
SELECT pg_temp.esperar_error('la aplicación no borra lotes',
    $$ DELETE FROM lote WHERE codigo = 'PRU-011' $$, 'permission denied');
SELECT pg_temp.esperar_error('la aplicación no ejecuta DDL',
    $$ CREATE TABLE public.tabla_intrusa (id INT) $$, 'permission denied');

ROLLBACK;
