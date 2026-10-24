package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Pago manual de una venta. El pago en línea no entra por aquí: lo registra el
 * backend cuando la pasarela confirma la transacción.
 *
 * @param codigoMetodo EFECTIVO, TRANSFERENCIA o CREDITO
 * @param fecha        fecha; si es {@code null}, hoy
 * @param monto        valor; no puede superar el saldo
 */
public record RegistroPagoDTO(
		@NotNull(message = "es obligatorio")
		@Pattern(regexp = "EFECTIVO|TRANSFERENCIA|CREDITO", message = "debe ser EFECTIVO, TRANSFERENCIA o CREDITO") String codigoMetodo,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") BigDecimal monto) {
}
