-- =====================================================================
-- MÓDULO 7: PRODUCCIÓN ESCALONADA (ajustes del documento del negocio)
--   ./instalar_bd.sh modulo_07_produccion_escalonada.sql
-- Se instala después de los módulos 1 a 6.
--
-- Cambios respecto al módulo 1:
--   * Un galpón aloja VARIOS lotes a la vez: ingresa un lote nuevo cada semana
--     para vender cada semana. Se elimina el índice de "un lote activo por
--     galpón" y la capacidad se valida con la suma de aves vivas de todos sus
--     lotes activos.
--   * El galpón está OCUPADO mientras tenga al menos un lote activo y vuelve a
--     DISPONIBLE cuando sale el último.
--   * El engorde dura entre 35 y 45 días (antes 40 a 50); si no se indica la
--     venta estimada, se usa el ingreso más 40 días.
--   * El resumen del lote incluye aves vendidas y consumo de alimento, y hay
--     una vista de ocupación por galpón para el panel general.
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- Varios lotes por galpón y ciclo de 35 a 45 días
-- ---------------------------------------------------------------------

DROP INDEX ux_lote_activo_por_galpon;
DELETE FROM mensaje_restriccion WHERE restriccion = 'ux_lote_activo_por_galpon';

-- Índice para sumar rápido las aves de los lotes activos de cada galpón.
CREATE INDEX ix_lote_galpon_activo ON lote (id_galpon) WHERE estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA');

ALTER TABLE lote DROP CONSTRAINT ck_lote_fecha_estimada;

-- Los lotes registrados con la regla anterior (40 a 50 días) que queden fuera
-- del nuevo rango se ajustan al límite más cercano antes de aplicar la regla.
DO $$
DECLARE
    v_lote RECORD;
BEGIN
    FOR v_lote IN
        UPDATE lote
           SET fecha_estimada_venta = LEAST(GREATEST(fecha_estimada_venta, fecha_ingreso + 35), fecha_ingreso + 45)
         WHERE fecha_estimada_venta NOT BETWEEN fecha_ingreso + 35 AND fecha_ingreso + 45
        RETURNING codigo, fecha_estimada_venta
    LOOP
        RAISE NOTICE 'Lote %: venta estimada ajustada al %.', v_lote.codigo, v_lote.fecha_estimada_venta;
    END LOOP;
END;
$$;

ALTER TABLE lote ADD CONSTRAINT ck_lote_fecha_estimada
    CHECK (fecha_estimada_venta BETWEEN fecha_ingreso + 35 AND fecha_ingreso + 45);
UPDATE mensaje_restriccion
   SET mensaje = 'La fecha estimada de venta debe estar entre 35 y 45 días después del ingreso.'
 WHERE restriccion = 'ck_lote_fecha_estimada';

-- Aves vivas sin vender de los lotes activos de un galpón. Se puede excluir un
-- lote (el que se está modificando).
CREATE FUNCTION fn_aves_alojadas(p_id_galpon BIGINT, p_excluir_lote BIGINT DEFAULT NULL)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(fn_aves_disponibles(id_lote)), 0)::INTEGER
    FROM lote
    WHERE id_galpon = p_id_galpon
      AND estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')
      AND id_lote IS DISTINCT FROM p_excluir_lote;
$$;

-- ---------------------------------------------------------------------
-- Galpón: ocupado solo con lotes activos; la capacidad no baja de sus aves.
-- ---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_trg_galpon_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lotes_activos INTEGER;
    v_alojadas      INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_lotes_activos
    FROM lote WHERE id_galpon = NEW.id_galpon AND estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA');

    IF NEW.estado = 'OCUPADO' AND v_lotes_activos = 0 THEN
        RAISE EXCEPTION 'El galpón "%" no puede marcarse como ocupado: no tiene lotes activos.', NEW.nombre;
    END IF;
    IF NEW.estado <> 'OCUPADO' AND v_lotes_activos > 0 THEN
        RAISE EXCEPTION 'El galpón "%" tiene % lote(s) activo(s): no puede pasar a %.', NEW.nombre, v_lotes_activos, NEW.estado;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        v_alojadas := fn_aves_alojadas(NEW.id_galpon);
        IF v_alojadas > NEW.capacidad_maxima THEN
            RAISE EXCEPTION 'La capacidad del galpón "%" (%) no puede ser menor que las aves de sus lotes activos (%).',
                NEW.nombre, NEW.capacidad_maxima, v_alojadas;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------
-- Lote: versión definitiva del trigger (capacidad compartida, aves vendidas
-- y aves comprometidas en pedidos).
-- ---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_trg_lote_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_galpon       galpon%ROWTYPE;
    v_retiradas    INTEGER;
    v_disponibles  INTEGER;
    v_alojadas     INTEGER;
BEGIN
    -- Bloquea el galpón para que dos lotes simultáneos no superen juntos su capacidad.
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
        IF NEW.fecha_ingreso > CURRENT_DATE THEN
            RAISE EXCEPTION 'La fecha de ingreso no puede ser futura.';
        END IF;
        v_alojadas := fn_aves_alojadas(NEW.id_galpon);
        IF v_alojadas + NEW.cantidad_inicial > v_galpon.capacidad_maxima THEN
            RAISE EXCEPTION 'El galpón "%" tiene % aves en sus lotes activos y capacidad para %: no caben % más.',
                v_galpon.nombre, v_alojadas, v_galpon.capacidad_maxima, NEW.cantidad_inicial;
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.codigo <> OLD.codigo OR NEW.id_galpon <> OLD.id_galpon OR NEW.fecha_ingreso <> OLD.fecha_ingreso THEN
        RAISE EXCEPTION 'El código, el galpón y la fecha de ingreso de un lote no se pueden modificar.';
    END IF;

    v_retiradas := fn_mortalidad_acumulada(NEW.id_lote) + fn_aves_vendidas(NEW.id_lote);
    IF NEW.cantidad_inicial <> OLD.cantidad_inicial THEN
        IF NEW.cantidad_inicial < v_retiradas THEN
            RAISE EXCEPTION 'La cantidad inicial (%) no puede ser menor que la mortalidad y las ventas ya registradas (%).',
                NEW.cantidad_inicial, v_retiradas;
        END IF;
        v_alojadas := fn_aves_alojadas(NEW.id_galpon, NEW.id_lote);
        IF NEW.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')
           AND v_alojadas + NEW.cantidad_inicial - v_retiradas > v_galpon.capacidad_maxima THEN
            RAISE EXCEPTION 'Con % aves el lote % supera la capacidad del galpón "%" (% aves de otros lotes, capacidad %).',
                NEW.cantidad_inicial, NEW.codigo, v_galpon.nombre, v_alojadas, v_galpon.capacidad_maxima;
        END IF;
    END IF;

    IF NEW.estado <> OLD.estado THEN
        v_disponibles := NEW.cantidad_inicial - v_retiradas;
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
        IF NEW.estado = 'CERRADO' AND fn_aves_comprometidas(NEW.id_lote) > 0 THEN
            RAISE EXCEPTION 'El lote % tiene aves en pedidos confirmados sin vender: no se puede cerrar.', NEW.codigo;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- El galpón queda ocupado al recibir su primer lote activo y disponible cuando
-- sale el último.
CREATE OR REPLACE FUNCTION fn_trg_lote_galpon()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE galpon SET estado = 'OCUPADO' WHERE id_galpon = NEW.id_galpon AND estado <> 'OCUPADO';
    ELSIF NEW.estado IN ('VENDIDO', 'CERRADO') AND OLD.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')
          AND NOT EXISTS (SELECT 1 FROM lote
                          WHERE id_galpon = NEW.id_galpon AND estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')) THEN
        UPDATE galpon SET estado = 'DISPONIBLE' WHERE id_galpon = NEW.id_galpon;
    END IF;
    RETURN NULL;
END;
$$;

-- Si no se indica la venta estimada, el ingreso más 40 días (mitad del ciclo
-- de 35 a 45).
CREATE OR REPLACE PROCEDURE sp_registrar_lote(
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
                COALESCE(p_fecha_estimada_venta, p_fecha_ingreso + 40))
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

-- ---------------------------------------------------------------------
-- Vistas para el seguimiento por lote y el panel del galpón
-- ---------------------------------------------------------------------

-- Se agregan columnas al final (CREATE OR REPLACE VIEW conserva las existentes).
CREATE OR REPLACE VIEW v_lote_resumen AS
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
       fn_mortalidad_pct(l.id_lote) > fn_mortalidad_referencia_pct() AS alerta_mortalidad,
       fn_aves_vendidas(l.id_lote)                       AS aves_vendidas,
       COALESCE((SELECT SUM(c.cantidad_bultos) FROM consumo_alimento c WHERE c.id_lote = l.id_lote), 0)
                                                        AS consumo_alimento_bultos,
       COALESCE((SELECT SUM(c.cantidad_bultos * t.peso_bulto_kg) FROM consumo_alimento c
                   JOIN tipo_alimento t ON t.id_tipo_alimento = c.id_tipo_alimento
                  WHERE c.id_lote = l.id_lote), 0)      AS consumo_alimento_kg
FROM lote l
JOIN galpon g ON g.id_galpon = l.id_galpon;

-- Ocupación de cada galpón: lotes activos, aves alojadas y espacio libre.
CREATE VIEW v_galpon_ocupacion AS
SELECT g.id_galpon,
       g.nombre,
       g.capacidad_maxima,
       g.estado::text                                    AS estado,
       (SELECT COUNT(*) FROM lote l
         WHERE l.id_galpon = g.id_galpon AND l.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA'))::INTEGER AS lotes_activos,
       fn_aves_alojadas(g.id_galpon)                     AS aves_alojadas,
       g.capacidad_maxima - fn_aves_alojadas(g.id_galpon) AS capacidad_libre,
       (SELECT MIN(l.fecha_estimada_venta) FROM lote l
         WHERE l.id_galpon = g.id_galpon AND l.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')) AS proxima_salida
FROM galpon g;

REVOKE ALL ON v_galpon_ocupacion FROM pollos_app;
GRANT SELECT ON v_lote_resumen, v_galpon_ocupacion TO pollos_app;

RESET ROLE;

COMMIT;
