package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o modificar un empleado.
 *
 * @param documento    documento de identidad, único
 * @param nombres      nombres
 * @param apellidos    apellidos
 * @param telefono     teléfono, opcional
 * @param idCargo      cargo del empleado
 * @param fechaIngreso fecha de ingreso a la finca
 */
public record RegistroEmpleadoDTO(
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^\\d{5,15}(-\\d)?$", message = "debe tener de 5 a 15 dígitos, sin puntos ni espacios (NIT: 900123456-7)") String documento,
		@NotBlank(message = "son obligatorios") @Size(max = 80, message = "admiten máximo 80 caracteres") String nombres,
		@NotBlank(message = "son obligatorios") @Size(max = 80, message = "admiten máximo 80 caracteres") String apellidos,
		@Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^$|^\\+?\\d{7,15}$", message = "debe tener de 7 a 15 dígitos, sin espacios ni guiones") String telefono,
		@NotNull(message = "es obligatorio") Long idCargo,
		@NotNull(message = "es obligatoria") @PastOrPresent(message = "no puede ser futura") LocalDate fechaIngreso) {
}
