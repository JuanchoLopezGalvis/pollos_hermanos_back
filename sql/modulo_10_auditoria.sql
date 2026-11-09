BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- D31: tabla de auditoría
-- ---------------------------------------------------------------------

-- Una fila por cada fila insertada, modificada o borrada en cualquier tabla.
-- usuario_app es el usuario autenticado en el backend (NULL si el cambio lo
-- hizo el sistema: aviso de Wompi, tarea programada, registro público o un
-- script). usuario_bd es el rol con el que se conectó la sesión.
-- id_transaccion agrupa los cambios que hizo una misma operación.
CREATE TABLE auditoria (
    id_auditoria        BIGINT GENERATED ALWAYS AS IDENTITY,
    tabla               VARCHAR(63)  NOT NULL,
    operacion           VARCHAR(8)   NOT NULL,
    id_registro         VARCHAR(100),
    columnas_cambiadas  TEXT[],
    datos_antes         JSONB,
    datos_despues       JSONB,
    usuario_app         VARCHAR(50)  DEFAULT NULLIF(current_setting('pollos.usuario', TRUE), ''),
    usuario_bd          VARCHAR(63)  NOT NULL DEFAULT session_user,
    id_transaccion      BIGINT       NOT NULL DEFAULT txid_current(),
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
    CONSTRAINT ck_auditoria_operacion CHECK (operacion IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE'))
);

CREATE INDEX ix_auditoria_registro ON auditoria (tabla, id_registro);
CREATE INDEX ix_auditoria_fecha ON auditoria (fecha_creacion);
CREATE INDEX ix_auditoria_usuario ON auditoria (usuario_app, fecha_creacion);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('ck_auditoria_operacion', 'La operación auditada debe ser INSERT, UPDATE, DELETE o TRUNCATE.');

-- Copia en la auditoría la fila antes y después del cambio. Los argumentos del
-- trigger son las columnas de la llave primaria, para guardar id_registro
-- (las llaves compuestas quedan separadas por coma, como "12,4").
-- Es SECURITY DEFINER para que la aplicación pueda auditar sin tener permiso
-- de escribir en la tabla auditoria.
CREATE FUNCTION fn_trg_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
    v_antes     JSONB;
    v_despues   JSONB;
    vv_columnas  TEXT[];
    vv_id        TEXT;
BEGIN
    IF TG_OP = 'TRUNCATE' THEN
        INSERT INTO auditoria (tabla, operacion) VALUES (TG_TABLE_NAME, TG_OP);
        RETURN NULL;
    END IF;

    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_antes := to_jsonb(OLD);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_despues := to_jsonb(NEW);
    END IF;

    -- Un UPDATE que no cambia nada (JPA lo hace al guardar sin cambios) no se audita.
    IF TG_OP = 'UPDATE' THEN
        SELECT array_agg(d.key ORDER BY d.key) INTO vv_columnas
        FROM jsonb_each(v_despues) d
        WHERE d.value IS DISTINCT FROM v_antes -> d.key;
        IF vv_columnas IS NULL THEN
            RETURN NULL;
        END IF;
    END IF;

    SELECT string_agg(COALESCE(v_despues, v_antes) ->> t.columna, ',' ORDER BY t.orden) INTO vv_id
    FROM unnest(TG_ARGV) WITH ORDINALITY AS t(columna, orden);

    -- D33: el hash de la contraseña no se copia; columnas_cambiadas sí dice que cambió.
    INSERT INTO auditoria (tabla, operacion, id_registro, columnas_cambiadas, datos_antes, datos_despues)
    VALUES (TG_TABLE_NAME, TG_OP, vv_id, vv_columnas, v_antes - 'contrasena', v_despues - 'contrasena');
    RETURN NULL;
END;
$$;

-- ---------------------------------------------------------------------
-- D34: la auditoría no se modifica ni se borra
-- ---------------------------------------------------------------------

CREATE FUNCTION fn_trg_auditoria_inmutable()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría no se modifica ni se borra.';
END;
$$;

CREATE TRIGGER tgr_bud_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_trg_auditoria_inmutable();

CREATE TRIGGER tgr_bt_auditoria_no_truncar
    BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_trg_auditoria_inmutable();

-- ---------------------------------------------------------------------
-- D35: enganchar las tablas
-- ---------------------------------------------------------------------

-- Crea tgr_aiud_<tabla>_auditoria en cada tabla del esquema public que todavía no
-- lo tenga, con las columnas de su llave primaria como argumentos. Los módulos
-- futuros la llaman al final con SELECT fn_auditar_tablas(). Se salta (con un
-- aviso) las tablas sobre las que quien la llama no puede crear triggers.
-- Devuelve cuántas tablas enganchó.
CREATE FUNCTION fn_auditar_tablas()
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    r_tabla      RECORD;
    v_enganchadas INTEGER := 0;
BEGIN
    FOR r_tabla IN
        SELECT c.oid, c.relname,
               (SELECT string_agg(quote_literal(a.attname), ', ' ORDER BY k.orden)
                FROM unnest(i.indkey::INT2[]) WITH ORDINALITY AS k(attnum, orden)
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = k.attnum) AS llave
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        LEFT JOIN pg_index i ON i.indrelid = c.oid AND i.indisprimary
        WHERE n.nspname = 'public'
          AND c.relkind IN ('r', 'p')
          AND NOT c.relispartition
          AND c.relname <> 'auditoria'
          AND NOT EXISTS (SELECT 1 FROM pg_trigger t
                          WHERE t.tgrelid = c.oid AND t.tgfoid = 'fn_trg_auditoria'::regproc)
        ORDER BY c.relname
    LOOP
        IF NOT has_table_privilege(r_tabla.oid, 'TRIGGER') THEN
            RAISE NOTICE 'No se auditó % : no hay permiso para crearle triggers.', r_tabla.relname;
            CONTINUE;
        END IF;
        EXECUTE format('CREATE TRIGGER %I AFTER INSERT OR UPDATE OR DELETE ON %I '
                       'FOR EACH ROW EXECUTE FUNCTION fn_trg_auditoria(%s)',
                       'tgr_aiud_' || r_tabla.relname || '_auditoria', r_tabla.relname, COALESCE(r_tabla.llave, ''));
        EXECUTE format('CREATE TRIGGER %I AFTER TRUNCATE ON %I '
                       'FOR EACH STATEMENT EXECUTE FUNCTION fn_trg_auditoria()',
                       'tgr_at_' || r_tabla.relname || '_auditoria_truncar', r_tabla.relname);
        v_enganchadas := v_enganchadas + 1;
    END LOOP;
    RETURN v_enganchadas;
END;
$$;

-- ---------------------------------------------------------------------
-- Privilegios
-- ---------------------------------------------------------------------

-- La aplicación solo consulta la auditoría: escribe en ella a través del
-- trigger (SECURITY DEFINER) y no puede modificarla ni borrarla.
REVOKE ALL ON auditoria FROM pollos_app;
GRANT SELECT ON auditoria TO pollos_app;
REVOKE EXECUTE ON FUNCTION fn_auditar_tablas FROM PUBLIC;

RESET ROLE;

-- Se engancha como superusuario para cubrir también las tablas de seguridad
-- (usuario, rol, permiso...), aunque no sean de pollos_owner.
DO $$
BEGIN
    RAISE NOTICE 'Tablas auditadas: %', fn_auditar_tablas();
END;
$$;

COMMIT;
