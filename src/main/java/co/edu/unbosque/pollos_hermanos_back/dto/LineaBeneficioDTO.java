package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Participación de una beneficiadora al registrar una jornada.
 *
 * @param idEmpleado       beneficiadora (empleada activa con ese cargo)
 * @param avesBeneficiadas pollos que benefició
 * @param tarifaPorAve     tarifa pagada por pollo
 */
public record LineaBeneficioDTO(
		@NotNull(message = "es obligatoria") Long idEmpleado,
		@NotNull(message = "son obligatorias") @Positive(message = "deben ser más de cero") Integer avesBeneficiadas,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") BigDecimal tarifaPorAve) {
}
