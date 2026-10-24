package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un tipo de alimento.
 *
 * @param nombre      nombre único del alimento
 * @param idEtapa     etapa en la que se usa
 * @param pesoBultoKg peso del bulto; si es {@code null}, 40 kg
 */
public record TipoAlimentoDTO(
		@NotBlank(message = "es obligatorio") @Size(max = 80, message = "admite máximo 80 caracteres") String nombre,
		@NotNull(message = "es obligatoria") Long idEtapa,
		@Positive(message = "debe ser mayor que cero") BigDecimal pesoBultoKg) {
}
