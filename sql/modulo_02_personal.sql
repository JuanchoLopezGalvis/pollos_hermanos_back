-- =====================================================================
-- MÓDULO 2: PERSONAL (cargos y empleados)
-- Modelo: docs/modelo_relacional.md (módulo 2 del documento).
--   ./instalar_bd.sh modulo_02_personal.sql
--
-- Reglas cubiertas:
--   S24 un cargo por empleado            -> empleado.id_cargo NOT NULL
--   S27 empleado retirado sin borrarse   -> ck_empleado_retiro, sp_retirar_empleado
--   S25, S26 y beneficio: solo empleados activos con el cargo indicado
--                                        -> fn_validar_empleado (la usan los módulos siguientes)
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

CREATE TABLE cargo (
    id_cargo        BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo          VARCHAR(30)  NOT NULL,
    nombre          VARCHAR(60)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cargo PRIMARY KEY (id_cargo),
    CONSTRAINT uq_cargo_codigo UNIQUE (codigo),
    CONSTRAINT uq_cargo_nombre UNIQUE (nombre),
    CONSTRAINT ck_cargo_codigo CHECK (codigo ~ '^[A-Z_]+$')
);

INSERT INTO cargo (codigo, nombre) VALUES
    ('OPERARIO_GALPON',   'Operario de galpón'),
    ('ENCARGADO_SANIDAD', 'Encargado de sanidad'),
    ('TRANSPORTISTA',     'Transportista'),
    ('ADMINISTRADOR',     'Administrador'),
    ('BENEFICIADORA',     'Beneficiadora');

CREATE TABLE empleado (
    id_empleado     BIGINT GENERATED ALWAYS AS IDENTITY,
    documento       VARCHAR(20)  NOT NULL,
    nombres         VARCHAR(80)  NOT NULL,
    apellidos       VARCHAR(80)  NOT NULL,
    telefono        VARCHAR(20),
    id_cargo        BIGINT       NOT NULL,
    fecha_ingreso   DATE         NOT NULL,
    fecha_retiro    DATE,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_empleado PRIMARY KEY (id_empleado),
    CONSTRAINT uq_empleado_documento UNIQUE (documento),
    CONSTRAINT fk_empleado_cargo FOREIGN KEY (id_cargo) REFERENCES cargo (id_cargo),
    CONSTRAINT ck_empleado_fecha_retiro CHECK (fecha_retiro >= fecha_ingreso),
    CONSTRAINT ck_empleado_retiro
        CHECK ((activo AND fecha_retiro IS NULL) OR (NOT activo AND fecha_retiro IS NOT NULL))
);

CREATE INDEX ix_empleado_cargo ON empleado (id_cargo);

-- Valida que un empleado exista, esté activo y tenga el cargo indicado. Lanza
-- una excepción con un mensaje claro si no; la usan los triggers de sanidad,
-- beneficio y entrega.
CREATE FUNCTION fn_validar_empleado(p_id_empleado BIGINT, p_codigo_cargo VARCHAR)
RETURNS VOID
LANGUAGE plpgsql STABLE AS $$
DECLARE
    v_nombre  TEXT;
    v_activo  BOOLEAN;
    v_cargo   TEXT;
    v_esperado TEXT;
BEGIN
    SELECT e.nombres || ' ' || e.apellidos, e.activo, c.codigo
      INTO v_nombre, v_activo, v_cargo
    FROM empleado e JOIN cargo c ON c.id_cargo = e.id_cargo
    WHERE e.id_empleado = p_id_empleado;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El empleado indicado no existe.';
    END IF;
    IF NOT v_activo THEN
        RAISE EXCEPTION 'El empleado % está retirado y no puede asignarse a nuevos registros.', v_nombre;
    END IF;
    IF v_cargo <> p_codigo_cargo THEN
        SELECT nombre INTO v_esperado FROM cargo WHERE codigo = p_codigo_cargo;
        RAISE EXCEPTION 'El empleado % no tiene el cargo de %.', v_nombre, lower(COALESCE(v_esperado, p_codigo_cargo));
    END IF;
END;
$$;

-- Un empleado retirado no vuelve a estar activo ni cambia de cargo: se
-- conserva su historial tal como estaba.
CREATE FUNCTION fn_trg_empleado_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NOT NEW.activo THEN
        RAISE EXCEPTION 'Un empleado nuevo debe registrarse activo.';
    END IF;
    IF TG_OP = 'UPDATE' AND NOT OLD.activo THEN
        IF NEW.activo THEN
            RAISE EXCEPTION 'El empleado % está retirado: no puede reactivarse. Registre un empleado nuevo.', OLD.documento;
        END IF;
        IF NEW.id_cargo <> OLD.id_cargo THEN
            RAISE EXCEPTION 'El empleado % está retirado: su cargo no se puede modificar.', OLD.documento;
        END IF;
    END IF;
    IF (TG_OP = 'INSERT' OR NEW.id_cargo IS DISTINCT FROM OLD.id_cargo)
       AND NOT EXISTS (SELECT 1 FROM cargo WHERE id_cargo = NEW.id_cargo AND activo) THEN
        RAISE EXCEPTION 'El cargo indicado no existe o está inactivo.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_empleado_validar
    BEFORE INSERT OR UPDATE ON empleado
    FOR EACH ROW EXECUTE FUNCTION fn_trg_empleado_validar();

-- Mensajes en español de las restricciones. Desde este módulo viven en una
-- tabla: cada módulo agrega los suyos con INSERT en lugar de reescribir la
-- función. Se cargan también los del módulo 1.
CREATE TABLE mensaje_restriccion (
    restriccion  VARCHAR(63)  NOT NULL,
    mensaje      VARCHAR(300) NOT NULL,
    CONSTRAINT pk_mensaje_restriccion PRIMARY KEY (restriccion)
);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('uq_galpon_nombre',          'Ya existe un galpón con ese nombre.'),
    ('ck_galpon_capacidad',       'La capacidad del galpón debe ser mayor que cero.'),
    ('uq_lote_codigo',            'Ya existe un lote con ese código.'),
    ('ux_lote_activo_por_galpon', 'El galpón ya tiene un lote activo.'),
    ('fk_lote_galpon',            'El galpón indicado no existe.'),
    ('ck_lote_codigo',            'El código del lote solo admite letras mayúsculas, números y guiones.'),
    ('ck_lote_cantidad',          'La cantidad inicial de aves debe ser mayor que cero.'),
    ('ck_lote_costo',             'El costo del pollito no puede ser negativo.'),
    ('ck_lote_fecha_estimada',    'La fecha estimada de venta debe estar entre 40 y 50 días después del ingreso.'),
    ('fk_mortalidad_lote',        'El lote indicado no existe.'),
    ('uq_mortalidad_lote_fecha',  'Ya se registró la mortalidad de ese lote para esa fecha.'),
    ('ck_mortalidad_cantidad',    'La cantidad de aves muertas debe ser mayor que cero.'),
    ('uq_empleado_documento',     'Ya existe un empleado con ese documento.'),
    ('fk_empleado_cargo',         'El cargo indicado no existe.'),
    ('ck_empleado_fecha_retiro',  'La fecha de retiro no puede ser anterior a la de ingreso.'),
    ('ck_empleado_retiro',        'Un empleado retirado debe tener fecha de retiro, y uno activo no.');

CREATE OR REPLACE FUNCTION fn_mensaje_restriccion(p_restriccion TEXT, p_mensaje_original TEXT)
RETURNS TEXT
LANGUAGE sql STABLE AS $$
    SELECT COALESCE((SELECT mensaje FROM mensaje_restriccion WHERE restriccion = p_restriccion),
                    p_mensaje_original);
$$;

-- S27: retira al empleado sin borrarlo; su historial se conserva.
CREATE PROCEDURE sp_retirar_empleado(p_id_empleado BIGINT, p_fecha_retiro DATE)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        UPDATE empleado
           SET activo = FALSE, fecha_retiro = COALESCE(p_fecha_retiro, CURRENT_DATE)
         WHERE id_empleado = p_id_empleado AND activo;
        IF NOT FOUND THEN
            IF EXISTS (SELECT 1 FROM empleado WHERE id_empleado = p_id_empleado) THEN
                RAISE EXCEPTION 'El empleado ya está retirado.';
            END IF;
            RAISE EXCEPTION 'No existe el empleado con id %.', p_id_empleado;
        END IF;
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

CREATE VIEW v_empleado AS
SELECT e.id_empleado, e.documento, e.nombres, e.apellidos, e.telefono, e.id_cargo,
       c.codigo AS codigo_cargo, c.nombre AS cargo, e.fecha_ingreso, e.fecha_retiro, e.activo
FROM empleado e
JOIN cargo c ON c.id_cargo = e.id_cargo;

-- Privilegios: los empleados no se borran, se retiran.
REVOKE ALL ON cargo, empleado, v_empleado, mensaje_restriccion FROM pollos_app;
GRANT SELECT ON cargo, v_empleado, mensaje_restriccion TO pollos_app;
GRANT SELECT, INSERT, UPDATE ON empleado TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_retirar_empleado TO pollos_app;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('personal.ver',        'Consultar los empleados y sus cargos'),
    ('personal.gestionar',  'Registrar, modificar y retirar empleados')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
