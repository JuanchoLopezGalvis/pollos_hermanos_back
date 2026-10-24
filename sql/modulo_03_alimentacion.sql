-- =====================================================================
-- MÓDULO 3: PROVEEDORES Y ALIMENTACIÓN
-- Modelo: docs/modelo_relacional.md (módulo 4 del documento y catálogos).
--   ./instalar_bd.sh modulo_03_alimentacion.sql
--
-- Reglas cubiertas:
--   S15 compras por bultos con costo unitario  -> compra_alimento
--   S16 consumo sin exceder el inventario      -> trg_consumo_alimento_validar, fn_inventario_alimento
--   S17, S21 proveedor de alimento y/o sanidad -> ck_proveedor_suministra
--   Alimento según la edad del lote (etapas)   -> etapa_alimentacion, trg_consumo_alimento_validar
--   D8 costo congelado al consumir             -> consumo_alimento.costo_unitario (promedio ponderado)
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

CREATE TABLE proveedor (
    id_proveedor         BIGINT GENERATED ALWAYS AS IDENTITY,
    nit                  VARCHAR(20)   NOT NULL,
    nombre               VARCHAR(120)  NOT NULL,
    telefono             VARCHAR(20),
    correo               VARCHAR(120),
    direccion            VARCHAR(200),
    suministra_alimento  BOOLEAN       NOT NULL DEFAULT FALSE,
    suministra_sanidad   BOOLEAN       NOT NULL DEFAULT FALSE,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_proveedor_nit UNIQUE (nit),
    CONSTRAINT ck_proveedor_suministra CHECK (suministra_alimento OR suministra_sanidad)
);

CREATE TABLE etapa_alimentacion (
    id_etapa        BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR(40)  NOT NULL,
    orden           SMALLINT     NOT NULL,
    dia_inicio      SMALLINT     NOT NULL,
    dia_fin         SMALLINT,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_etapa_alimentacion PRIMARY KEY (id_etapa),
    CONSTRAINT uq_etapa_nombre UNIQUE (nombre),
    CONSTRAINT uq_etapa_orden UNIQUE (orden),
    CONSTRAINT ck_etapa_orden CHECK (orden > 0),
    CONSTRAINT ck_etapa_dia_inicio CHECK (dia_inicio >= 1),
    CONSTRAINT ck_etapa_dias CHECK (dia_fin IS NULL OR dia_fin >= dia_inicio)
);

INSERT INTO etapa_alimentacion (nombre, orden, dia_inicio, dia_fin) VALUES
    ('Preiniciación', 1, 1, 7),
    ('Iniciación',    2, 8, 21),
    ('Engorde',       3, 22, 35),
    ('Finalización',  4, 36, NULL);

CREATE TABLE tipo_alimento (
    id_tipo_alimento  BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre            VARCHAR(80)   NOT NULL,
    id_etapa          BIGINT        NOT NULL,
    peso_bulto_kg     NUMERIC(6,2)  NOT NULL DEFAULT 40,
    activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tipo_alimento PRIMARY KEY (id_tipo_alimento),
    CONSTRAINT uq_tipo_alimento_nombre UNIQUE (nombre),
    CONSTRAINT fk_tipo_alimento_etapa FOREIGN KEY (id_etapa) REFERENCES etapa_alimentacion (id_etapa),
    CONSTRAINT ck_tipo_alimento_peso CHECK (peso_bulto_kg > 0)
);

INSERT INTO tipo_alimento (nombre, id_etapa)
SELECT t.nombre, e.id_etapa
FROM (VALUES ('Preiniciador', 'Preiniciación'), ('Iniciador', 'Iniciación'),
             ('Engorde', 'Engorde'), ('Finalizador', 'Finalización')) AS t (nombre, etapa)
JOIN etapa_alimentacion e ON e.nombre = t.etapa;

CREATE TABLE compra_alimento (
    id_compra_alimento  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_proveedor        BIGINT         NOT NULL,
    id_tipo_alimento    BIGINT         NOT NULL,
    fecha               DATE           NOT NULL,
    cantidad_bultos     INTEGER        NOT NULL,
    costo_unitario      NUMERIC(14,2)  NOT NULL,
    fecha_creacion      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_compra_alimento PRIMARY KEY (id_compra_alimento),
    CONSTRAINT fk_compra_alimento_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor),
    CONSTRAINT fk_compra_alimento_tipo FOREIGN KEY (id_tipo_alimento) REFERENCES tipo_alimento (id_tipo_alimento),
    CONSTRAINT ck_compra_alimento_bultos CHECK (cantidad_bultos > 0),
    CONSTRAINT ck_compra_alimento_costo CHECK (costo_unitario > 0)
);

CREATE TABLE consumo_alimento (
    id_consumo_alimento  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_lote              BIGINT         NOT NULL,
    id_tipo_alimento     BIGINT         NOT NULL,
    fecha                DATE           NOT NULL,
    cantidad_bultos      NUMERIC(8,2)   NOT NULL,
    costo_unitario       NUMERIC(14,2)  NOT NULL,
    fecha_creacion       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_consumo_alimento PRIMARY KEY (id_consumo_alimento),
    CONSTRAINT fk_consumo_alimento_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT fk_consumo_alimento_tipo FOREIGN KEY (id_tipo_alimento) REFERENCES tipo_alimento (id_tipo_alimento),
    CONSTRAINT uq_consumo_lote_tipo_fecha UNIQUE (id_lote, id_tipo_alimento, fecha),
    CONSTRAINT ck_consumo_alimento_bultos CHECK (cantidad_bultos > 0)
);

CREATE INDEX ix_compra_alimento_tipo ON compra_alimento (id_tipo_alimento, fecha);
CREATE INDEX ix_consumo_alimento_tipo ON consumo_alimento (id_tipo_alimento);
CREATE INDEX ix_consumo_alimento_lote ON consumo_alimento (id_lote);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('uq_proveedor_nit',              'Ya existe un proveedor con ese NIT.'),
    ('ck_proveedor_suministra',       'El proveedor debe suministrar alimento, insumos sanitarios o ambos.'),
    ('uq_tipo_alimento_nombre',       'Ya existe un tipo de alimento con ese nombre.'),
    ('fk_tipo_alimento_etapa',        'La etapa de alimentación indicada no existe.'),
    ('ck_tipo_alimento_peso',         'El peso del bulto debe ser mayor que cero.'),
    ('fk_compra_alimento_proveedor',  'El proveedor indicado no existe.'),
    ('fk_compra_alimento_tipo',       'El tipo de alimento indicado no existe.'),
    ('ck_compra_alimento_bultos',     'La cantidad de bultos debe ser mayor que cero.'),
    ('ck_compra_alimento_costo',      'El costo del bulto debe ser mayor que cero.'),
    ('fk_consumo_alimento_lote',      'El lote indicado no existe.'),
    ('fk_consumo_alimento_tipo',      'El tipo de alimento indicado no existe.'),
    ('uq_consumo_lote_tipo_fecha',    'Ya se registró el consumo de ese alimento para ese lote en esa fecha.'),
    ('ck_consumo_alimento_bultos',    'La cantidad de bultos consumidos debe ser mayor que cero.');

-- ---------------------------------------------------------------------
-- Funciones
-- ---------------------------------------------------------------------

-- S16: bultos en bodega = comprados - consumidos.
CREATE FUNCTION fn_inventario_alimento(p_id_tipo_alimento BIGINT)
RETURNS NUMERIC
LANGUAGE sql STABLE AS $$
    SELECT COALESCE((SELECT SUM(cantidad_bultos) FROM compra_alimento WHERE id_tipo_alimento = p_id_tipo_alimento), 0)
         - COALESCE((SELECT SUM(cantidad_bultos) FROM consumo_alimento WHERE id_tipo_alimento = p_id_tipo_alimento), 0);
$$;

-- Costo promedio ponderado del bulto según las compras hasta la fecha dada.
CREATE FUNCTION fn_costo_promedio_alimento(p_id_tipo_alimento BIGINT, p_fecha DATE)
RETURNS NUMERIC(14,2)
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v_costo NUMERIC(14,2);
BEGIN
    SELECT ROUND(SUM(cantidad_bultos * costo_unitario) / SUM(cantidad_bultos), 2) INTO v_costo
    FROM compra_alimento
    WHERE id_tipo_alimento = p_id_tipo_alimento AND fecha <= p_fecha;
    IF v_costo IS NULL THEN
        RAISE EXCEPTION 'No hay compras de ese alimento registradas hasta el %: no se puede valorar el consumo.', p_fecha;
    END IF;
    RETURN v_costo;
END;
$$;

-- Etapa de alimentación que corresponde a una edad en días.
CREATE FUNCTION fn_etapa_por_edad(p_edad INTEGER)
RETURNS BIGINT
LANGUAGE sql STABLE AS $$
    SELECT id_etapa FROM etapa_alimentacion
    WHERE p_edad >= dia_inicio AND (dia_fin IS NULL OR p_edad <= dia_fin)
    ORDER BY orden
    LIMIT 1;
$$;

-- ---------------------------------------------------------------------
-- Triggers
-- ---------------------------------------------------------------------

-- La compra debe venir de un proveedor activo que suministre alimento.
CREATE FUNCTION fn_trg_compra_alimento_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM proveedor WHERE id_proveedor = NEW.id_proveedor AND activo AND suministra_alimento) THEN
        RAISE EXCEPTION 'El proveedor indicado no existe, está inactivo o no suministra alimento.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM tipo_alimento WHERE id_tipo_alimento = NEW.id_tipo_alimento AND activo) THEN
        RAISE EXCEPTION 'El tipo de alimento indicado no existe o está inactivo.';
    END IF;
    IF NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de la compra no puede ser futura.';
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.cantidad_bultos < OLD.cantidad_bultos
       AND fn_inventario_alimento(NEW.id_tipo_alimento) - (OLD.cantidad_bultos - NEW.cantidad_bultos) < 0 THEN
        RAISE EXCEPTION 'No se puede reducir la compra: esos bultos ya se consumieron.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_compra_alimento_validar
    BEFORE INSERT OR UPDATE ON compra_alimento
    FOR EACH ROW EXECUTE FUNCTION fn_trg_compra_alimento_validar();

-- Consumo: lote activo, fecha dentro del ciclo, alimento de la etapa que
-- corresponde a la edad del lote, inventario suficiente y costo congelado.
CREATE FUNCTION fn_trg_consumo_alimento_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote        lote%ROWTYPE;
    v_edad        INTEGER;
    v_etapa_tipo  BIGINT;
    v_etapa_edad  BIGINT;
    v_nombre_tipo TEXT;
    v_nombre_etapa TEXT;
    v_inventario  NUMERIC;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Un consumo de alimento no se modifica: registre la corrección como un consumo nuevo de otra fecha.';
    END IF;

    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF v_lote.estado NOT IN ('EN_ENGORDE', 'LISTO_PARA_VENTA') THEN
        RAISE EXCEPTION 'El lote % está %: solo se registra consumo en lotes activos.', v_lote.codigo, v_lote.estado;
    END IF;
    IF NEW.fecha < v_lote.fecha_ingreso OR NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha del consumo debe estar entre el ingreso del lote (%) y hoy.', v_lote.fecha_ingreso;
    END IF;

    SELECT t.id_etapa, t.nombre INTO v_etapa_tipo, v_nombre_tipo
    FROM tipo_alimento t WHERE t.id_tipo_alimento = NEW.id_tipo_alimento;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El tipo de alimento indicado no existe.';
    END IF;
    v_edad := NEW.fecha - v_lote.fecha_ingreso + 1;
    v_etapa_edad := fn_etapa_por_edad(v_edad);
    IF v_etapa_edad IS DISTINCT FROM v_etapa_tipo THEN
        SELECT nombre INTO v_nombre_etapa FROM etapa_alimentacion WHERE id_etapa = v_etapa_edad;
        RAISE EXCEPTION 'El lote % tiene % días ese día y está en la etapa de %: el alimento "%" no corresponde.',
            v_lote.codigo, v_edad, lower(COALESCE(v_nombre_etapa, 'ninguna')), v_nombre_tipo;
    END IF;

    -- Bloquea las compras del tipo para que dos consumos simultáneos no
    -- superen juntos el inventario.
    PERFORM 1 FROM tipo_alimento WHERE id_tipo_alimento = NEW.id_tipo_alimento FOR UPDATE;
    v_inventario := fn_inventario_alimento(NEW.id_tipo_alimento);
    IF NEW.cantidad_bultos > v_inventario THEN
        RAISE EXCEPTION 'El consumo (% bultos) supera el inventario de "%" (% bultos).',
            NEW.cantidad_bultos, v_nombre_tipo, v_inventario;
    END IF;

    NEW.costo_unitario := fn_costo_promedio_alimento(NEW.id_tipo_alimento, NEW.fecha);
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_consumo_alimento_validar
    BEFORE INSERT OR UPDATE ON consumo_alimento
    FOR EACH ROW EXECUTE FUNCTION fn_trg_consumo_alimento_validar();

-- ---------------------------------------------------------------------
-- Procedimientos
-- ---------------------------------------------------------------------

CREATE PROCEDURE sp_registrar_compra_alimento(
    p_id_proveedor      BIGINT,
    p_id_tipo_alimento  BIGINT,
    p_fecha             DATE,
    p_cantidad_bultos   INTEGER,
    p_costo_unitario    NUMERIC,
    OUT p_id_compra     BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO compra_alimento (id_proveedor, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
        VALUES (p_id_proveedor, p_id_tipo_alimento, COALESCE(p_fecha, CURRENT_DATE), p_cantidad_bultos, p_costo_unitario)
        RETURNING id_compra_alimento INTO p_id_compra;
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

CREATE PROCEDURE sp_registrar_consumo_alimento(
    p_id_lote           BIGINT,
    p_id_tipo_alimento  BIGINT,
    p_fecha             DATE,
    p_cantidad_bultos   NUMERIC,
    OUT p_id_consumo    BIGINT,
    OUT p_costo_unitario NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO consumo_alimento (id_lote, id_tipo_alimento, fecha, cantidad_bultos, costo_unitario)
        VALUES (p_id_lote, p_id_tipo_alimento, COALESCE(p_fecha, CURRENT_DATE), p_cantidad_bultos, 0)
        RETURNING id_consumo_alimento, costo_unitario INTO p_id_consumo, p_costo_unitario;
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

CREATE VIEW v_inventario_alimento AS
SELECT t.id_tipo_alimento,
       t.nombre,
       e.nombre                                         AS etapa,
       e.dia_inicio,
       e.dia_fin,
       t.peso_bulto_kg,
       t.activo,
       COALESCE((SELECT SUM(cantidad_bultos) FROM compra_alimento c WHERE c.id_tipo_alimento = t.id_tipo_alimento), 0) AS bultos_comprados,
       COALESCE((SELECT SUM(cantidad_bultos) FROM consumo_alimento c WHERE c.id_tipo_alimento = t.id_tipo_alimento), 0) AS bultos_consumidos,
       fn_inventario_alimento(t.id_tipo_alimento)       AS bultos_disponibles,
       (SELECT ROUND(SUM(cantidad_bultos * costo_unitario) / SUM(cantidad_bultos), 2)
          FROM compra_alimento c WHERE c.id_tipo_alimento = t.id_tipo_alimento) AS costo_promedio
FROM tipo_alimento t
JOIN etapa_alimentacion e ON e.id_etapa = t.id_etapa;

CREATE VIEW v_compra_alimento AS
SELECT c.id_compra_alimento, c.fecha, c.id_proveedor, p.nombre AS proveedor, c.id_tipo_alimento,
       t.nombre AS tipo_alimento, c.cantidad_bultos, c.costo_unitario, c.cantidad_bultos * c.costo_unitario AS total
FROM compra_alimento c
JOIN proveedor p ON p.id_proveedor = c.id_proveedor
JOIN tipo_alimento t ON t.id_tipo_alimento = c.id_tipo_alimento;

CREATE VIEW v_consumo_alimento AS
SELECT c.id_consumo_alimento, c.fecha, c.id_lote, l.codigo AS codigo_lote, c.fecha - l.fecha_ingreso + 1 AS edad_dias,
       c.id_tipo_alimento, t.nombre AS tipo_alimento, c.cantidad_bultos, c.costo_unitario,
       ROUND(c.cantidad_bultos * c.costo_unitario, 2) AS costo_total
FROM consumo_alimento c
JOIN lote l ON l.id_lote = c.id_lote
JOIN tipo_alimento t ON t.id_tipo_alimento = c.id_tipo_alimento;

-- ---------------------------------------------------------------------
-- Privilegios: las compras y consumos no se borran.
-- ---------------------------------------------------------------------

REVOKE ALL ON proveedor, etapa_alimentacion, tipo_alimento, compra_alimento, consumo_alimento,
              v_inventario_alimento, v_compra_alimento, v_consumo_alimento FROM pollos_app;
GRANT SELECT, INSERT, UPDATE ON proveedor, tipo_alimento TO pollos_app;
GRANT SELECT ON etapa_alimentacion, v_inventario_alimento, v_compra_alimento, v_consumo_alimento TO pollos_app;
GRANT SELECT, INSERT ON compra_alimento, consumo_alimento TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_registrar_compra_alimento, sp_registrar_consumo_alimento TO pollos_app;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('proveedor.gestionar',     'Registrar y modificar proveedores'),
    ('alimentacion.ver',        'Consultar inventario, compras y consumo de alimento'),
    ('alimentacion.registrar',  'Registrar compras y consumo de alimento')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
