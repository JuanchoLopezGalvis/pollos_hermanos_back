package co.edu.unbosque.pollos_hermanos_back.dto;

import java.util.Set;

/**
 * Respuesta de un inicio de sesión exitoso.
 *
 * @param token          token JWT para el encabezado {@code Authorization}
 * @param tipo           tipo de token, siempre {@code Bearer}
 * @param nombreUsuario  nombre de usuario
 * @param nombreCompleto nombres y apellidos del usuario
 * @param superusuario   indica si el usuario tiene todos los permisos
 * @param permisos       códigos de permiso del usuario, para que el frontend arme el menú
 */
public record TokenDTO(String token, String tipo, String nombreUsuario, String nombreCompleto, boolean superusuario,
		Set<String> permisos) {
}
