BEGIN;

SET client_min_messages = notice;
\pset tuples_only on
\pset format unaligned

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

-- Filas de auditoría de esta prueba para una tabla y operación.
CREATE FUNCTION pg_temp.fn_aud(p_tabla TEXT, p_operacion TEXT) RETURNS SETOF auditoria LANGUAGE sql AS $$
    SELECT * FROM auditoria
    WHERE id_transaccion = txid_current() AND tabla = p_tabla AND operacion = p_operacion
    ORDER BY id_auditoria
$$;

CREATE FUNCTION pg_temp.fn_galpon() RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_galpon FROM galpon WHERE nombre = 'PRUEBA AUDITORIA' $$;
CREATE FUNCTION pg_temp.fn_usuario() RETURNS BIGINT LANGUAGE sql AS $$ SELECT id_usuario FROM usuario WHERE nombre_usuario = 'prueba.auditoria' $$;

-- El backend fija el usuario autenticado; aquí se simula.
SELECT set_config('pollos.usuario', 'gerente.prueba', TRUE);

-- =====================================================================
-- D31 y D32: INSERT, UPDATE y DELETE quedan registrados con su usuario
-- =====================================================================
INSERT INTO galpon (nombre, capacidad_maxima) VALUES ('PRUEBA AUDITORIA', 1000);

SELECT pg_temp.fn_verificar('el INSERT queda auditado con la fila nueva',
    (SELECT count(*) = 1 AND bool_and(datos_antes IS NULL AND datos_despues ->> 'nombre' = 'PRUEBA AUDITORIA')
     FROM pg_temp.fn_aud('galpon', 'INSERT')));
SELECT pg_temp.fn_verificar('id_registro es la llave primaria',
    (SELECT id_registro = pg_temp.fn_galpon()::TEXT FROM pg_temp.fn_aud('galpon', 'INSERT')));
SELECT pg_temp.fn_verificar('se guarda el usuario de la aplicación y el de la base',
    (SELECT usuario_app = 'gerente.prueba' AND usuario_bd = session_user FROM pg_temp.fn_aud('galpon', 'INSERT')));

UPDATE galpon SET capacidad_maxima = 1200 WHERE id_galpon = pg_temp.fn_galpon();
SELECT pg_temp.fn_verificar('el UPDATE guarda antes, después y la columna cambiada',
    (SELECT columnas_cambiadas = ARRAY['capacidad_maxima']
        AND (datos_antes ->> 'capacidad_maxima')::INT = 1000
        AND (datos_despues ->> 'capacidad_maxima')::INT = 1200
     FROM pg_temp.fn_aud('galpon', 'UPDATE')));

UPDATE galpon SET capacidad_maxima = 1200 WHERE id_galpon = pg_temp.fn_galpon();
SELECT pg_temp.fn_verificar('un UPDATE que no cambia nada no se audita',
    (SELECT count(*) = 1 FROM pg_temp.fn_aud('galpon', 'UPDATE')));

-- Sin usuario autenticado (aviso de Wompi, tarea programada) queda NULL.
SELECT set_config('pollos.usuario', '', TRUE);
DELETE FROM galpon WHERE nombre = 'PRUEBA AUDITORIA';
SELECT pg_temp.fn_verificar('el DELETE guarda la fila borrada sin usuario de la aplicación',
    (SELECT count(*) = 1 AND bool_and(datos_despues IS NULL AND datos_antes ->> 'nombre' = 'PRUEBA AUDITORIA'
                                      AND usuario_app IS NULL)
     FROM pg_temp.fn_aud('galpon', 'DELETE')));

-- =====================================================================
-- D33: la contraseña no se copia; llaves compuestas
-- =====================================================================
SELECT set_config('pollos.usuario', 'gerente.prueba', TRUE);
INSERT INTO usuario (nombre_usuario, correo, contrasena, fecha_registro)
VALUES ('prueba.auditoria', 'prueba.auditoria@pollos.test', '$2a$10$hashDePrueba', CURRENT_TIMESTAMP);
UPDATE usuario SET contrasena = '$2a$10$otroHash' WHERE id_usuario = pg_temp.fn_usuario();

SELECT pg_temp.fn_verificar('el hash de la contraseña no queda en la auditoría',
    (SELECT bool_and(NOT (COALESCE(datos_antes, '{}') ? 'contrasena') AND NOT (COALESCE(datos_despues, '{}') ? 'contrasena'))
     FROM auditoria WHERE id_transaccion = txid_current() AND tabla = 'usuario'));
SELECT pg_temp.fn_verificar('el cambio de contraseña sí queda registrado',
    (SELECT columnas_cambiadas = ARRAY['contrasena'] FROM pg_temp.fn_aud('usuario', 'UPDATE')));

INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT pg_temp.fn_usuario(), id_rol FROM rol WHERE nombre = 'Cliente';
SELECT pg_temp.fn_verificar('la llave compuesta queda como "usuario,rol"',
    (SELECT id_registro = pg_temp.fn_usuario() || ',' || (SELECT id_rol FROM rol WHERE nombre = 'Cliente')
     FROM pg_temp.fn_aud('usuario_rol', 'INSERT')));

-- =====================================================================
-- D34: la aplicación no escribe, modifica ni borra la auditoría
-- =====================================================================
SELECT pg_temp.fn_esperar_error('la aplicación no inserta en la auditoría',
    $$ INSERT INTO auditoria (tabla, operacion) VALUES ('galpon', 'DELETE') $$, 'auditoria');
SELECT pg_temp.fn_esperar_error('la aplicación no modifica la auditoría',
    $$ UPDATE auditoria SET usuario_app = 'otro' $$, 'auditoria');
SELECT pg_temp.fn_esperar_error('la aplicación no borra la auditoría',
    $$ DELETE FROM auditoria $$, 'auditoria');

-- =====================================================================
-- D35: todas las tablas están enganchadas
-- =====================================================================
SELECT pg_temp.fn_verificar('toda tabla del esquema tiene su trigger de auditoría',
    NOT EXISTS (SELECT 1 FROM pg_tables p
                WHERE p.schemaname = 'public' AND p.tablename <> 'auditoria'
                  AND NOT EXISTS (SELECT 1 FROM pg_trigger t
                                  WHERE t.tgrelid = format('%I', p.tablename)::regclass
                                    AND t.tgname = 'tgr_aiud_' || p.tablename || '_auditoria')));

ROLLBACK;
