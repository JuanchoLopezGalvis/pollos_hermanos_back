-- =====================================================================
-- MÓDULO 9: SOLO PAGO EN LÍNEA Y ENTREGA EN LA DIRECCIÓN DEL CLIENTE
-- Modelo: docs/modelo_relacional.md, sección 1.6 (D27-D29).
--   ./instalar_bd.sh modulo_09_pago_en_linea_y_entrega.sql
--
-- Reglas cubiertas:
--   D27 el único medio de pago es Wompi (EN_LINEA)      -> metodo_pago.activo, fn_trg_pago_validar
--       los pagos ya registrados con otros medios se conservan como historial
--   D28 la entrega va a la dirección del cliente        -> fn_trg_entrega_direccion, sp_registrar_entrega
--   D29 la venta muestra la dirección del cliente        -> v_venta.direccion_cliente
--
-- Se instala después de modulo_08_portal_clientes.sql.
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- ---------------------------------------------------------------------
-- D27: solo pagos en línea
-- ---------------------------------------------------------------------

UPDATE metodo_pago SET activo = FALSE WHERE codigo <> 'EN_LINEA';

-- Igual a la del módulo 6, pero rechaza cualquier medio distinto de EN_LINEA.
CREATE OR REPLACE FUNCTION fn_trg_pago_validar()
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
    SELECT codigo INTO v_metodo FROM metodo_pago WHERE id_metodo_pago = NEW.id_metodo_pago;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El método de pago indicado no existe.';
    END IF;
    IF v_metodo <> 'EN_LINEA' THEN
        RAISE EXCEPTION 'Los pagos solo se reciben en línea con Wompi: el cliente paga desde su portal.';
    END IF;
    IF NEW.referencia_pasarela IS NULL THEN
        RAISE EXCEPTION 'Un pago en línea debe tener la referencia de la transacción aprobada.';
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

UPDATE permiso SET descripcion = 'Consultar los pagos de las ventas (solo el cliente paga, con Wompi)'
WHERE codigo = 'pago.registrar';

-- ---------------------------------------------------------------------
-- D28: la entrega va a la dirección del cliente
-- ---------------------------------------------------------------------

-- Copia la dirección del cliente en la entrega (queda como constancia de a
-- dónde se llevó) y no deja cambiarla después. Corre antes que
-- trg_entrega_validar porque los triggers BEFORE van en orden alfabético.
CREATE FUNCTION fn_trg_entrega_direccion()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_cliente    VARCHAR;
    v_direccion  VARCHAR;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.direccion IS DISTINCT FROM OLD.direccion THEN
            RAISE EXCEPTION 'La dirección de una entrega no se modifica: es la que registró el cliente.';
        END IF;
        RETURN NEW;
    END IF;

    SELECT c.nombre, NULLIF(TRIM(c.direccion), '') INTO v_cliente, v_direccion
    FROM venta v
    JOIN pedido p ON p.id_pedido = v.id_pedido
    JOIN cliente c ON c.id_cliente = p.id_cliente
    WHERE v.id_venta = NEW.id_venta;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La venta indicada no existe.';
    END IF;
    IF v_direccion IS NULL THEN
        RAISE EXCEPTION 'El cliente % no tiene dirección registrada: agrégala en Clientes antes de programar la entrega.', v_cliente;
    END IF;
    NEW.direccion := v_direccion;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_entrega_direccion
    BEFORE INSERT OR UPDATE ON entrega
    FOR EACH ROW EXECUTE FUNCTION fn_trg_entrega_direccion();

-- Ya no recibe dirección: la pone el trigger.
DROP PROCEDURE sp_registrar_entrega(BIGINT, BIGINT, VARCHAR, VARCHAR);

CREATE PROCEDURE sp_registrar_entrega(
    p_id_venta       BIGINT,
    p_id_empleado    BIGINT,
    p_observaciones  VARCHAR,
    OUT p_id_entrega BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_error        TEXT;
    v_restriccion  TEXT;
BEGIN
    BEGIN
        INSERT INTO entrega (id_venta, id_empleado, direccion, observaciones)
        VALUES (p_id_venta, p_id_empleado, NULL, NULLIF(TRIM(p_observaciones), ''))
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

-- ---------------------------------------------------------------------
-- D29: la venta muestra la dirección del cliente (columna agregada al final)
-- ---------------------------------------------------------------------

CREATE OR REPLACE VIEW v_venta AS
SELECT v.id_venta, v.id_pedido, p.id_cliente, c.nombre AS cliente, v.fecha_venta,
       (SELECT SUM(d.cantidad_aves) FROM detalle_pedido d WHERE d.id_pedido = v.id_pedido) AS cantidad_aves,
       v.peso_total_libras, v.precio_libra, v.valor_total,
       v.valor_total - fn_saldo_venta(v.id_venta) AS pagado,
       fn_saldo_venta(v.id_venta) AS saldo,
       v.fecha_limite_pago, v.estado::text AS estado,
       (v.fecha_limite_pago < CURRENT_DATE AND fn_saldo_venta(v.id_venta) > 0) AS vencida,
       e.id_entrega, e.estado::text AS estado_entrega,
       c.direccion AS direccion_cliente
FROM venta v
JOIN pedido p ON p.id_pedido = v.id_pedido
JOIN cliente c ON c.id_cliente = p.id_cliente
LEFT JOIN entrega e ON e.id_venta = v.id_venta;

-- ---------------------------------------------------------------------
-- Privilegios
-- ---------------------------------------------------------------------

REVOKE ALL ON v_venta FROM pollos_app;
GRANT SELECT ON v_venta TO pollos_app;
GRANT EXECUTE ON PROCEDURE sp_registrar_entrega TO pollos_app;

RESET ROLE;

COMMIT;
