-- =====================================================================
-- MÓDULO 8: PORTAL DE CLIENTES (cuenta propia, pedidos y pagos)
-- Modelo: docs/modelo_relacional.md, sección 1.5 (D21-D26).
--   ./instalar_bd.sh modulo_08_portal_clientes.sql
--
-- Reglas cubiertas:
--   D21 cliente con cuenta: usuario 1:1 opcional        -> cliente.id_usuario, uq_cliente_usuario
--   D22 reparto del pedido por antigüedad (FIFO)         -> fn_repartir_pedido_fifo, sp_crear_pedido_cliente
--   D23 los pedidos pendientes también apartan aves      -> fn_aves_comprometidas (reemplaza la del módulo 6)
--   Sin existencias el pedido se rechaza de inmediato    -> fn_repartir_pedido_fifo
--   D24 rol Cliente con el único permiso portal.cliente  -> rol, permiso, rol_permiso
--
-- Se instala después de modulo_07_produccion_escalonada.sql.
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- D21: cuenta del cliente
-- ---------------------------------------------------------------------

ALTER TABLE cliente ADD COLUMN id_usuario BIGINT;
ALTER TABLE cliente ADD CONSTRAINT uq_cliente_usuario UNIQUE (id_usuario);
ALTER TABLE cliente ADD CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);

INSERT INTO mensaje_restriccion (restriccion, mensaje) VALUES
    ('uq_cliente_usuario', 'Ese usuario ya está vinculado a otro cliente.'),
    ('fk_cliente_usuario', 'El usuario indicado no existe.');

-- La vista agrega al final la cuenta del cliente (las columnas anteriores no cambian).
CREATE OR REPLACE VIEW v_cliente AS
SELECT c.id_cliente, c.id_tipo_cliente, t.nombre AS tipo_cliente, c.documento, c.nombre, c.direccion,
       c.telefono, c.correo, c.activo, c.id_usuario, u.nombre_usuario AS usuario
FROM cliente c
JOIN tipo_cliente t ON t.id_tipo_cliente = c.id_tipo_cliente
LEFT JOIN usuario u ON u.id_usuario = c.id_usuario;

-- ---------------------------------------------------------------------
-- D23: aves apartadas
-- ---------------------------------------------------------------------

-- Aves apartadas en pedidos pendientes o confirmados que aún no se venden. Se
-- puede excluir un pedido (el que se está confirmando o modificando).
CREATE OR REPLACE FUNCTION fn_aves_comprometidas(p_id_lote BIGINT, p_excluir_pedido BIGINT DEFAULT NULL)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(d.cantidad_aves), 0)::INTEGER
    FROM detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
    WHERE d.id_lote = p_id_lote
      AND p.estado IN ('PENDIENTE', 'CONFIRMADO')
      AND p.id_pedido IS DISTINCT FROM p_excluir_pedido
      AND NOT EXISTS (SELECT 1 FROM venta v WHERE v.id_pedido = p.id_pedido);
$$;

-- Aves de un lote que todavía se pueden pedir.
CREATE FUNCTION fn_aves_libres_lote(p_id_lote BIGINT)
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT GREATEST(fn_aves_disponibles(p_id_lote) - fn_aves_comprometidas(p_id_lote), 0);
$$;

-- Total de aves que un cliente puede pedir: las libres de los lotes listos para
-- venta. El portal muestra solo este número, nunca los lotes.
CREATE FUNCTION fn_aves_libres_venta()
RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(SUM(fn_aves_libres_lote(id_lote)), 0)::INTEGER
    FROM lote
    WHERE estado = 'LISTO_PARA_VENTA';
$$;

-- El detalle de un pedido que se modifica no cuenta sus propias aves como apartadas.
CREATE OR REPLACE FUNCTION fn_trg_detalle_pedido_validar()
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
    v_libres := fn_aves_disponibles(NEW.id_lote)
              - fn_aves_comprometidas(NEW.id_lote, CASE WHEN TG_OP = 'UPDATE' THEN NEW.id_pedido END);
    IF NEW.cantidad_aves > v_libres THEN
        RAISE EXCEPTION 'El lote % tiene % aves libres para pedidos: no alcanza para %.', v_lote.codigo, v_libres, NEW.cantidad_aves;
    END IF;
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------
-- D22: pedido del cliente repartido por antigüedad
-- ---------------------------------------------------------------------

-- Reparte un pedido pendiente por antigüedad: toma las aves libres del lote
-- listo para venta que más tiempo lleva disponible (fecha estimada de venta más
-- antigua) y, si no alcanza, sigue con el siguiente. Si entre todos no alcanza,
-- lanza el error sin repartir nada. Bloquea los lotes mientras reparte, para que
-- dos pedidos simultáneos no tomen las mismas aves. Devuelve cuántos lotes usó.
-- Es una función (no hace COMMIT) para que el procedimiento y las pruebas la
-- usen dentro de su transacción.
CREATE FUNCTION fn_repartir_pedido_fifo(p_id_pedido BIGINT, p_cantidad_aves INTEGER)
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    v_libres    INTEGER;
    v_restante  INTEGER := p_cantidad_aves;
    v_toma      INTEGER;
    v_lotes     INTEGER := 0;
    v_lote      RECORD;
BEGIN
    IF p_cantidad_aves IS NULL OR p_cantidad_aves <= 0 THEN
        RAISE EXCEPTION 'La cantidad de aves debe ser mayor que cero.';
    END IF;

    PERFORM 1 FROM lote WHERE estado = 'LISTO_PARA_VENTA' ORDER BY id_lote FOR UPDATE;

    v_libres := fn_aves_libres_venta();
    IF v_libres = 0 THEN
        RAISE EXCEPTION 'En este momento no hay aves disponibles para la venta.';
    END IF;
    IF p_cantidad_aves > v_libres THEN
        RAISE EXCEPTION 'Solo hay % aves disponibles: no alcanza para las % del pedido.', v_libres, p_cantidad_aves;
    END IF;

    FOR v_lote IN
        SELECT id_lote, fn_aves_libres_lote(id_lote) AS libres
        FROM lote
        WHERE estado = 'LISTO_PARA_VENTA'
        ORDER BY fecha_estimada_venta, id_lote
    LOOP
        EXIT WHEN v_restante = 0;
        CONTINUE WHEN v_lote.libres = 0;
        v_toma := LEAST(v_restante, v_lote.libres);
        INSERT INTO detalle_pedido (id_pedido, id_lote, cantidad_aves)
        VALUES (p_id_pedido, v_lote.id_lote, v_toma);
        v_restante := v_restante - v_toma;
        v_lotes := v_lotes + 1;
    END LOOP;
    RETURN v_lotes;
END;
$$;

-- El cliente pide una cantidad de aves; el sistema elige los lotes.
CREATE PROCEDURE sp_crear_pedido_cliente(
    p_id_cliente              BIGINT,
    p_cantidad_aves           INTEGER,
    p_fecha_estimada_entrega  DATE,
    p_observaciones           VARCHAR,
    OUT p_id_pedido           BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO pedido (id_cliente, fecha_estimada_entrega, observaciones)
        VALUES (p_id_cliente, p_fecha_estimada_entrega, NULLIF(TRIM(p_observaciones), ''))
        RETURNING id_pedido INTO p_id_pedido;
        PERFORM fn_repartir_pedido_fifo(p_id_pedido, p_cantidad_aves);
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
-- Privilegios
-- ---------------------------------------------------------------------

REVOKE ALL ON v_cliente FROM pollos_app;
GRANT SELECT ON v_cliente TO pollos_app;
GRANT EXECUTE ON FUNCTION fn_aves_libres_lote, fn_aves_libres_venta, fn_repartir_pedido_fifo TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_crear_pedido_cliente TO pollos_app;

-- ---------------------------------------------------------------------
-- D24: rol Cliente con un único permiso
-- ---------------------------------------------------------------------

INSERT INTO permiso (codigo, descripcion) VALUES
    ('portal.cliente', 'Hacer pedidos y pagarlos desde el portal de clientes')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol (nombre, descripcion) VALUES
    ('Cliente', 'Clientes con cuenta propia: solo hacen y pagan sus pedidos en el portal')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r, permiso p
WHERE r.nombre = 'Cliente' AND p.codigo = 'portal.cliente'
ON CONFLICT DO NOTHING;

RESET ROLE;

COMMIT;
