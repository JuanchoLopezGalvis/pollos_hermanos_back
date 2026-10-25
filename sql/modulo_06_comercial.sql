-- =====================================================================
-- MÓDULO 6: COMERCIAL (clientes, pedidos, ventas, pagos, entregas, cartera)
-- Modelo: docs/modelo_relacional.md (módulo 7 del documento).
--   ./instalar_bd.sh modulo_06_comercial.sql
--
-- Reglas cubiertas:
--   S28-S30 clientes con documento y contacto único  -> cliente, ck_cliente_contacto
--   S31, S32 pedidos de uno o varios lotes (N:M)     -> pedido, detalle_pedido
--   S11 aves disponibles restan las vendidas          -> fn_aves_disponibles (reemplaza la del módulo 1)
--   S12 lote vendido al llegar a cero                 -> trg_venta_lotes
--   S13 venta solo de lotes listos para venta         -> trg_venta_validar
--   S33, S34 precio por libra fijo y valor total      -> venta.valor_total (columna generada)
--   Ajuste: la venta se pesa en libras (peso total y precio por libra)
--   D5 una venta por pedido (1:1)                     -> uq_venta_pedido
--   S35-S37 pagos parciales y saldo                   -> pago, fn_saldo_venta, trg_pago_validar
--   S38 cartera vencida (por el paso del tiempo)      -> fn_cartera_vencida, sp_marcar_cartera_vencida
--   S39-S41 una entrega por venta, transportista      -> entrega, trg_entrega_validar
--   D6 ingreso por lote proporcional a sus aves       -> fn_ingreso_lote, v_rentabilidad_lote
--   Otros ingresos del lote (p. ej. gallinaza)         -> otro_ingreso, sp_registrar_otro_ingreso
--
-- Se instala antes de modulo_07_produccion_escalonada.sql, que ajusta los
-- lotes y galpones y deja el trigger de lote definitivo.
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- Catálogos y tipos
-- ---------------------------------------------------------------------

CREATE TABLE tipo_cliente (
    id_tipo_cliente  BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo           VARCHAR(30)  NOT NULL,
    nombre           VARCHAR(60)  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tipo_cliente PRIMARY KEY (id_tipo_cliente),
    CONSTRAINT uq_tipo_cliente_codigo UNIQUE (codigo),
    CONSTRAINT uq_tipo_cliente_nombre UNIQUE (nombre)
);

INSERT INTO tipo_cliente (codigo, nombre) VALUES
    ('TIENDA', 'Tienda'), ('RESTAURANTE', 'Restaurante'), ('PERSONA_NATURAL', 'Persona natural');

CREATE TABLE metodo_pago (
    id_metodo_pago  BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo          VARCHAR(30)  NOT NULL,
    nombre          VARCHAR(60)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_metodo_pago PRIMARY KEY (id_metodo_pago),
    CONSTRAINT uq_metodo_pago_codigo UNIQUE (codigo),
    CONSTRAINT uq_metodo_pago_nombre UNIQUE (nombre)
);

INSERT INTO metodo_pago (codigo, nombre) VALUES
    ('EFECTIVO', 'Efectivo'), ('TRANSFERENCIA', 'Transferencia'), ('CREDITO', 'Crédito'),
    ('EN_LINEA', 'Pago en línea (pasarela de pruebas)');

CREATE TYPE estado_pedido AS ENUM ('PENDIENTE', 'CONFIRMADO', 'ENTREGADO', 'CANCELADO');
CREATE TYPE estado_venta AS ENUM ('PENDIENTE_PAGO', 'PAGADA', 'VENCIDA');
CREATE TYPE estado_entrega AS ENUM ('PENDIENTE', 'ENTREGADO', 'DEVUELTO');

-- ---------------------------------------------------------------------
-- Tablas
-- ---------------------------------------------------------------------

CREATE TABLE cliente (
    id_cliente       BIGINT GENERATED ALWAYS AS IDENTITY,
    id_tipo_cliente  BIGINT        NOT NULL,
    documento        VARCHAR(20)   NOT NULL,
    nombre           VARCHAR(120)  NOT NULL,
    direccion        VARCHAR(200),
    telefono         VARCHAR(20),
    correo           VARCHAR(120),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT fk_cliente_tipo FOREIGN KEY (id_tipo_cliente) REFERENCES tipo_cliente (id_tipo_cliente),
    CONSTRAINT uq_cliente_documento UNIQUE (documento),
    CONSTRAINT uq_cliente_telefono UNIQUE (telefono),
    CONSTRAINT uq_cliente_correo UNIQUE (correo),
    CONSTRAINT ck_cliente_contacto CHECK (telefono IS NOT NULL OR correo IS NOT NULL)
);

CREATE TABLE pedido (
    id_pedido               BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cliente              BIGINT         NOT NULL,
    fecha_pedido            DATE           NOT NULL DEFAULT CURRENT_DATE,
    fecha_estimada_entrega  DATE           NOT NULL,
    estado                  estado_pedido  NOT NULL DEFAULT 'PENDIENTE',
    observaciones           VARCHAR(300),
    fecha_creacion          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT ck_pedido_fecha_entrega CHECK (fecha_estimada_entrega >= fecha_pedido)
);

-- N:M pedido–lote: un pedido toma aves de uno o varios lotes.
CREATE TABLE detalle_pedido (
    id_pedido       BIGINT     NOT NULL,
    id_lote         BIGINT     NOT NULL,
    cantidad_aves   INTEGER    NOT NULL,
    fecha_creacion  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_detalle_pedido PRIMARY KEY (id_pedido, id_lote),
    CONSTRAINT fk_detalle_pedido_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_pedido_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT ck_detalle_pedido_cantidad CHECK (cantidad_aves > 0)
);

-- 1:1 con pedido (D5).
CREATE TABLE venta (
    id_venta           BIGINT GENERATED ALWAYS AS IDENTITY,
    id_pedido          BIGINT          NOT NULL,
    fecha_venta        DATE            NOT NULL DEFAULT CURRENT_DATE,
    peso_total_libras  NUMERIC(10,2)   NOT NULL,
    precio_libra       NUMERIC(14,2)   NOT NULL,
    valor_total        NUMERIC(16,2)   GENERATED ALWAYS AS (ROUND(peso_total_libras * precio_libra, 2)) STORED,
    fecha_limite_pago  DATE            NOT NULL,
    estado             estado_venta    NOT NULL DEFAULT 'PENDIENTE_PAGO',
    fecha_creacion     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_venta PRIMARY KEY (id_venta),
    CONSTRAINT uq_venta_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_venta_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT ck_venta_peso CHECK (peso_total_libras > 0),
    CONSTRAINT ck_venta_precio CHECK (precio_libra > 0),
    CONSTRAINT ck_venta_fecha_limite CHECK (fecha_limite_pago >= fecha_venta)
);

CREATE TABLE pago (
    id_pago              BIGINT GENERATED ALWAYS AS IDENTITY,
    id_venta             BIGINT         NOT NULL,
    id_metodo_pago       BIGINT         NOT NULL,
    fecha                DATE           NOT NULL DEFAULT CURRENT_DATE,
    monto                NUMERIC(14,2)  NOT NULL,
    referencia_pasarela  VARCHAR(100),
    fecha_creacion       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT fk_pago_venta FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago (id_metodo_pago),
    CONSTRAINT uq_pago_referencia UNIQUE (referencia_pasarela),
    CONSTRAINT ck_pago_monto CHECK (monto > 0)
);

-- 1:1 con venta.
CREATE TABLE entrega (
    id_entrega      BIGINT GENERATED ALWAYS AS IDENTITY,
    id_venta        BIGINT          NOT NULL,
    id_empleado     BIGINT          NOT NULL,
    direccion       VARCHAR(200)    NOT NULL,
    fecha_entrega   DATE,
    estado          estado_entrega  NOT NULL DEFAULT 'PENDIENTE',
    observaciones   VARCHAR(300),
    fecha_creacion  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_entrega PRIMARY KEY (id_entrega),
    CONSTRAINT uq_entrega_venta UNIQUE (id_venta),
    CONSTRAINT fk_entrega_venta FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_entrega_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado),
    CONSTRAINT ck_entrega_fecha CHECK (estado <> 'ENTREGADO' OR fecha_entrega IS NOT NULL)
);

-- Otros ingresos atribuibles a un lote, distintos de la venta de aves (por
-- ejemplo, la venta de gallinaza). Suman en la ganancia del lote.
CREATE TABLE otro_ingreso (
    id_otro_ingreso  BIGINT GENERATED ALWAYS AS IDENTITY,
    id_lote          BIGINT         NOT NULL,
    fecha            DATE           NOT NULL,
    monto            NUMERIC(14,2)  NOT NULL,
    descripcion      VARCHAR(200)   NOT NULL,
    fecha_creacion   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_otro_ingreso PRIMARY KEY (id_otro_ingreso),
    CONSTRAINT fk_otro_ingreso_lote FOREIGN KEY (id_lote) REFERENCES lote (id_lote),
    CONSTRAINT ck_otro_ingreso_monto CHECK (monto > 0),
    CONSTRAINT ck_otro_ingreso_descripcion CHECK (TRIM(descripcion) <> '')
);

CREATE INDEX ix_pedido_cliente ON pedido (id_cliente);
CREATE INDEX ix_otro_ingreso_lote ON otro_ingreso (id_lote);
CREATE INDEX ix_detalle_pedido_lote ON detalle_pedido (id_lote);
CREATE INDEX ix_pago_venta ON pago (id_venta);
CREATE INDEX ix_venta_cartera ON venta (fecha_limite_pago) WHERE estado <> 'PAGADA';

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('fk_cliente_tipo',             'El tipo de cliente indicado no existe.'),
    ('uq_cliente_documento',        'Ya existe un cliente con ese documento o NIT.'),
    ('uq_cliente_telefono',         'Ya existe un cliente con ese teléfono.'),
    ('uq_cliente_correo',           'Ya existe un cliente con ese correo.'),
    ('ck_cliente_contacto',         'El cliente debe tener teléfono o correo de contacto.'),
    ('fk_pedido_cliente',           'El cliente indicado no existe.'),
    ('ck_pedido_fecha_entrega',     'La fecha estimada de entrega no puede ser anterior a la del pedido.'),
    ('pk_detalle_pedido',           'Un lote aparece dos veces en el mismo pedido.'),
    ('fk_detalle_pedido_lote',      'El lote indicado no existe.'),
    ('ck_detalle_pedido_cantidad',  'La cantidad de aves de cada lote debe ser mayor que cero.'),
    ('uq_venta_pedido',             'Ese pedido ya tiene una venta registrada.'),
    ('fk_venta_pedido',             'El pedido indicado no existe.'),
    ('ck_venta_peso',               'El peso total debe ser mayor que cero.'),
    ('ck_venta_precio',             'El precio por libra debe ser mayor que cero.'),
    ('ck_venta_fecha_limite',       'La fecha límite de pago no puede ser anterior a la venta.'),
    ('fk_pago_venta',               'La venta indicada no existe.'),
    ('fk_pago_metodo',              'El método de pago indicado no existe.'),
    ('uq_pago_referencia',          'Ese pago en línea ya fue registrado.'),
    ('ck_pago_monto',               'El monto del pago debe ser mayor que cero.'),
    ('uq_entrega_venta',            'Esa venta ya tiene una entrega registrada.'),
    ('fk_entrega_venta',            'La venta indicada no existe.'),
    ('fk_entrega_empleado',         'El transportista indicado no existe.'),
    ('ck_entrega_fecha',            'Una entrega realizada debe tener fecha de entrega.'),
    ('fk_otro_ingreso_lote',        'El lote indicado no existe.'),
    ('ck_otro_ingreso_monto',       'El monto del ingreso debe ser mayor que cero.'),
    ('ck_otro_ingreso_descripcion', 'La descripción del ingreso es obligatoria.');

-- ---------------------------------------------------------------------
-- Funciones
-- ---------------------------------------------------------------------

-- Aves vendidas de un lote: las de los pedidos que ya tienen venta.
CREATE FUNCTION fn_aves_vendidas(p_id_lote BIGINT)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(d.cantidad_aves), 0)::INTEGER
    FROM detalle_pedido d JOIN venta v ON v.id_pedido = d.id_pedido
    WHERE d.id_lote = p_id_lote;
$$;

-- S11 completa: cantidad inicial - mortalidad - vendidas.
CREATE OR REPLACE FUNCTION fn_aves_disponibles(p_id_lote BIGINT)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT l.cantidad_inicial - fn_mortalidad_acumulada(l.id_lote) - fn_aves_vendidas(l.id_lote)
    FROM lote l
    WHERE l.id_lote = p_id_lote;
$$;

-- Aves apartadas en pedidos confirmados que aún no se venden. Se puede
-- excluir un pedido (el que se está confirmando).
CREATE FUNCTION fn_aves_comprometidas(p_id_lote BIGINT, p_excluir_pedido BIGINT DEFAULT NULL)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(d.cantidad_aves), 0)::INTEGER
    FROM detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
    WHERE d.id_lote = p_id_lote
      AND p.estado = 'CONFIRMADO'
      AND p.id_pedido IS DISTINCT FROM p_excluir_pedido
      AND NOT EXISTS (SELECT 1 FROM venta v WHERE v.id_pedido = p.id_pedido);
$$;

-- S37: saldo = valor total - pagos.
CREATE FUNCTION fn_saldo_venta(p_id_venta BIGINT)
RETURNS NUMERIC(16,2)
LANGUAGE sql STABLE AS $$
    SELECT v.valor_total - COALESCE((SELECT SUM(monto) FROM pago WHERE id_venta = v.id_venta), 0)
    FROM venta v WHERE v.id_venta = p_id_venta;
$$;

-- D6: ingreso por ventas y libras atribuidos a un lote, en proporción a sus
-- aves dentro de cada pedido vendido.
CREATE FUNCTION fn_ingreso_lote(p_id_lote BIGINT)
RETURNS TABLE (ingreso NUMERIC, libras NUMERIC)
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(ROUND(v.valor_total * d.cantidad_aves / t.aves, 2)), 0),
           COALESCE(SUM(ROUND(v.peso_total_libras * d.cantidad_aves / t.aves, 2)), 0)
    FROM detalle_pedido d
    JOIN venta v ON v.id_pedido = d.id_pedido
    JOIN (SELECT id_pedido, SUM(cantidad_aves) AS aves FROM detalle_pedido GROUP BY id_pedido) t
      ON t.id_pedido = d.id_pedido
    WHERE d.id_lote = p_id_lote;
$$;

-- S38: ventas con saldo después de su fecha límite, se hayan marcado o no.
CREATE FUNCTION fn_cartera_vencida(p_fecha DATE DEFAULT CURRENT_DATE)
RETURNS TABLE (id_venta BIGINT, id_cliente BIGINT, cliente VARCHAR, fecha_limite_pago DATE,
               dias_vencida INTEGER, valor_total NUMERIC, saldo NUMERIC)
LANGUAGE sql STABLE AS $$
    SELECT v.id_venta, c.id_cliente, c.nombre, v.fecha_limite_pago, p_fecha - v.fecha_limite_pago,
           v.valor_total, fn_saldo_venta(v.id_venta)
    FROM venta v
    JOIN pedido p ON p.id_pedido = v.id_pedido
    JOIN cliente c ON c.id_cliente = p.id_cliente
    WHERE v.fecha_limite_pago < p_fecha
      AND fn_saldo_venta(v.id_venta) > 0
    ORDER BY v.fecha_limite_pago;
$$;

-- ---------------------------------------------------------------------
-- Triggers
-- ---------------------------------------------------------------------

-- Pedido: cliente activo al crearlo y transiciones de estado válidas.
CREATE FUNCTION fn_trg_pedido_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'PENDIENTE' THEN
            RAISE EXCEPTION 'Un pedido nuevo debe iniciar pendiente.';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_cliente = NEW.id_cliente AND activo) THEN
            RAISE EXCEPTION 'El cliente indicado no existe o está inactivo.';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.id_cliente <> OLD.id_cliente THEN
        RAISE EXCEPTION 'El cliente de un pedido no se puede cambiar.';
    END IF;
    IF NEW.estado <> OLD.estado THEN
        IF NOT (
               (OLD.estado = 'PENDIENTE'  AND NEW.estado IN ('CONFIRMADO', 'CANCELADO'))
            OR (OLD.estado = 'CONFIRMADO' AND NEW.estado IN ('ENTREGADO', 'CANCELADO'))
        ) THEN
            RAISE EXCEPTION 'El pedido % no puede pasar de % a %.', NEW.id_pedido, OLD.estado, NEW.estado;
        END IF;
        IF NEW.estado = 'CANCELADO' AND EXISTS (SELECT 1 FROM venta WHERE id_pedido = NEW.id_pedido) THEN
            RAISE EXCEPTION 'El pedido % ya tiene una venta: no se puede cancelar.', NEW.id_pedido;
        END IF;
        IF NEW.estado = 'CONFIRMADO' AND NOT EXISTS (SELECT 1 FROM detalle_pedido WHERE id_pedido = NEW.id_pedido) THEN
            RAISE EXCEPTION 'El pedido % no tiene aves: no se puede confirmar.', NEW.id_pedido;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pedido_validar
    BEFORE INSERT OR UPDATE ON pedido
    FOR EACH ROW EXECUTE FUNCTION fn_trg_pedido_validar();

-- Detalle: solo en pedidos pendientes, de lotes listos para venta y sin
-- superar las aves libres (disponibles menos comprometidas).
CREATE FUNCTION fn_trg_detalle_pedido_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_estado_pedido estado_pedido;
    v_lote          lote%ROWTYPE;
    v_libres        INTEGER;
BEGIN
    SELECT estado INTO v_estado_pedido FROM pedido WHERE id_pedido = NEW.id_pedido;
    IF v_estado_pedido IS DISTINCT FROM 'PENDIENTE' THEN
        RAISE EXCEPTION 'El pedido % está %: solo se modifican las aves de pedidos pendientes.', NEW.id_pedido, v_estado_pedido;
    END IF;
    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF v_lote.estado <> 'LISTO_PARA_VENTA' THEN
        RAISE EXCEPTION 'El lote % está %: solo se piden aves de lotes listos para venta.', v_lote.codigo, v_lote.estado;
    END IF;
    v_libres := fn_aves_disponibles(NEW.id_lote) - fn_aves_comprometidas(NEW.id_lote);
    IF NEW.cantidad_aves > v_libres THEN
        RAISE EXCEPTION 'El lote % tiene % aves libres para pedidos: no alcanza para %.', v_lote.codigo, v_libres, NEW.cantidad_aves;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_detalle_pedido_validar
    BEFORE INSERT OR UPDATE ON detalle_pedido
    FOR EACH ROW EXECUTE FUNCTION fn_trg_detalle_pedido_validar();

-- Venta: pedido confirmado (S33), lotes listos para venta (S13) y aves
-- suficientes. No se modifica después de registrada (S34), salvo su estado.
CREATE FUNCTION fn_trg_venta_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_estado_pedido estado_pedido;
    v_linea RECORD;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.id_pedido <> OLD.id_pedido OR NEW.fecha_venta <> OLD.fecha_venta OR NEW.peso_total_libras <> OLD.peso_total_libras
           OR NEW.precio_libra <> OLD.precio_libra OR NEW.fecha_limite_pago <> OLD.fecha_limite_pago THEN
            RAISE EXCEPTION 'Una venta registrada no se modifica: el precio por libra y el valor quedan fijos.';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.estado <> 'PENDIENTE_PAGO' THEN
        RAISE EXCEPTION 'Una venta nueva debe iniciar pendiente de pago.';
    END IF;
    IF NEW.fecha_venta > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de la venta no puede ser futura.';
    END IF;
    SELECT estado INTO v_estado_pedido FROM pedido WHERE id_pedido = NEW.id_pedido;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El pedido indicado no existe.';
    END IF;
    IF v_estado_pedido <> 'CONFIRMADO' THEN
        RAISE EXCEPTION 'El pedido % está %: la venta solo se genera desde un pedido confirmado.', NEW.id_pedido, v_estado_pedido;
    END IF;
    IF EXISTS (SELECT 1 FROM venta WHERE id_pedido = NEW.id_pedido) THEN
        RAISE EXCEPTION 'El pedido % ya tiene una venta registrada.', NEW.id_pedido;
    END IF;

    FOR v_linea IN
        SELECT d.cantidad_aves, l.id_lote, l.codigo, l.estado
        FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote
        WHERE d.id_pedido = NEW.id_pedido
        FOR UPDATE OF l
    LOOP
        IF v_linea.estado <> 'LISTO_PARA_VENTA' THEN
            RAISE EXCEPTION 'El lote % está %: solo se vende un lote listo para venta.', v_linea.codigo, v_linea.estado;
        END IF;
        IF v_linea.cantidad_aves > fn_aves_disponibles(v_linea.id_lote) THEN
            RAISE EXCEPTION 'El lote % tiene % aves disponibles: no alcanza para las % del pedido.',
                v_linea.codigo, fn_aves_disponibles(v_linea.id_lote), v_linea.cantidad_aves;
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_venta_validar
    BEFORE INSERT OR UPDATE ON venta
    FOR EACH ROW EXECUTE FUNCTION fn_trg_venta_validar();

-- S12: cada lote del pedido que se queda sin aves pasa a vendido (y su
-- galpón queda disponible por el trigger del módulo 1).
CREATE FUNCTION fn_trg_venta_lotes()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE lote l SET estado = 'VENDIDO'
    FROM detalle_pedido d
    WHERE d.id_pedido = NEW.id_pedido AND l.id_lote = d.id_lote
      AND l.estado = 'LISTO_PARA_VENTA' AND fn_aves_disponibles(l.id_lote) = 0;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_venta_lotes
    AFTER INSERT ON venta
    FOR EACH ROW EXECUTE FUNCTION fn_trg_venta_lotes();

-- Pago: no supera el saldo (S37); el pago en línea exige la referencia de la
-- pasarela y los demás no la llevan. Los pagos no se modifican.
CREATE FUNCTION fn_trg_pago_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_saldo   NUMERIC;
    v_metodo  TEXT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Un pago registrado no se modifica.';
    END IF;
    PERFORM 1 FROM venta WHERE id_venta = NEW.id_venta FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La venta indicada no existe.';
    END IF;
    SELECT codigo INTO v_metodo FROM metodo_pago WHERE id_metodo_pago = NEW.id_metodo_pago AND activo;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El método de pago indicado no existe o está inactivo.';
    END IF;
    IF v_metodo = 'EN_LINEA' AND NEW.referencia_pasarela IS NULL THEN
        RAISE EXCEPTION 'Un pago en línea debe tener la referencia de la transacción aprobada.';
    END IF;
    IF v_metodo <> 'EN_LINEA' AND NEW.referencia_pasarela IS NOT NULL THEN
        RAISE EXCEPTION 'Solo los pagos en línea llevan referencia de la pasarela.';
    END IF;
    IF NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha del pago no puede ser futura.';
    END IF;
    v_saldo := fn_saldo_venta(NEW.id_venta);
    IF v_saldo <= 0 THEN
        RAISE EXCEPTION 'La venta % ya está pagada.', NEW.id_venta;
    END IF;
    IF NEW.monto > v_saldo THEN
        RAISE EXCEPTION 'El pago (%) supera el saldo pendiente de la venta % (%).', NEW.monto, NEW.id_venta, v_saldo;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pago_validar
    BEFORE INSERT OR UPDATE ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_trg_pago_validar();

-- Con el saldo en cero la venta queda pagada, aunque estuviera vencida.
CREATE FUNCTION fn_trg_pago_venta()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF fn_saldo_venta(NEW.id_venta) = 0 THEN
        UPDATE venta SET estado = 'PAGADA' WHERE id_venta = NEW.id_venta;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_pago_venta
    AFTER INSERT ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_trg_pago_venta();

-- Entrega: transportista activo (S26), una por venta (S39) y solo se marca
-- entregada si la venta existe y su pedido está confirmado (S41).
CREATE FUNCTION fn_trg_entrega_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_estado_pedido estado_pedido;
    v_fecha_venta   DATE;
BEGIN
    IF TG_OP = 'INSERT' OR NEW.id_empleado <> OLD.id_empleado THEN
        PERFORM fn_validar_empleado(NEW.id_empleado, 'TRANSPORTISTA');
    END IF;
    SELECT p.estado, v.fecha_venta INTO v_estado_pedido, v_fecha_venta
    FROM venta v JOIN pedido p ON p.id_pedido = v.id_pedido
    WHERE v.id_venta = NEW.id_venta;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La venta indicada no existe.';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'PENDIENTE' THEN
            RAISE EXCEPTION 'Una entrega nueva debe iniciar pendiente.';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.id_venta <> OLD.id_venta THEN
        RAISE EXCEPTION 'La venta de una entrega no se puede cambiar.';
    END IF;
    IF OLD.estado <> 'PENDIENTE' AND (NEW.estado <> OLD.estado OR NEW.id_empleado <> OLD.id_empleado) THEN
        RAISE EXCEPTION 'La entrega ya está %: no se puede modificar.', OLD.estado;
    END IF;
    IF NEW.estado = 'ENTREGADO' AND OLD.estado = 'PENDIENTE' THEN
        IF v_estado_pedido <> 'CONFIRMADO' THEN
            RAISE EXCEPTION 'El pedido de la venta está %: solo se entrega una venta de un pedido confirmado.', v_estado_pedido;
        END IF;
        IF NEW.fecha_entrega < v_fecha_venta OR NEW.fecha_entrega > CURRENT_DATE THEN
            RAISE EXCEPTION 'La fecha de entrega debe estar entre la venta (%) y hoy.', v_fecha_venta;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_entrega_validar
    BEFORE INSERT OR UPDATE ON entrega
    FOR EACH ROW EXECUTE FUNCTION fn_trg_entrega_validar();

-- Al entregar, el pedido queda entregado.
CREATE FUNCTION fn_trg_entrega_pedido()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = 'ENTREGADO' AND OLD.estado <> 'ENTREGADO' THEN
        UPDATE pedido p SET estado = 'ENTREGADO'
        FROM venta v WHERE v.id_venta = NEW.id_venta AND p.id_pedido = v.id_pedido;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_entrega_pedido
    AFTER UPDATE OF estado ON entrega
    FOR EACH ROW EXECUTE FUNCTION fn_trg_entrega_pedido();

-- Otro ingreso: lote existente, no cerrado y fecha dentro de su ciclo.
CREATE FUNCTION fn_trg_otro_ingreso_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_lote lote%ROWTYPE;
BEGIN
    SELECT * INTO v_lote FROM lote WHERE id_lote = NEW.id_lote;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El lote indicado no existe.';
    END IF;
    IF v_lote.estado = 'CERRADO' THEN
        RAISE EXCEPTION 'El lote % está cerrado: no admite más ingresos.', v_lote.codigo;
    END IF;
    IF NEW.fecha < v_lote.fecha_ingreso OR NEW.fecha > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha del ingreso debe estar entre el ingreso del lote (%) y hoy.', v_lote.fecha_ingreso;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_otro_ingreso_validar
    BEFORE INSERT OR UPDATE ON otro_ingreso
    FOR EACH ROW EXECUTE FUNCTION fn_trg_otro_ingreso_validar();

-- ---------------------------------------------------------------------
-- Procedimientos
-- ---------------------------------------------------------------------

CREATE PROCEDURE sp_registrar_otro_ingreso(
    p_id_lote          BIGINT,
    p_fecha            DATE,
    p_monto            NUMERIC,
    p_descripcion      VARCHAR,
    OUT p_id_otro_ingreso BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO otro_ingreso (id_lote, fecha, monto, descripcion)
        VALUES (p_id_lote, COALESCE(p_fecha, CURRENT_DATE), p_monto, TRIM(p_descripcion))
        RETURNING id_otro_ingreso INTO p_id_otro_ingreso;
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

-- Crea el pedido con sus lotes. p_detalle: [{"idLote": 3, "cantidadAves": 200}, ...].
CREATE PROCEDURE sp_crear_pedido(
    p_id_cliente              BIGINT,
    p_fecha_estimada_entrega  DATE,
    p_observaciones           VARCHAR,
    p_detalle                 JSONB,
    OUT p_id_pedido           BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        IF p_detalle IS NULL OR jsonb_typeof(p_detalle) <> 'array' OR jsonb_array_length(p_detalle) = 0 THEN
            RAISE EXCEPTION 'El pedido debe incluir aves de al menos un lote.';
        END IF;

        INSERT INTO pedido (id_cliente, fecha_estimada_entrega, observaciones)
        VALUES (p_id_cliente, p_fecha_estimada_entrega, NULLIF(TRIM(p_observaciones), ''))
        RETURNING id_pedido INTO p_id_pedido;

        INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves)
        SELECT p_id_pedido, d."idLote", d."cantidadAves"
        FROM jsonb_to_recordset(p_detalle) AS d ("idLote" BIGINT, "cantidadAves" INTEGER);
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

-- Confirma el pedido apartando sus aves. Vuelve a validar cada lote con las
-- aves libres en este momento, bloqueando los lotes para evitar que dos
-- confirmaciones simultáneas aparten las mismas aves.
CREATE PROCEDURE sp_confirmar_pedido(p_id_pedido BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error  TEXT;
    v_linea  RECORD;
    v_libres INTEGER;
BEGIN
    BEGIN
        FOR v_linea IN
            SELECT d.cantidad_aves, l.id_lote, l.codigo, l.estado
            FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote
            WHERE d.id_pedido = p_id_pedido
            FOR UPDATE OF l
        LOOP
            IF v_linea.estado <> 'LISTO_PARA_VENTA' THEN
                RAISE EXCEPTION 'El lote % está %: ya no está listo para venta.', v_linea.codigo, v_linea.estado;
            END IF;
            v_libres := fn_aves_disponibles(v_linea.id_lote) - fn_aves_comprometidas(v_linea.id_lote, p_id_pedido);
            IF v_linea.cantidad_aves > v_libres THEN
                RAISE EXCEPTION 'El lote % tiene % aves libres: no alcanza para las % del pedido.',
                    v_linea.codigo, v_libres, v_linea.cantidad_aves;
            END IF;
        END LOOP;

        UPDATE pedido SET estado = 'CONFIRMADO' WHERE id_pedido = p_id_pedido;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe el pedido con id %.', p_id_pedido;
        END IF;
    EXCEPTION
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

CREATE PROCEDURE sp_cancelar_pedido(p_id_pedido BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error TEXT;
BEGIN
    BEGIN
        UPDATE pedido SET estado = 'CANCELADO' WHERE id_pedido = p_id_pedido;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe el pedido con id %.', p_id_pedido;
        END IF;
    EXCEPTION
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

CREATE PROCEDURE sp_registrar_venta(
    p_id_pedido          BIGINT,
    p_fecha_venta        DATE,
    p_peso_total_libras  NUMERIC,
    p_precio_libra       NUMERIC,
    p_fecha_limite_pago  DATE,
    OUT p_id_venta       BIGINT,
    OUT p_valor_total    NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO venta (id_pedido, fecha_venta, peso_total_libras, precio_libra, fecha_limite_pago)
        VALUES (p_id_pedido, COALESCE(p_fecha_venta, CURRENT_DATE), p_peso_total_libras, p_precio_libra,
                COALESCE(p_fecha_limite_pago, COALESCE(p_fecha_venta, CURRENT_DATE) + 30))
        RETURNING id_venta, valor_total INTO p_id_venta, p_valor_total;
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

-- Registra un pago. Para el pago en línea, el backend lo llama solo cuando la
-- pasarela confirma la transacción como aprobada.
CREATE PROCEDURE sp_registrar_pago(
    p_id_venta             BIGINT,
    p_codigo_metodo        VARCHAR,
    p_fecha                DATE,
    p_monto                NUMERIC,
    p_referencia_pasarela  VARCHAR,
    OUT p_id_pago          BIGINT,
    OUT p_saldo            NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
    v_id_metodo    BIGINT;
BEGIN
    BEGIN
        SELECT id_metodo_pago INTO v_id_metodo FROM metodo_pago WHERE codigo = p_codigo_metodo;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'El método de pago "%" no existe.', p_codigo_metodo;
        END IF;
        INSERT INTO pago (id_venta, id_metodo_pago, fecha, monto, referencia_pasarela)
        VALUES (p_id_venta, v_id_metodo, COALESCE(p_fecha, CURRENT_DATE), p_monto, NULLIF(TRIM(p_referencia_pasarela), ''))
        RETURNING id_pago INTO p_id_pago;
        p_saldo := fn_saldo_venta(p_id_venta);
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

CREATE PROCEDURE sp_registrar_entrega(
    p_id_venta       BIGINT,
    p_id_empleado    BIGINT,
    p_direccion      VARCHAR,
    p_observaciones  VARCHAR,
    OUT p_id_entrega BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
    v_direccion    TEXT;
BEGIN
    BEGIN
        v_direccion := NULLIF(TRIM(p_direccion), '');
        IF v_direccion IS NULL THEN
            SELECT c.direccion INTO v_direccion
            FROM venta v JOIN pedido p ON p.id_pedido = v.id_pedido JOIN cliente c ON c.id_cliente = p.id_cliente
            WHERE v.id_venta = p_id_venta;
        END IF;
        IF v_direccion IS NULL THEN
            RAISE EXCEPTION 'Indique la dirección de entrega: el cliente no tiene una registrada.';
        END IF;
        INSERT INTO entrega (id_venta, id_empleado, direccion, observaciones)
        VALUES (p_id_venta, p_id_empleado, v_direccion, NULLIF(TRIM(p_observaciones), ''))
        RETURNING id_entrega INTO p_id_entrega;
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

-- Marca la entrega como entregada o devuelta.
CREATE PROCEDURE sp_actualizar_entrega(
    p_id_entrega     BIGINT,
    p_estado         VARCHAR,
    p_fecha_entrega  DATE,
    p_observaciones  VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
    v_estado       estado_entrega;
BEGIN
    BEGIN
        v_estado := p_estado::estado_entrega;
        UPDATE entrega
           SET estado = v_estado,
               fecha_entrega = CASE WHEN v_estado = 'ENTREGADO' THEN COALESCE(p_fecha_entrega, CURRENT_DATE) ELSE fecha_entrega END,
               observaciones = COALESCE(NULLIF(TRIM(p_observaciones), ''), observaciones)
         WHERE id_entrega = p_id_entrega;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe la entrega con id %.', p_id_entrega;
        END IF;
    EXCEPTION
        WHEN invalid_text_representation THEN
            v_error := format('El estado de entrega "%s" no es válido.', p_estado);
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

-- S38: marca como vencidas las ventas con saldo después de su fecha límite.
-- Es un procedimiento y no un trigger porque depende del paso del tiempo; el
-- backend lo ejecuta cada día.
CREATE PROCEDURE sp_marcar_cartera_vencida(OUT p_marcadas INTEGER)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE venta v SET estado = 'VENCIDA'
    WHERE v.estado = 'PENDIENTE_PAGO'
      AND v.fecha_limite_pago < CURRENT_DATE
      AND fn_saldo_venta(v.id_venta) > 0;
    GET DIAGNOSTICS p_marcadas = ROW_COUNT;
    COMMIT;
END;
$$;

-- ---------------------------------------------------------------------
-- Vistas
-- ---------------------------------------------------------------------

CREATE VIEW v_cliente AS
SELECT c.id_cliente, c.id_tipo_cliente, t.nombre AS tipo_cliente, c.documento, c.nombre, c.direccion,
       c.telefono, c.correo, c.activo
FROM cliente c JOIN tipo_cliente t ON t.id_tipo_cliente = c.id_tipo_cliente;

CREATE VIEW v_pedido AS
SELECT p.id_pedido, p.id_cliente, c.nombre AS cliente, p.fecha_pedido, p.fecha_estimada_entrega,
       p.estado::text AS estado, p.observaciones,
       (SELECT SUM(d.cantidad_aves) FROM detalle_pedido d WHERE d.id_pedido = p.id_pedido) AS total_aves,
       (SELECT string_agg(l.codigo || ' (' || d.cantidad_aves || ')', ', ' ORDER BY l.codigo)
          FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote WHERE d.id_pedido = p.id_pedido) AS lotes,
       v.id_venta
FROM pedido p
JOIN cliente c ON c.id_cliente = p.id_cliente
LEFT JOIN venta v ON v.id_pedido = p.id_pedido;

CREATE VIEW v_detalle_pedido AS
SELECT d.id_pedido, d.id_lote, l.codigo AS codigo_lote, d.cantidad_aves
FROM detalle_pedido d JOIN lote l ON l.id_lote = d.id_lote;

CREATE VIEW v_venta AS
SELECT v.id_venta, v.id_pedido, p.id_cliente, c.nombre AS cliente, v.fecha_venta,
       (SELECT SUM(d.cantidad_aves) FROM detalle_pedido d WHERE d.id_pedido = v.id_pedido) AS cantidad_aves,
       v.peso_total_libras, v.precio_libra, v.valor_total,
       v.valor_total - fn_saldo_venta(v.id_venta) AS pagado,
       fn_saldo_venta(v.id_venta) AS saldo,
       v.fecha_limite_pago, v.estado::text AS estado,
       (v.fecha_limite_pago < CURRENT_DATE AND fn_saldo_venta(v.id_venta) > 0) AS vencida,
       e.id_entrega, e.estado::text AS estado_entrega
FROM venta v
JOIN pedido p ON p.id_pedido = v.id_pedido
JOIN cliente c ON c.id_cliente = p.id_cliente
LEFT JOIN entrega e ON e.id_venta = v.id_venta;

CREATE VIEW v_pago AS
SELECT pg.id_pago, pg.id_venta, pg.fecha, pg.monto, m.codigo AS codigo_metodo, m.nombre AS metodo,
       pg.referencia_pasarela
FROM pago pg JOIN metodo_pago m ON m.id_metodo_pago = pg.id_metodo_pago;

CREATE VIEW v_entrega AS
SELECT e.id_entrega, e.id_venta, c.nombre AS cliente, e.id_empleado,
       em.nombres || ' ' || em.apellidos AS transportista, e.direccion, e.fecha_entrega,
       e.estado::text AS estado, e.observaciones
FROM entrega e
JOIN venta v ON v.id_venta = e.id_venta
JOIN pedido p ON p.id_pedido = v.id_pedido
JOIN cliente c ON c.id_cliente = p.id_cliente
JOIN empleado em ON em.id_empleado = e.id_empleado;

CREATE VIEW v_ventas_por_cliente AS
SELECT c.id_cliente, c.nombre AS cliente, t.nombre AS tipo_cliente,
       COUNT(v.id_venta)                          AS ventas,
       COALESCE(SUM(v.valor_total), 0)            AS valor_vendido,
       COALESCE(SUM(fn_saldo_venta(v.id_venta)), 0) AS saldo_pendiente
FROM cliente c
JOIN tipo_cliente t ON t.id_tipo_cliente = c.id_tipo_cliente
LEFT JOIN pedido p ON p.id_cliente = c.id_cliente
LEFT JOIN venta v ON v.id_pedido = p.id_pedido
GROUP BY c.id_cliente, t.nombre;

CREATE VIEW v_otro_ingreso AS
SELECT o.id_otro_ingreso, o.fecha, o.id_lote, l.codigo AS codigo_lote, o.monto, o.descripcion
FROM otro_ingreso o JOIN lote l ON l.id_lote = o.id_lote;

-- Rentabilidad: costo del lote (módulo 5) frente a sus ingresos (ventas en
-- proporción a sus aves, más otros ingresos). Ganancia neta = ingresos - costos.
CREATE VIEW v_rentabilidad_lote AS
SELECT c.id_lote, c.codigo, c.estado, c.costo_pollitos, c.costo_alimento, c.costo_sanidad,
       c.costo_beneficio, c.otros_gastos, c.costo_total,
       fn_aves_vendidas(c.id_lote)                                          AS aves_vendidas,
       i.libras                                                             AS libras_vendidas,
       CASE WHEN fn_aves_vendidas(c.id_lote) > 0
            THEN ROUND(i.libras / fn_aves_vendidas(c.id_lote), 2) END       AS peso_promedio_libras,
       i.ingreso                                                            AS ingreso_ventas,
       o.otros                                                              AS otros_ingresos,
       i.ingreso + o.otros                                                  AS ingreso_total,
       i.ingreso + o.otros - c.costo_total                                  AS ganancia,
       CASE WHEN fn_aves_vendidas(c.id_lote) > 0
            THEN ROUND(c.costo_total / fn_aves_vendidas(c.id_lote), 2) END  AS costo_por_ave_vendida,
       CASE WHEN i.libras > 0 THEN ROUND(c.costo_total / i.libras, 2) END   AS costo_por_libra,
       CASE WHEN i.libras > 0 THEN ROUND(i.ingreso / i.libras, 2) END       AS precio_promedio_libra,
       CASE WHEN i.ingreso + o.otros > 0
            THEN ROUND((i.ingreso + o.otros - c.costo_total) * 100 / (i.ingreso + o.otros), 2) END AS margen_porcentaje
FROM v_costo_lote c
CROSS JOIN LATERAL fn_ingreso_lote(c.id_lote) i
CROSS JOIN LATERAL (SELECT COALESCE(SUM(monto), 0) AS otros FROM otro_ingreso WHERE id_lote = c.id_lote) o;

-- ---------------------------------------------------------------------
-- Privilegios: ventas y pagos no se borran; los pedidos se cancelan.
-- ---------------------------------------------------------------------

REVOKE ALL ON tipo_cliente, metodo_pago, cliente, pedido, detalle_pedido, venta, pago, entrega, otro_ingreso,
              v_otro_ingreso, v_cliente, v_pedido, v_detalle_pedido, v_venta, v_pago, v_entrega,
              v_ventas_por_cliente, v_rentabilidad_lote FROM pollos_app;
GRANT SELECT ON tipo_cliente, metodo_pago, v_cliente, v_pedido, v_detalle_pedido, v_venta, v_pago,
                v_entrega, v_ventas_por_cliente, v_rentabilidad_lote, v_otro_ingreso TO pollos_app;
GRANT SELECT, INSERT, UPDATE ON cliente, pedido, venta, entrega TO pollos_app;
GRANT SELECT, INSERT ON detalle_pedido, pago, otro_ingreso TO pollos_app;
GRANT EXECUTE ON FUNCTION fn_cartera_vencida TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_crear_pedido, sp_confirmar_pedido, sp_cancelar_pedido, sp_registrar_venta,
                           sp_registrar_pago, sp_registrar_entrega, sp_actualizar_entrega,
                           sp_marcar_cartera_vencida, sp_registrar_otro_ingreso TO pollos_app;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('cliente.gestionar',  'Registrar y modificar clientes'),
    ('pedido.gestionar',   'Crear, confirmar y cancelar pedidos'),
    ('venta.gestionar',    'Registrar ventas, entregas y otros ingresos de los lotes'),
    ('pago.registrar',     'Registrar pagos de las ventas, incluido el pago en línea'),
    ('cartera.ver',        'Consultar ventas, saldos y cartera vencida')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
