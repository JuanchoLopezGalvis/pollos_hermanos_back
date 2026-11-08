package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Datos para iniciar un pago en línea de una venta.
 *
 * @param monto      valor a pagar; no puede superar el saldo
 * @param urlRetorno página del frontend a la que vuelve la pasarela
 */
public record IniciarPagoEnLineaDTO(
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero")
		@Digits(integer = 12, fraction = 2, message = "admite máximo 2 decimales") BigDecimal monto,
		@NotBlank(message = "es obligatoria") @Pattern(regexp = "^https?://\\S+$", message = "debe ser una URL http o https") String urlRetorno) {
}
