-- =====================================================================
-- MÓDULO 4: SANIDAD (vitaminas y medicamentos)
-- Modelo: docs/modelo_relacional.md (módulo 5 del documento).
--   ./instalar_bd.sh modulo_04_sanidad.sql
--
-- Reglas cubiertas:
--   S18 catálogo con presentación y proveedor   -> producto_sanitario, trg_producto_sanitario_validar
--   S19, S20 aplicaciones por lote              -> aplicacion_sanitaria
--   S25 solo un encargado de sanidad activo     -> trg_aplicacion_validar (fn_validar_empleado)
--   Costo cargado al lote (D8)                  -> aplicacion_sanitaria.costo_unitario
--   Sin inventario de medicamentos (fuera del alcance).
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

CREATE TYPE tipo_producto_sanitario AS ENUM ('VITAMINA', 'MEDICAMENTO');

CREATE TABLE producto_sanitario (
    id_producto     BIGINT GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR(100)             NOT NULL,
    tipo            tipo_producto_sanitario  NOT NULL,
    presentacion    VARCHAR(80)              NOT NULL,
    unidad_medida   VARCHAR(20)              NOT NULL,
    id_proveedor    BIGINT                   NOT NULL,
    activo          BOOLEAN                  NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_producto_sanitario PRIMARY KEY (id_producto),
    CONSTRAINT uq_producto_nombre_presentacion UNIQUE (nombre, presentacion),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor)
);

CREATE TABLE compra_sanitaria (
    id_compra_sanitaria  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_producto          BIGINT         NOT NULL,
    fecha                DATE           NOT NULL,
    cantidad             NUMERIC(10,2)  NOT NULL,
    costo_unitario       NUMERIC(14,2)  NOT NULL,
    fecha_creacion       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_compra_sanitaria PRIMARY KEY (id_compra_sanitaria),
    CONSTRAINT fk_compra_sanitaria_producto FOREIGN KEY (id_producto) REFERENCES producto_sanitario (id_producto),
    CONSTRAINT ck_compra_sanitaria_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_compra_sanitaria_costo CHECK (costo_unitario > 0)
);

CREATE TABLE aplicacion_sanitaria (
    id_aplicacion   BIGINT GENERATED ALWAYS AS IDENTITY,
    id_lote         BIGINT         NOT NULL,
    id_producto     BIGINT         NOT NULL,
    id_empleado     BIGINT         NOT NULL,
    fecha           DATE           NOT NULL,
    dosis           NUMERIC(10,2)  NOT NULL,
    costo_unitario  NUMERIC(14,2)  NOT NULL,
    observaciones   VARCHAR(300),
    fecha_creacion  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_aplicacion_sanitaria PRIMARY KEY (id_aplicacion),
    CONSTRAINT fk_aplicacion_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT fk_aplicacion_producto FOREIGN KEY (id_producto) REFERENCES producto_sanitario (id_producto),
    CONSTRAINT fk_aplicacion_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado),
    CONSTRAINT ck_aplicacion_dosis CHECK (dosis > 0)
);

CREATE INDEX ix_compra_sanitaria_producto ON compra_sanitaria (id_producto, fecha);
CREATE INDEX ix_aplicacion_lote ON aplicacion_sanitaria (id_lote);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('uq_producto_nombre_presentacion', 'Ya existe un producto con ese nombre y esa presentación.'),
    ('fk_producto_proveedor',           'El proveedor indicado no existe.'),
    ('fk_compra_sanitaria_producto',    'El producto indicado no existe.'),
    ('ck_compra_sanitaria_cantidad',    'La cantidad comprada debe ser mayor que cero.'),
    ('ck_compra_sanitaria_costo',       'El costo unitario debe ser mayor que cero.'),
    ('fk_aplicacion_lote',              'El lote indicado no existe.'),
    ('fk_aplicacion_producto',          'El producto indicado no existe.'),
    ('fk_aplicacion_empleado',          'El empleado indicado no existe.'),
    ('ck_aplicacion_dosis',             'La dosis debe ser mayor que cero.');

-- Costo promedio ponderado del producto según las compras hasta la fecha dada.
CREATE FUNCTION fn_costo_promedio_producto(p_id_producto BIGINT, p_fecha DATE)
RETURNS NUMERIC(14,2)
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v_costo NUMERIC(14,2);
BEGIN
    SELECT ROUND(SUM(cantidad * costo_unitario) / SUM(cantidad), 2) INTO v_costo
    FROM compra_sanitaria
    WHERE id_producto = p_id_producto AND fecha <= p_fecha;
    IF v_costo IS NULL THEN
        RAISE EXCEPTION 'No hay compras de ese producto registradas hasta el %: no se puede valorar la aplicación.', p_fecha;
    END IF;
    RETURN v_costo;
END;
$$;

-- S18: el proveedor del producto debe suministrar insumos sanitarios.
CREATE FUNCTION fn_trg_producto_sanitario_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM proveedor WHERE id_proveedor = NEW.id_proveedor AND suministra_sanidad) THEN
        RAISE EXCEPTION 'El proveedor indicado no existe o no suministra insumos sanitarios.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_producto_sanitario_validar
    BEFORE INSERT OR UPDATE OF id_proveedor ON producto_sanitario
    FOR EACH ROW EXECUTE FUNCTION fn_trg_producto_sanitario_validar();

CREATE FUNCTION fn_trg_compra_sanitaria_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM producto_sanitario WHERE id_producto = NEW.id_producto AND activo) THEN
        RAISE EXCEPTION 'El producto indicado no existe o está inactivo.';
    END IF;
    IF NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de la compra no puede ser futura.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_compra_sanitaria_validar
    BEFORE INSERT ON compra_sanitaria
    FOR EACH ROW EXECUTE FUNCTION fn_trg_compra_sanitaria_validar();

-- S25: responsable encargado de sanidad activo; lote activo; costo congelado.
CREATE FUNCTION fn_trg_aplicacion_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote lote%ROWTYPE;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Una aplicación sanitaria no se modifica: registre la corrección como una aplicación nueva.';
    END IF;
    PERFORM fn_validar_empleado(NEW.id_empleado, 'ENCARGADO_SANIDAD');

    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF v_lote.estado NOT IN ('EN_ENGORDE', 'LISTO_PARA_VENTA') THEN
        RAISE EXCEPTION 'El lote % está %: solo se registran aplicaciones en lotes activos.', v_lote.codigo, v_lote.estado;
    END IF;
    IF NEW.fecha < v_lote.fecha_ingreso OR NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de la aplicación debe estar entre el ingreso del lote (%) y hoy.', v_lote.fecha_ingreso;
    END IF;

    NEW.costo_unitario := fn_costo_promedio_producto(NEW.id_producto, NEW.fecha);
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_aplicacion_validar
    BEFORE INSERT OR UPDATE ON aplicacion_sanitaria
    FOR EACH ROW EXECUTE FUNCTION fn_trg_aplicacion_validar();

CREATE PROCEDURE sp_registrar_compra_sanitaria(
    p_id_producto     BIGINT,
    p_fecha           DATE,
    p_cantidad        NUMERIC,
    p_costo_unitario  NUMERIC,
    OUT p_id_compra   BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO compra_sanitaria (id_producto, fecha, cantidad, costo_unitario)
        VALUES (p_id_producto, COALESCE(p_fecha, CURRENT_DATE), p_cantidad, p_costo_unitario)
        RETURNING id_compra_sanitaria INTO p_id_compra;
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

CREATE PROCEDURE sp_registrar_aplicacion_sanitaria(
    p_id_lote          BIGINT,
    p_id_producto      BIGINT,
    p_id_empleado      BIGINT,
    p_fecha            DATE,
    p_dosis            NUMERIC,
    p_observaciones    VARCHAR,
    OUT p_id_aplicacion BIGINT,
    OUT p_costo_total  NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO aplicacion_sanitaria (id_lote, id_producto, id_empleado, fecha, dosis, costo_unitario, observaciones)
        VALUES (p_id_lote, p_id_producto, p_id_empleado, COALESCE(p_fecha, CURRENT_DATE), p_dosis, 0,
                NULLIF(TRIM(p_observaciones), ''))
        RETURNING id_aplicacion, ROUND(dosis * costo_unitario, 2) INTO p_id_aplicacion, p_costo_total;
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

CREATE VIEW v_producto_sanitario AS
SELECT p.id_producto, p.nombre, p.tipo::text AS tipo, p.presentacion, p.unidad_medida, p.id_proveedor,
       pr.nombre AS proveedor, p.activo,
       (SELECT ROUND(SUM(c.cantidad * c.costo_unitario) / SUM(c.cantidad), 2)
          FROM compra_sanitaria c WHERE c.id_producto = p.id_producto) AS costo_promedio
FROM producto_sanitario p
JOIN proveedor pr ON pr.id_proveedor = p.id_proveedor;

CREATE VIEW v_compra_sanitaria AS
SELECT c.id_compra_sanitaria, c.fecha, c.id_producto, p.nombre AS producto, p.unidad_medida,
       c.cantidad, c.costo_unitario, ROUND(c.cantidad * c.costo_unitario, 2) AS total
FROM compra_sanitaria c
JOIN producto_sanitario p ON p.id_producto = c.id_producto;

CREATE VIEW v_aplicacion_sanitaria AS
SELECT a.id_aplicacion, a.fecha, a.id_lote, l.codigo AS codigo_lote, a.id_producto, p.nombre AS producto,
       p.unidad_medida, a.dosis, a.costo_unitario, ROUND(a.dosis * a.costo_unitario, 2) AS costo_total,
       a.id_empleado, e.nombres || ' ' || e.apellidos AS responsable, a.observaciones
FROM aplicacion_sanitaria a
JOIN lote l ON l.id_lote = a.id_lote
JOIN producto_sanitario p ON p.id_producto = a.id_producto
JOIN empleado e ON e.id_empleado = a.id_empleado;

REVOKE ALL ON producto_sanitario, compra_sanitaria, aplicacion_sanitaria,
              v_producto_sanitario, v_compra_sanitaria, v_aplicacion_sanitaria FROM pollos_app;
GRANT SELECT, INSERT, UPDATE ON producto_sanitario TO pollos_app;
GRANT SELECT, INSERT ON compra_sanitaria, aplicacion_sanitaria TO pollos_app;
GRANT SELECT ON v_producto_sanitario, v_compra_sanitaria, v_aplicacion_sanitaria TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_registrar_compra_sanitaria, sp_registrar_aplicacion_sanitaria TO pollos_app;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('sanidad.ver',        'Consultar productos, compras y aplicaciones sanitarias'),
    ('sanidad.registrar',  'Registrar productos, compras y aplicaciones sanitarias')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
