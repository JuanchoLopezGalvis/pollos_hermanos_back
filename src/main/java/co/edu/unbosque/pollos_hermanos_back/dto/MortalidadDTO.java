package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Registro de mortalidad de un lote en un día.
 *
 * @param id       identificador del registro
 * @param fecha    día de la mortalidad; si es {@code null} al registrar, se usa
 *                 la fecha actual
 * @param cantidad aves muertas ese día
 * @param causa    causa observada, opcional
 */
public record MortalidadDTO(
		@JsonProperty(access = JsonProperty.Access.READ_ONLY) Long id,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Max(value = 100000, message = "admite máximo 100.000 aves") Integer cantidad,
		@Size(max = 200, message = "admite máximo 200 caracteres") String causa) {
}
