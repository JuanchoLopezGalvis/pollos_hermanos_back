package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Datos para registrar una compra sanitaria con {@code sp_registrar_compra_sanitaria}.
 *
 * @param idProducto    producto
 * @param fecha         fecha; si es {@code null}, hoy
 * @param cantidad      cantidad en la unidad del producto
 * @param costoUnitario costo por unidad
 */
public record RegistroCompraSanitariaDTO(
		@NotNull(message = "es obligatorio") Long idProducto,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Digits(integer = 8, fraction = 2, message = "admite máximo 8 cifras enteras y 2 decimales") BigDecimal cantidad,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") @Digits(integer = 12, fraction = 2, message = "admite máximo 12 cifras enteras y 2 decimales") BigDecimal costoUnitario) {
}
