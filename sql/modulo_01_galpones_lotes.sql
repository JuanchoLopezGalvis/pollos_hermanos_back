-- =====================================================================
-- MÓDULO 1: GALPONES, LOTES Y MORTALIDAD
-- Modelo: docs/modelo_relacional.md (módulo 3 del documento).
--
-- Ejecutar una sola vez, como superusuario, sobre la base pollos_hermanos:
--   sudo -u postgres psql -d pollos_hermanos -v ON_ERROR_STOP=1 -f sql/modulo_01_galpones_lotes.sql
-- Los objetos quedan a nombre de pollos_owner, igual que las tablas de seguridad.
--
-- Reglas cubiertas:
--   S5  ciclo de 40 a 50 días           -> ck_lote_fecha_estimada
--   S6  estado del galpón               -> estado_galpon + trg_galpon_validar
--   S7  un lote activo por galpón       -> ux_lote_activo_por_galpon (índice parcial)
--   S8  capacidad del galpón            -> trg_lote_validar, trg_galpon_validar
--   S10 estados del lote                -> estado_lote
--   S11 aves disponibles                -> fn_aves_disponibles
--   S12 transiciones de estado          -> trg_lote_validar
--   Mortalidad diaria y alerta del 5%   -> mortalidad, trg_mortalidad_validar, sp_registrar_mortalidad
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- Tipos enumerados
-- ---------------------------------------------------------------------

CREATE TYPE estado_galpon AS ENUM ('DISPONIBLE', 'OCUPADO', 'MANTENIMIENTO');

CREATE TYPE estado_lote AS ENUM ('EN_ENGORDE', 'LISTO_PARA_VENTA', 'VENDIDO', 'CERRADO');

-- ---------------------------------------------------------------------
-- Tablas
-- ---------------------------------------------------------------------

CREATE TABLE galpon (
    id_galpon         BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre            VARCHAR(60)   NOT NULL,
    capacidad_maxima  INTEGER       NOT NULL,
    estado            estado_galpon NOT NULL DEFAULT 'DISPONIBLE',
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_galpon PRIMARY KEY (id_galpon),
    CONSTRAINT uq_galpon_nombre UNIQUE (nombre),
    CONSTRAINT ck_galpon_capacidad CHECK (capacidad_maxima > 0)
);

CREATE TABLE lote (
    id_lote               BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo                VARCHAR(20)   NOT NULL,
    id_galpon             BIGINT        NOT NULL,
    fecha_ingreso         DATE          NOT NULL,
    cantidad_inicial      INTEGER       NOT NULL,
    costo_pollito         NUMERIC(14,2) NOT NULL,
    fecha_estimada_venta  DATE          NOT NULL,
    estado                estado_lote   NOT NULL DEFAULT 'EN_ENGORDE',
    fecha_creacion        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lote PRIMARY KEY (id_lote),
    CONSTRAINT uq_lote_codigo UNIQUE (codigo),
    CONSTRAINT fk_lote_galpon FOREIGN KEY (id_galpon) REFERENCES galpon (id_galpon),
    CONSTRAINT ck_lote_codigo CHECK (codigo ~ '^[A-Z0-9-]+$'),
    CONSTRAINT ck_lote_cantidad CHECK (cantidad_inicial > 0),
    CONSTRAINT ck_lote_costo CHECK (costo_pollito >= 0),
    CONSTRAINT ck_lote_fecha_estimada
        CHECK (fecha_estimada_venta BETWEEN fecha_ingreso + 40 AND fecha_ingreso + 50)
);

-- S7: un galpón aloja un solo lote activo. Se resuelve de forma declarativa.
CREATE UNIQUE INDEX ux_lote_activo_por_galpon ON lote (id_galpon)
    WHERE estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA');

CREATE TABLE mortalidad (
    id_mortalidad   BIGINT GENERATED ALWAYS AS IDENTITY,
    id_lote         BIGINT       NOT NULL,
    fecha           DATE         NOT NULL,
    cantidad        INTEGER      NOT NULL,
    causa           VARCHAR(200),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_mortalidad PRIMARY KEY (id_mortalidad),
    CONSTRAINT fk_mortalidad_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT uq_mortalidad_lote_fecha UNIQUE (id_lote, fecha),
    CONSTRAINT ck_mortalidad_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_lote_galpon ON lote (id_galpon);

-- ---------------------------------------------------------------------
-- Funciones
-- ---------------------------------------------------------------------

-- Edad del lote en días en una fecha. El día de ingreso es el día 1.
CREATE FUNCTION fn_edad_lote(p_id_lote BIGINT, p_fecha DATE DEFAULT CURRENT_DATE)
RETURNS INTEGER
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v_ingreso DATE;
BEGIN
    SELECT fecha_ingreso INTO v_ingreso FROM lote WHERE id_lote = p_id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el lote con id %.', p_id_lote;
    END IF;
    RETURN p_fecha - v_ingreso + 1;
END;
$$;

-- Aves muertas registradas en un lote.
CREATE FUNCTION fn_mortalidad_acumulada(p_id_lote BIGINT)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(cantidad), 0)::INTEGER FROM mortalidad WHERE id_lote = p_id_lote;
$$;

-- S11: aves disponibles = cantidad inicial - mortalidad - aves vendidas.
-- Las aves vendidas se restan cuando exista el módulo de ventas, que reemplaza
-- esta función con CREATE OR REPLACE.
CREATE FUNCTION fn_aves_disponibles(p_id_lote BIGINT)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT l.cantidad_inicial - fn_mortalidad_acumulada(l.id_lote)
    FROM lote l
    WHERE l.id_lote = p_id_lote;
$$;

-- Mortalidad acumulada como porcentaje de las aves iniciales.
CREATE FUNCTION fn_mortalidad_pct(p_id_lote BIGINT)
RETURNS NUMERIC(5,2)
LANGUAGE sql STABLE AS $$
    SELECT ROUND(fn_mortalidad_acumulada(l.id_lote) * 100.0 / l.cantidad_inicial, 2)
    FROM lote l
    WHERE l.id_lote = p_id_lote;
$$;

-- Mortalidad de referencia del acta: 5% de las aves iniciales.
CREATE FUNCTION fn_mortalidad_referencia_pct()
RETURNS NUMERIC(5,2)
LANGUAGE sql IMMUTABLE AS $$
    SELECT 5.00::NUMERIC(5,2);
$$;

-- Traduce el nombre de una restricción a un mensaje en español. Lo usan los
-- procedimientos para que el usuario nunca vea un error técnico.
CREATE FUNCTION fn_mensaje_restriccion(p_restriccion TEXT, p_mensaje_original TEXT)
RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE p_restriccion
        WHEN 'uq_galpon_nombre'          THEN 'Ya existe un galpón con ese nombre.'
        WHEN 'ck_galpon_capacidad'       THEN 'La capacidad del galpón debe ser mayor que cero.'
        WHEN 'uq_lote_codigo'            THEN 'Ya existe un lote con ese código.'
        WHEN 'ux_lote_activo_por_galpon' THEN 'El galpón ya tiene un lote activo.'
        WHEN 'fk_lote_galpon'            THEN 'El galpón indicado no existe.'
        WHEN 'ck_lote_codigo'            THEN 'El código del lote solo admite letras mayúsculas, números y guiones.'
        WHEN 'ck_lote_cantidad'          THEN 'La cantidad inicial de aves debe ser mayor que cero.'
        WHEN 'ck_lote_costo'             THEN 'El costo del pollito no puede ser negativo.'
        WHEN 'ck_lote_fecha_estimada'    THEN 'La fecha estimada de venta debe estar entre 40 y 50 días después del ingreso.'
        WHEN 'fk_mortalidad_lote'        THEN 'El lote indicado no existe.'
        WHEN 'uq_mortalidad_lote_fecha'  THEN 'Ya se registró la mortalidad de ese lote para esa fecha.'
        WHEN 'ck_mortalidad_cantidad'    THEN 'La cantidad de aves muertas debe ser mayor que cero.'
        ELSE p_mensaje_original
    END;
$$;

-- ---------------------------------------------------------------------
-- Triggers
-- ---------------------------------------------------------------------

-- Galpón: no se puede marcar OCUPADO sin un lote activo, ni dejar de estarlo
-- mientras lo tenga; la capacidad no puede bajar de las aves del lote activo.
CREATE FUNCTION fn_trg_galpon_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_aves_lote_activo INTEGER;
BEGIN
    SELECT cantidad_inicial INTO v_aves_lote_activo
    FROM lote
    WHERE id_galpon = NEW.id_galpon AND estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA');

    IF NEW.estado = 'OCUPADO' AND v_aves_lote_activo IS NULL THEN
        RAISE EXCEPTION 'El galpón "%" no puede marcarse como ocupado: no tiene un lote activo.', NEW.nombre;
    END IF;
    IF NEW.estado <> 'OCUPADO' AND v_aves_lote_activo IS NOT NULL THEN
        RAISE EXCEPTION 'El galpón "%" tiene un lote activo: no puede pasar a %.', NEW.nombre, NEW.estado;
    END IF;
    IF v_aves_lote_activo > NEW.capacidad_maxima THEN
        RAISE EXCEPTION 'La capacidad del galpón "%" (%) no puede ser menor que las aves de su lote activo (%).',
            NEW.nombre, NEW.capacidad_maxima, v_aves_lote_activo;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_galpon_validar
    BEFORE INSERT OR UPDATE ON galpon
    FOR EACH ROW EXECUTE FUNCTION fn_trg_galpon_validar();

-- Lote: capacidad (S8), galpón disponible, datos inmutables y transiciones de
-- estado válidas (S12).
CREATE FUNCTION fn_trg_lote_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_galpon galpon%ROWTYPE;
    v_disponibles INTEGER;
BEGIN
    SELECT * INTO v_galpon FROM galpon WHERE id_galpon = NEW.id_galpon FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El galpón indicado no existe.';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'EN_ENGORDE' THEN
            RAISE EXCEPTION 'Un lote nuevo debe iniciar en estado EN_ENGORDE.';
        END IF;
        IF v_galpon.estado = 'MANTENIMIENTO' THEN
            RAISE EXCEPTION 'El galpón "%" está en mantenimiento y no puede recibir lotes.', v_galpon.nombre;
        END IF;
        IF v_galpon.estado = 'OCUPADO' THEN
            RAISE EXCEPTION 'El galpón "%" ya tiene un lote activo.', v_galpon.nombre;
        END IF;
        IF NEW.fecha_ingreso > CURRENT_DATE THEN
            RAISE EXCEPTION 'La fecha de ingreso no puede ser futura.';
        END IF;
    ELSE
        IF NEW.codigo <> OLD.codigo OR NEW.id_galpon <> OLD.id_galpon OR NEW.fecha_ingreso <> OLD.fecha_ingreso THEN
            RAISE EXCEPTION 'El código, el galpón y la fecha de ingreso de un lote no se pueden modificar.';
        END IF;
        IF NEW.cantidad_inicial <> OLD.cantidad_inicial
           AND NEW.cantidad_inicial < fn_mortalidad_acumulada(NEW.id_lote) THEN
            RAISE EXCEPTION 'La cantidad inicial (%) no puede ser menor que la mortalidad ya registrada (%).',
                NEW.cantidad_inicial, fn_mortalidad_acumulada(NEW.id_lote);
        END IF;

        IF NEW.estado <> OLD.estado THEN
            v_disponibles := NEW.cantidad_inicial - fn_mortalidad_acumulada(NEW.id_lote);
            IF NOT (
                   (OLD.estado = 'EN_ENGORDE'       AND NEW.estado IN ('LISTO_PARA_VENTA', 'CERRADO'))
                OR (OLD.estado = 'LISTO_PARA_VENTA' AND NEW.estado IN ('VENDIDO', 'CERRADO'))
                OR (OLD.estado = 'VENDIDO'          AND NEW.estado = 'CERRADO')
            ) THEN
                RAISE EXCEPTION 'El lote % no puede pasar de % a %.', NEW.codigo, OLD.estado, NEW.estado;
            END IF;
            IF NEW.estado IN ('VENDIDO', 'CERRADO') AND v_disponibles > 0 THEN
                RAISE EXCEPTION 'El lote % todavía tiene % aves disponibles: solo puede pasar a % cuando lleguen a cero.',
                    NEW.codigo, v_disponibles, NEW.estado;
            END IF;
        END IF;
    END IF;

    IF NEW.cantidad_inicial > v_galpon.capacidad_maxima THEN
        RAISE EXCEPTION 'La cantidad de aves (%) supera la capacidad del galpón "%" (%).',
            NEW.cantidad_inicial, v_galpon.nombre, v_galpon.capacidad_maxima;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_lote_validar
    BEFORE INSERT OR UPDATE ON lote
    FOR EACH ROW EXECUTE FUNCTION fn_trg_lote_validar();

-- Lote: mantiene el estado del galpón. Ocupado al recibir un lote, disponible
-- cuando el lote sale (vendido o cerrado).
CREATE FUNCTION fn_trg_lote_galpon()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE galpon SET estado = 'OCUPADO' WHERE id_galpon = NEW.id_galpon;
    ELSIF NEW.estado IN ('VENDIDO', 'CERRADO') AND OLD.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA') THEN
        UPDATE galpon SET estado = 'DISPONIBLE' WHERE id_galpon = NEW.id_galpon;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_lote_galpon
    AFTER INSERT OR UPDATE OF estado ON lote
    FOR EACH ROW EXECUTE FUNCTION fn_trg_lote_galpon();

-- Mortalidad: lote activo, fecha dentro del ciclo y sin superar las aves
-- disponibles.
CREATE FUNCTION fn_trg_mortalidad_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote lote%ROWTYPE;
    v_disponibles INTEGER;
BEGIN
    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF TG_OP = 'UPDATE' AND (NEW.id_lote <> OLD.id_lote OR NEW.fecha <> OLD.fecha) THEN
        RAISE EXCEPTION 'El lote y la fecha de un registro de mortalidad no se pueden modificar.';
    END IF;
    IF v_lote.estado NOT IN ('EN_ENGORDE', 'LISTO_PARA_VENTA') THEN
        RAISE EXCEPTION 'El lote % está %: solo se registra mortalidad en lotes activos.', v_lote.codigo, v_lote.estado;
    END IF;
    IF NEW.fecha < v_lote.fecha_ingreso OR NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de mortalidad debe estar entre el ingreso del lote (%) y hoy.', v_lote.fecha_ingreso;
    END IF;

    v_disponibles := fn_aves_disponibles(NEW.id_lote);
    IF TG_OP = 'UPDATE' THEN
        v_disponibles := v_disponibles + OLD.cantidad;
    END IF;
    IF NEW.cantidad > v_disponibles THEN
        RAISE EXCEPTION 'La mortalidad (%) supera las aves disponibles del lote % (%).',
            NEW.cantidad, v_lote.codigo, v_disponibles;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_mortalidad_validar
    BEFORE INSERT OR UPDATE ON mortalidad
    FOR EACH ROW EXECUTE FUNCTION fn_trg_mortalidad_validar();

-- ---------------------------------------------------------------------
-- Procedimientos
-- Cada uno hace el trabajo en un bloque interno con EXCEPTION, que traduce los
-- errores a español. Fuera de ese bloque decide COMMIT o ROLLBACK (PostgreSQL
-- no permite COMMIT dentro de un bloque con EXCEPTION). Deben llamarse fuera
-- de una transacción explícita (autocommit), como lo hace el backend.
-- ---------------------------------------------------------------------

CREATE PROCEDURE sp_registrar_lote(
    p_codigo                VARCHAR,
    p_id_galpon             BIGINT,
    p_fecha_ingreso         DATE,
    p_cantidad_inicial      INTEGER,
    p_costo_pollito         NUMERIC,
    p_fecha_estimada_venta  DATE,
    OUT p_id_lote           BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO lote (codigo, id_galpon, fecha_ingreso, cantidad_inicial, costo_pollito, fecha_estimada_venta)
        VALUES (UPPER(TRIM(p_codigo)), p_id_galpon, p_fecha_ingreso, p_cantidad_inicial, p_costo_pollito,
                COALESCE(p_fecha_estimada_venta, p_fecha_ingreso + 45))
        RETURNING id_lote INTO p_id_lote;
    EXCEPTION
        WHEN integrity_constraint_violation THEN
            GET STACKED DIAGNOSTICS v_restriccion = CONSTRAINT_NAME;
            v_error := fn_mensaje_restriccion(v_restriccion, SQLERRM);
        WHEN raise_exception THEN
            v_error := SQLERRM;
    END;

    IF v_error IS NOT NULL THEN
        ROLLBACK;
        RAISE EXCEPTION '%', v_error;
    END IF;
    COMMIT;
END;
$$;

CREATE PROCEDURE sp_cambiar_estado_lote(p_id_lote BIGINT, p_estado VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_error   TEXT;
    v_estado  estado_lote;
BEGIN
    BEGIN
        v_estado := p_estado::estado_lote;
        UPDATE lote SET estado = v_estado WHERE id_lote = p_id_lote;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe el lote con id %.', p_id_lote;
        END IF;
    EXCEPTION
        WHEN invalid_text_representation THEN
            v_error := format('El estado "%s" no es válido.', p_estado);
        WHEN raise_exception THEN
            v_error := SQLERRM;
    END;

    IF v_error IS NOT NULL THEN
        ROLLBACK;
        RAISE EXCEPTION '%', v_error;
    END IF;
    COMMIT;
END;
$$;

-- Registra la mortalidad del día y avisa si la acumulada supera la referencia
-- del 5%.
CREATE PROCEDURE sp_registrar_mortalidad(
    p_id_lote             BIGINT,
    p_fecha               DATE,
    p_cantidad            INTEGER,
    p_causa               VARCHAR,
    OUT p_id_mortalidad   BIGINT,
    OUT p_porcentaje      NUMERIC,
    OUT p_alerta          BOOLEAN)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO mortalidad (id_lote, fecha, cantidad, causa)
        VALUES (p_id_lote, COALESCE(p_fecha, CURRENT_DATE), p_cantidad, NULLIF(TRIM(p_causa), ''))
        RETURNING id_mortalidad INTO p_id_mortalidad;
        p_porcentaje := fn_mortalidad_pct(p_id_lote);
        p_alerta := p_porcentaje > fn_mortalidad_referencia_pct();
    EXCEPTION
        WHEN integrity_constraint_violation THEN
            GET STACKED DIAGNOSTICS v_restriccion = CONSTRAINT_NAME;
            v_error := fn_mensaje_restriccion(v_restriccion, SQLERRM);
        WHEN raise_exception THEN
            v_error := SQLERRM;
    END;

    IF v_error IS NOT NULL THEN
        ROLLBACK;
        RAISE EXCEPTION '%', v_error;
    END IF;
    COMMIT;
END;
$$;

-- ---------------------------------------------------------------------
-- Vista de consulta para la aplicación
-- ---------------------------------------------------------------------

CREATE VIEW v_lote_resumen AS
SELECT l.id_lote,
       l.codigo,
       l.id_galpon,
       g.nombre                                         AS galpon,
       l.fecha_ingreso,
       l.cantidad_inicial,
       l.costo_pollito,
       l.cantidad_inicial * l.costo_pollito              AS costo_pollitos,
       l.fecha_estimada_venta,
       l.estado,
       CURRENT_DATE - l.fecha_ingreso + 1                AS edad_dias,
       fn_mortalidad_acumulada(l.id_lote)                AS mortalidad_acumulada,
       fn_mortalidad_pct(l.id_lote)                      AS porcentaje_mortalidad,
       fn_aves_disponibles(l.id_lote)                    AS aves_disponibles,
       fn_mortalidad_pct(l.id_lote) > fn_mortalidad_referencia_pct() AS alerta_mortalidad
FROM lote l
JOIN galpon g ON g.id_galpon = l.id_galpon;

-- ---------------------------------------------------------------------
-- Privilegios mínimos de la aplicación: sin DDL ni borrado de lotes y
-- mortalidad (se corrigen, no se borran). Primero se quitan los privilegios
-- heredados de ALTER DEFAULT PRIVILEGES, que dan DELETE en toda tabla nueva.
-- ---------------------------------------------------------------------

REVOKE ALL ON galpon, lote, mortalidad, v_lote_resumen FROM pollos_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON galpon TO pollos_app;
GRANT SELECT, INSERT, UPDATE ON lote, mortalidad TO pollos_app;
GRANT SELECT ON v_lote_resumen TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_registrar_lote, sp_cambiar_estado_lote, sp_registrar_mortalidad TO pollos_app;

-- Permisos de la aplicación para este módulo.
INSERT INTO permiso (codigo, descripcion) VALUES
    ('galpon.ver',            'Consultar los galpones'),
    ('galpon.gestionar',      'Crear, modificar y eliminar galpones'),
    ('lote.ver',              'Consultar los lotes y su mortalidad'),
    ('lote.gestionar',        'Registrar lotes y cambiar su estado'),
    ('mortalidad.registrar',  'Registrar y corregir la mortalidad diaria de los lotes')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
