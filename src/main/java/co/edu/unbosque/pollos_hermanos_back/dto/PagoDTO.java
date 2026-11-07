package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago de una venta, leído de {@code v_pago}.
 *
 * @param idPago             identificador
 * @param idVenta            venta
 * @param fecha              fecha del pago
 * @param monto              valor pagado
 * @param codigoMetodo       código del método de pago
 * @param metodo             nombre del método
 * @param referenciaPasarela transacción aprobada de la pasarela, solo en pagos en línea
 */
public record PagoDTO(Long idPago, Long idVenta, LocalDate fecha, BigDecimal monto, String codigoMetodo, String metodo,
		String referenciaPasarela) {
}
