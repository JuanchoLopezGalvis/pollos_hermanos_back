package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o modificar un cliente. PostgreSQL exige teléfono o
 * correo, y que sean únicos (S29).
 *
 * @param idTipoCliente tipo de cliente
 * @param documento     documento o NIT
 * @param nombre        nombre o razón social
 * @param direccion     dirección, opcional
 * @param telefono      teléfono
 * @param correo        correo
 * @param activo        si es {@code null}, no cambia (al crear queda activo)
 */
public record RegistroClienteDTO(
		@NotNull(message = "es obligatorio") Long idTipoCliente,
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^\\d{5,15}(-\\d)?$", message = "debe tener de 5 a 15 dígitos, sin puntos ni espacios (NIT: 900123456-7)") String documento,
		@NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite máximo 120 caracteres") String nombre,
		@Size(max = 200, message = "admite máximo 200 caracteres") String direccion,
		@Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^$|^\\+?\\d{7,15}$", message = "debe tener de 7 a 15 dígitos, sin espacios ni guiones") String telefono,
		@Email(message = "no es un correo válido") @Size(max = 120, message = "admite máximo 120 caracteres") String correo,
		Boolean activo) {
}
