package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuenta que el personal crea para un cliente ya registrado (D25).
 *
 * @param nombreUsuario nombre de usuario para iniciar sesión
 * @param contrasena    contraseña inicial; se guarda cifrada con BCrypt
 * @param correo        correo de la cuenta; si no llega, se usa el del cliente
 */
public record CuentaClienteDTO(
		@NotBlank(message = "es obligatorio") @Size(min = 3, max = 50, message = "debe tener entre 3 y 50 caracteres")
		@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "solo admite letras, números, punto, guion y guion bajo") String nombreUsuario,
		@NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String contrasena,
		@Email(message = "no es un correo válido") @Size(max = 120, message = "admite máximo 120 caracteres") String correo) {
}
