package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos para iniciar sesión.
 *
 * @param usuario    nombre de usuario o correo
 * @param contrasena contraseña en texto plano; solo viaja en esta solicitud
 */
public record LoginDTO(@NotBlank(message = "es obligatorio") String usuario,
		@NotBlank(message = "es obligatoria") String contrasena) {
}
