package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un gasto con {@code sp_registrar_gasto}. Los pagos a las
 * beneficiadoras no van aquí: se registran en el beneficio (D4).
 *
 * @param idCategoriaGasto categoría
 * @param idLote           lote; {@code null} para un gasto general de la finca
 * @param fecha            fecha; si es {@code null}, hoy
 * @param monto            valor
 * @param descripcion      descripción
 */
public record RegistroGastoDTO(
		@NotNull(message = "es obligatoria") Long idCategoriaGasto,
		Long idLote,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") BigDecimal monto,
		@NotBlank(message = "es obligatoria") @Size(max = 200, message = "admite máximo 200 caracteres") String descripcion) {
}
