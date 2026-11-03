package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Datos para registrar el consumo de alimento de un lote con
 * {@code sp_registrar_consumo_alimento}. PostgreSQL valida la etapa según la
 * edad del lote y el inventario.
 *
 * @param idLote         lote
 * @param idTipoAlimento tipo de alimento
 * @param fecha          día del consumo; si es {@code null}, hoy
 * @param cantidadBultos bultos consumidos
 */
public record RegistroConsumoAlimentoDTO(
		@NotNull(message = "es obligatorio") Long idLote,
		@NotNull(message = "es obligatorio") Long idTipoAlimento,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Digits(integer = 6, fraction = 2, message = "admite máximo 6 cifras enteras y 2 decimales") BigDecimal cantidadBultos) {
}
