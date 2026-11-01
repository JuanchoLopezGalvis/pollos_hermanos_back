package co.edu.unbosque.pollos_hermanos_back.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.edu.unbosque.pollos_hermanos_back.entity.UsuarioBase;

/**
 * Consulta los permisos del usuario autenticado desde las anotaciones de
 * seguridad, como {@code user.has_perm()} y {@code @permission_required} de
 * Django.
 * <p>
 * Uso en un controlador:
 * {@code @PreAuthorize("@permisos.tiene('lote.crear')")}. Un superusuario
 * activo pasa cualquier verificación.
 */
@Component("permisos")
public class EvaluadorPermisos {

	/**
	 * Indica si el usuario autenticado tiene el permiso.
	 *
	 * @param codigo código del permiso, por ejemplo {@code lote.crear}
	 * @return {@code true} si lo tiene
	 */
	public boolean tiene(String codigo) {
		UsuarioBase usuario = usuarioActual();
		return usuario != null && usuario.tienePermiso(codigo);
	}

	/**
	 * Indica si el usuario autenticado tiene al menos uno de los permisos.
	 *
	 * @param codigos códigos de permiso
	 * @return {@code true} si tiene alguno
	 */
	public boolean tieneAlguno(String... codigos) {
		UsuarioBase usuario = usuarioActual();
		if (usuario == null) {
			return false;
		}
		for (String codigo : codigos) {
			if (usuario.tienePermiso(codigo)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Indica si el usuario autenticado es un superusuario activo.
	 *
	 * @return {@code true} si es superusuario
	 */
	public boolean esSuperusuario() {
		UsuarioBase usuario = usuarioActual();
		return usuario != null && usuario.isActivo() && usuario.isSuperusuario();
	}

	/**
	 * Obtiene el usuario autenticado en la solicitud actual.
	 *
	 * @return el usuario, o {@code null} si la solicitud no está autenticada
	 */
	public UsuarioBase usuarioActual() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioBase usuario) {
			return usuario;
		}
		return null;
	}
}
