package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Resultado de confirmar un pago en línea.
 *
 * @param idTransaccion     transacción de la pasarela
 * @param estadoTransaccion estado según la pasarela
 * @param registrado        {@code true} si el pago quedó registrado en la venta
 * @param idVenta           venta pagada, o {@code null} si no se identificó
 * @param monto             monto de la transacción
 * @param saldo             saldo de la venta después del pago, o {@code null}
 * @param mensaje           explicación para el usuario
 */
public record ResultadoPagoEnLineaDTO(String idTransaccion, String estadoTransaccion, boolean registrado, Long idVenta,
		BigDecimal monto, BigDecimal saldo, String mensaje) {
}
