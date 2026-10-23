package co.edu.unbosque.pollos_hermanos_back;

import java.util.Arrays;
import java.util.HashSet;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;

/**
 * Utilidades para construir usuarios y autenticaciones en las pruebas.
 */
public final class UsuariosDePrueba {

	/** Clase de utilidades: no se instancia. */
	private UsuariosDePrueba() {
	}

	/**
	 * Crea un usuario activo con un rol que otorga los permisos indicados.
	 *
	 * @param codigos códigos de permiso
	 * @return el usuario
	 */
	public static Usuario conPermisos(String... codigos) {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setNombreUsuario("operario");
		usuario.setCorreo("operario@pollos.co");
		Rol rol = new Rol("Operario", null);
		rol.setPermisos(new HashSet<>(Arrays.stream(codigos).map(codigo -> new Permiso(codigo, null)).toList()));
		usuario.getRoles().add(rol);
		return usuario;
	}

	/**
	 * Crea un superusuario activo sin permisos asignados.
	 *
	 * @return el superusuario
	 */
	public static Usuario superusuario() {
		Usuario usuario = conPermisos();
		usuario.setNombreUsuario("admin");
		usuario.setSuperusuario(true);
		return usuario;
	}

	/**
	 * Crea la autenticación de Spring Security para el usuario.
	 *
	 * @param usuario usuario autenticado
	 * @return autenticación con sus autoridades
	 */
	public static Authentication autenticacion(Usuario usuario) {
		return new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
	}
}
