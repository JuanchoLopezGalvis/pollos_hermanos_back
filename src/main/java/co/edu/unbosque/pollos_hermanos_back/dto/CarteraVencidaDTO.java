package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Venta con saldo después de su fecha límite, leída de {@code fn_cartera_vencida}.
 *
 * @param idVenta         venta
 * @param idCliente       cliente
 * @param cliente         nombre del cliente
 * @param fechaLimitePago fecha límite de pago
 * @param diasVencida     días de mora
 * @param valorTotal      valor de la venta
 * @param saldo           saldo pendiente
 */
public record CarteraVencidaDTO(Long idVenta, Long idCliente, String cliente, LocalDate fechaLimitePago, Integer diasVencida,
		BigDecimal valorTotal, BigDecimal saldo) {
}
