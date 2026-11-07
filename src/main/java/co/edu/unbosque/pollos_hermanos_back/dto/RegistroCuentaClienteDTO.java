package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que un cliente crea su cuenta en el portal. Crea a la vez su
 * usuario (rol Cliente) y su registro de cliente.
 *
 * @param idTipoCliente tipo de cliente (tienda, restaurante, persona natural)
 * @param documento     documento o NIT
 * @param nombre        nombre o razón social
 * @param direccion     dirección de entrega
 * @param telefono      teléfono
 * @param correo        correo; también sirve para iniciar sesión
 * @param nombreUsuario nombre de usuario para iniciar sesión
 * @param contrasena    contraseña en texto plano; se guarda cifrada con BCrypt
 */
public record RegistroCuentaClienteDTO(
		@NotNull(message = "es obligatorio") Long idTipoCliente,
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^\\d{5,15}(-\\d)?$", message = "debe tener de 5 a 15 dígitos, sin puntos ni espacios (NIT: 900123456-7)") String documento,
		@NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite máximo 120 caracteres") String nombre,
		@NotBlank(message = "es obligatoria") @Size(max = 200, message = "admite máximo 200 caracteres") String direccion,
		@Size(max = 20, message = "admite máximo 20 caracteres") @Pattern(regexp = "^$|^\\+?\\d{7,15}$", message = "debe tener de 7 a 15 dígitos, sin espacios ni guiones") String telefono,
		@NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido")
		@Size(max = 120, message = "admite máximo 120 caracteres") String correo,
		@NotBlank(message = "es obligatorio") @Size(min = 3, max = 50, message = "debe tener entre 3 y 50 caracteres")
		@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "solo admite letras, números, punto, guion y guion bajo") String nombreUsuario,
		@NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String contrasena) {
}
