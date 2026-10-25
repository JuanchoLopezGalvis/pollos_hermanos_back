-- =====================================================================
-- MÓDULO 5: GASTOS, BENEFICIO Y COSTO DEL LOTE
-- Modelo: docs/modelo_relacional.md (módulo 6 del documento).
--   ./instalar_bd.sh modulo_05_gastos_beneficio.sql
--
-- Reglas cubiertas:
--   S22, S23 gastos por lote o generales      -> gasto (id_lote NULL = general), categoria_gasto
--   D4 sin doble conteo                       -> categorías sin alimentación, sanidad ni beneficio
--   D7 gastos generales fuera del costo       -> fn_costo_lote solo suma gastos con lote
--   Beneficio por jornada y beneficiadora     -> jornada_beneficio, detalle_beneficio (N:M)
--   Solo beneficiadoras activas               -> trg_detalle_beneficio_validar (fn_validar_empleado)
--   Costo del lote (5 componentes)            -> fn_costo_lote, v_costo_lote
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

CREATE TABLE categoria_gasto (
    id_categoria_gasto  BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo              VARCHAR(30)  NOT NULL,
    nombre              VARCHAR(60)  NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_categoria_gasto PRIMARY KEY (id_categoria_gasto),
    CONSTRAINT uq_categoria_gasto_codigo UNIQUE (codigo),
    CONSTRAINT uq_categoria_gasto_nombre UNIQUE (nombre),
    CONSTRAINT ck_categoria_gasto_codigo CHECK (codigo ~ '^[A-Z_]+$')
);

-- D4: la alimentación, la sanidad y el beneficio no son categorías de gasto:
-- su costo sale de sus propias tablas. "Mano de obra" es para otros trabajos
-- de la finca, nunca para pagar a las beneficiadoras.
INSERT INTO categoria_gasto (codigo, nombre) VALUES
    ('MANO_OBRA',          'Mano de obra'),
    ('SERVICIOS_PUBLICOS', 'Servicios públicos'),
    ('TRANSPORTE',         'Transporte'),
    ('OTROS',              'Otros');

CREATE TABLE gasto (
    id_gasto            BIGINT GENERATED ALWAYS AS IDENTITY,
    id_categoria_gasto  BIGINT         NOT NULL,
    id_lote             BIGINT,
    fecha               DATE           NOT NULL,
    monto               NUMERIC(14,2)  NOT NULL,
    descripcion         VARCHAR(200)   NOT NULL,
    fecha_creacion      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_gasto PRIMARY KEY (id_gasto),
    CONSTRAINT fk_gasto_categoria FOREIGN KEY (id_categoria_gasto) REFERENCES categoria_gasto (id_categoria_gasto),
    CONSTRAINT fk_gasto_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT ck_gasto_monto CHECK (monto > 0),
    CONSTRAINT ck_gasto_descripcion CHECK (TRIM(descripcion) <> '')
);

CREATE TABLE jornada_beneficio (
    id_jornada      BIGINT GENERATED ALWAYS AS IDENTITY,
    id_lote         BIGINT        NOT NULL,
    fecha           DATE          NOT NULL,
    observaciones   VARCHAR(300),
    fecha_creacion  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_jornada_beneficio PRIMARY KEY (id_jornada),
    CONSTRAINT fk_jornada_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT uq_jornada_lote_fecha UNIQUE (id_lote, fecha)
);

-- N:M jornada–empleado: qué beneficiadora participó, cuántos pollos benefició
-- y qué tarifa por pollo se le pagó.
CREATE TABLE detalle_beneficio (
    id_jornada         BIGINT         NOT NULL,
    id_empleado        BIGINT         NOT NULL,
    aves_beneficiadas  INTEGER        NOT NULL,
    tarifa_por_ave     NUMERIC(14,2)  NOT NULL,
    fecha_creacion     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_detalle_beneficio PRIMARY KEY (id_jornada, id_empleado),
    CONSTRAINT fk_detalle_beneficio_jornada FOREIGN KEY (id_jornada) REFERENCES jornada_beneficio (id_jornada) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_beneficio_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado),
    CONSTRAINT ck_detalle_beneficio_aves CHECK (aves_beneficiadas > 0),
    CONSTRAINT ck_detalle_beneficio_tarifa CHECK (tarifa_por_ave > 0)
);

CREATE INDEX ix_gasto_lote ON gasto (id_lote);
CREATE INDEX ix_detalle_beneficio_empleado ON detalle_beneficio (id_empleado);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('fk_gasto_categoria',             'La categoría de gasto indicada no existe.'),
    ('fk_gasto_lote',                  'El lote indicado no existe.'),
    ('ck_gasto_monto',                 'El monto del gasto debe ser mayor que cero.'),
    ('ck_gasto_descripcion',           'La descripción del gasto es obligatoria.'),
    ('fk_jornada_lote',                'El lote indicado no existe.'),
    ('uq_jornada_lote_fecha',          'Ya hay una jornada de beneficio de ese lote en esa fecha.'),
    ('pk_detalle_beneficio',           'Una beneficiadora aparece dos veces en la misma jornada.'),
    ('fk_detalle_beneficio_empleado',  'La beneficiadora indicada no existe.'),
    ('ck_detalle_beneficio_aves',      'Las aves beneficiadas deben ser más de cero.'),
    ('ck_detalle_beneficio_tarifa',    'La tarifa por pollo debe ser mayor que cero.');

-- ---------------------------------------------------------------------
-- Triggers
-- ---------------------------------------------------------------------

CREATE FUNCTION fn_trg_gasto_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote lote%ROWTYPE;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM categoria_gasto WHERE id_categoria_gasto = NEW.id_categoria_gasto AND activo) THEN
        RAISE EXCEPTION 'La categoría de gasto indicada no existe o está inactiva.';
    END IF;
    IF NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha del gasto no puede ser futura.';
    END IF;
    IF NEW.id_lote IS NOT NULL THEN
        SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'El lote indicado no existe.';
        END IF;
        IF v_lote.estado = 'CERRADO' THEN
            RAISE EXCEPTION 'El lote % está cerrado: no admite más gastos.', v_lote.codigo;
        END IF;
        IF NEW.fecha < v_lote.fecha_ingreso THEN
            RAISE EXCEPTION 'La fecha del gasto es anterior al ingreso del lote % (%).', v_lote.codigo, v_lote.fecha_ingreso;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_gasto_validar
    BEFORE INSERT OR UPDATE ON gasto
    FOR EACH ROW EXECUTE FUNCTION fn_trg_gasto_validar();

-- El beneficio se hace cuando el lote está listo para venta; también se
-- admite en un lote ya vendido (el beneficio puede registrarse el día de la
-- última venta), pero no en uno cerrado.
CREATE FUNCTION fn_trg_jornada_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote lote%ROWTYPE;
BEGIN
    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF v_lote.estado NOT IN ('LISTO_PARA_VENTA', 'VENDIDO') THEN
        RAISE EXCEPTION 'El lote % está %: el beneficio solo se registra en lotes listos para venta o vendidos.',
            v_lote.codigo, v_lote.estado;
    END IF;
    IF NEW.fecha < v_lote.fecha_ingreso OR NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha del beneficio debe estar entre el ingreso del lote (%) y hoy.', v_lote.fecha_ingreso;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_jornada_validar
    BEFORE INSERT OR UPDATE ON jornada_beneficio
    FOR EACH ROW EXECUTE FUNCTION fn_trg_jornada_validar();

-- Solo beneficiadoras activas; el total beneficiado del lote no supera sus
-- aves vivas.
CREATE FUNCTION fn_trg_detalle_beneficio_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_id_lote     BIGINT;
    v_codigo      TEXT;
    v_vivas       INTEGER;
    v_beneficiadas INTEGER;
BEGIN
    PERFORM fn_validar_empleado(NEW.id_empleado, 'BENEFICIADORA');

    SELECT j.id_lote, l.codigo, l.cantidad_inicial - fn_mortalidad_acumulada(l.id_lote)
      INTO v_id_lote, v_codigo, v_vivas
    FROM jornada_beneficio j JOIN lote l ON l.id_lote = j.id_lote
    WHERE j.id_jornada = NEW.id_jornada
    FOR UPDATE OF l;

    SELECT COALESCE(SUM(d.aves_beneficiadas), 0) INTO v_beneficiadas
    FROM detalle_beneficio d JOIN jornada_beneficio j ON j.id_jornada = d.id_jornada
    WHERE j.id_lote = v_id_lote
      AND NOT (TG_OP = 'UPDATE' AND d.id_jornada = OLD.id_jornada AND d.id_empleado = OLD.id_empleado);

    IF v_beneficiadas + NEW.aves_beneficiadas > v_vivas THEN
        RAISE EXCEPTION 'El lote % tiene % aves vivas y ya se beneficiaron %: no se pueden beneficiar % más.',
            v_codigo, v_vivas, v_beneficiadas, NEW.aves_beneficiadas;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_detalle_beneficio_validar
    BEFORE INSERT OR UPDATE ON detalle_beneficio
    FOR EACH ROW EXECUTE FUNCTION fn_trg_detalle_beneficio_validar();

-- ---------------------------------------------------------------------
-- Funciones de costo
-- ---------------------------------------------------------------------

-- Costo del beneficio: suma de los pagos a las beneficiadoras del lote.
CREATE FUNCTION fn_costo_beneficio_lote(p_id_lote BIGINT)
RETURNS NUMERIC(16,2)
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(d.aves_beneficiadas * d.tarifa_por_ave), 0)::NUMERIC(16,2)
    FROM detalle_beneficio d JOIN jornada_beneficio j ON j.id_jornada = d.id_jornada
    WHERE j.id_lote = p_id_lote;
$$;

-- Costo del lote con sus cinco componentes (acta): pollitos, alimentación,
-- sanidad, beneficio y otros gastos del lote.
CREATE FUNCTION fn_costo_lote(p_id_lote BIGINT)
RETURNS TABLE (costo_pollitos NUMERIC, costo_alimento NUMERIC, costo_sanidad NUMERIC,
               costo_beneficio NUMERIC, otros_gastos NUMERIC, costo_total NUMERIC)
LANGUAGE sql STABLE AS $$
    WITH c AS (
        SELECT l.cantidad_inicial * l.costo_pollito AS pollitos,
               COALESCE((SELECT SUM(ROUND(ca.cantidad_bultos * ca.costo_unitario, 2))
                           FROM consumo_alimento ca WHERE ca.id_lote = l.id_lote), 0) AS alimento,
               COALESCE((SELECT SUM(ROUND(a.dosis * a.costo_unitario, 2))
                           FROM aplicacion_sanitaria a WHERE a.id_lote = l.id_lote), 0) AS sanidad,
               fn_costo_beneficio_lote(l.id_lote) AS beneficio,
               COALESCE((SELECT SUM(g.monto) FROM gasto g WHERE g.id_lote = l.id_lote), 0) AS otros
        FROM lote l WHERE l.id_lote = p_id_lote
    )
    SELECT pollitos, alimento, sanidad, beneficio, otros, pollitos + alimento + sanidad + beneficio + otros
    FROM c;
$$;

-- ---------------------------------------------------------------------
-- Procedimientos
-- ---------------------------------------------------------------------

CREATE PROCEDURE sp_registrar_gasto(
    p_id_categoria_gasto  BIGINT,
    p_id_lote             BIGINT,
    p_fecha               DATE,
    p_monto               NUMERIC,
    p_descripcion         VARCHAR,
    OUT p_id_gasto        BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO gasto (id_categoria_gasto, id_lote, fecha, monto, descripcion)
        VALUES (p_id_categoria_gasto, p_id_lote, COALESCE(p_fecha, CURRENT_DATE), p_monto, TRIM(p_descripcion))
        RETURNING id_gasto INTO p_id_gasto;
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

-- Registra la jornada y su detalle en una sola transacción. p_detalle es un
-- arreglo JSON: [{"idEmpleado": 4, "avesBeneficiadas": 300, "tarifaPorAve": 900}, ...].
-- Si una línea es inválida, no queda nada registrado.
CREATE PROCEDURE sp_registrar_jornada_beneficio(
    p_id_lote          BIGINT,
    p_fecha            DATE,
    p_observaciones    VARCHAR,
    p_detalle          JSONB,
    OUT p_id_jornada   BIGINT,
    OUT p_costo_total  NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        IF p_detalle IS NULL OR jsonb_typeof(p_detalle) <> 'array' OR jsonb_array_length(p_detalle) = 0 THEN
            RAISE EXCEPTION 'La jornada de beneficio debe tener al menos una beneficiadora.';
        END IF;

        INSERT INTO jornada_beneficio (id_lote, fecha, observaciones)
        VALUES (p_id_lote, COALESCE(p_fecha, CURRENT_DATE), NULLIF(TRIM(p_observaciones), ''))
        RETURNING id_jornada INTO p_id_jornada;

        INSERT INTO detalle_beneficio (id_jornada, id_empleado, aves_beneficiadas, tarifa_por_ave)
        SELECT p_id_jornada, d."idEmpleado", d."avesBeneficiadas", d."tarifaPorAve"
        FROM jsonb_to_recordset(p_detalle) AS d ("idEmpleado" BIGINT, "avesBeneficiadas" INTEGER, "tarifaPorAve" NUMERIC);

        SELECT SUM(aves_beneficiadas * tarifa_por_ave) INTO p_costo_total
        FROM detalle_beneficio WHERE id_jornada = p_id_jornada;
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
-- Vistas
-- ---------------------------------------------------------------------

CREATE VIEW v_gasto AS
SELECT g.id_gasto, g.fecha, g.id_categoria_gasto, c.nombre AS categoria, g.id_lote, l.codigo AS codigo_lote,
       g.monto, g.descripcion
FROM gasto g
JOIN categoria_gasto c ON c.id_categoria_gasto = g.id_categoria_gasto
LEFT JOIN lote l ON l.id_lote = g.id_lote;

CREATE VIEW v_jornada_beneficio AS
SELECT j.id_jornada, j.fecha, j.id_lote, l.codigo AS codigo_lote, j.observaciones,
       SUM(d.aves_beneficiadas)                    AS aves_beneficiadas,
       COUNT(d.id_empleado)                        AS beneficiadoras,
       SUM(d.aves_beneficiadas * d.tarifa_por_ave) AS costo_total
FROM jornada_beneficio j
JOIN lote l ON l.id_lote = j.id_lote
LEFT JOIN detalle_beneficio d ON d.id_jornada = j.id_jornada
GROUP BY j.id_jornada, l.codigo;

CREATE VIEW v_detalle_beneficio AS
SELECT d.id_jornada, d.id_empleado, e.nombres || ' ' || e.apellidos AS beneficiadora,
       d.aves_beneficiadas, d.tarifa_por_ave, d.aves_beneficiadas * d.tarifa_por_ave AS pago
FROM detalle_beneficio d
JOIN empleado e ON e.id_empleado = d.id_empleado;

CREATE VIEW v_costo_lote AS
SELECT l.id_lote, l.codigo, l.estado::text AS estado, c.*
FROM lote l
CROSS JOIN LATERAL fn_costo_lote(l.id_lote) c;

REVOKE ALL ON categoria_gasto, gasto, jornada_beneficio, detalle_beneficio,
              v_gasto, v_jornada_beneficio, v_detalle_beneficio, v_costo_lote FROM pollos_app;
GRANT SELECT ON categoria_gasto, v_gasto, v_jornada_beneficio, v_detalle_beneficio, v_costo_lote TO pollos_app;
GRANT SELECT, INSERT ON gasto, jornada_beneficio, detalle_beneficio TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_registrar_gasto, sp_registrar_jornada_beneficio TO pollos_app;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('gasto.registrar',      'Registrar gastos de los lotes y de la finca'),
    ('beneficio.registrar',  'Registrar las jornadas de beneficio y el pago a las beneficiadoras'),
    ('costo.ver',            'Consultar el costo y la rentabilidad de los lotes')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
