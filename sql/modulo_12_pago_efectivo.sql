-- =====================================================================
-- MÓDULO 12: pago en efectivo de una venta ya registrada.
-- Reemplaza D27 (solo pago en línea): además de Wompi, el personal con el
-- permiso pago.registrar puede registrar que el cliente pagó el saldo en
-- efectivo. El pago en línea sigue igual, con la referencia de Wompi.
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

-- Medios de pago aceptados: en línea y efectivo.
UPDATE metodo_pago SET activo = (codigo IN ('EN_LINEA', 'EFECTIVO'));

-- Igual a la del módulo 9, pero acepta EFECTIVO (sin referencia de la pasarela).
CREATE OR REPLACE FUNCTION fn_trg_pago_validar()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    v_saldo    NUMERIC;
    vv_metodo  TEXT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Un pago registrado no se modifica.';
    END IF;
    PERFORM 1 FROM venta WHERE id_venta = NEW.id_venta FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La venta indicada no existe.';
    END IF;
    SELECT codigo INTO vv_metodo FROM metodo_pago WHERE id_metodo_pago = NEW.id_metodo_pago AND activo;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Los pagos se reciben en línea con Wompi o en efectivo.';
    END IF;
    IF vv_metodo = 'EN_LINEA' AND NEW.referencia_pasarela IS NULL THEN
        RAISE EXCEPTION 'Un pago en línea debe tener la referencia de la transacción aprobada.';
    END IF;
    IF vv_metodo = 'EFECTIVO' AND NEW.referencia_pasarela IS NOT NULL THEN
        RAISE EXCEPTION 'Un pago en efectivo no lleva referencia de la pasarela.';
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

UPDATE permiso SET descripcion = 'Registrar pagos en efectivo y consultar los pagos de las ventas'
WHERE codigo = 'pago.registrar';

RESET ROLE;

COMMIT;
