package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Datos para registrar la venta de un pedido confirmado con
 * {@code sp_registrar_venta}.
 *
 * @param idPedido        pedido confirmado
 * @param fechaVenta      fecha; si es {@code null}, hoy
 * @param pesoTotalLibras peso total vendido en libras
 * @param precioLibra     precio por libra, que queda fijo
 * @param fechaLimitePago fecha límite; si es {@code null}, 30 días después
 */
public record RegistroVentaDTO(
		@NotNull(message = "es obligatorio") Long idPedido,
		@PastOrPresent(message = "no puede ser futura") LocalDate fechaVenta,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") @Digits(integer = 8, fraction = 2, message = "admite máximo 8 cifras enteras y 2 decimales") BigDecimal pesoTotalLibras,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") @Digits(integer = 12, fraction = 2, message = "admite máximo 12 cifras enteras y 2 decimales") BigDecimal precioLibra,
		LocalDate fechaLimitePago) {
}
