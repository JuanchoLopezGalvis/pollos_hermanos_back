package co.edu.unbosque.pollos_hermanos_back.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;

/**
 * Pruebas de la lógica de permisos de {@link UsuarioBase}.
 */
class UsuarioBaseTest {

	/** Los permisos de los roles y los directos se combinan. */
	@Test
	void combinaPermisosDeRolesYDirectos() {
		Usuario usuario = UsuariosDePrueba.conPermisos("seguimiento.ver");
		usuario.getPermisos().add(new Permiso("lote.crear", null));

		assertEquals(Set.of("lote.crear", "seguimiento.ver"), usuario.obtenerTodosLosPermisos());
		assertTrue(usuario.tienePermiso("lote.crear"));
		assertTrue(usuario.tienePermiso("seguimiento.ver"));
		assertFalse(usuario.tienePermiso("pago.registrar"));
	}

	/** Un superusuario tiene cualquier permiso sin asignárselo. */
	@Test
	void superusuarioTieneTodosLosPermisos() {
		Usuario usuario = UsuariosDePrueba.superusuario();

		assertTrue(usuario.tienePermiso("cualquier.permiso"));
		assertTrue(usuario.getAuthorities().stream()
				.anyMatch(a -> UsuarioBase.ROL_SUPERUSUARIO.equals(a.getAuthority())));
	}

	/** Un usuario inactivo no tiene permisos, aunque sea superusuario. */
	@Test
	void usuarioInactivoNoTienePermisos() {
		Usuario usuario = UsuariosDePrueba.superusuario();
		usuario.setActivo(false);

		assertFalse(usuario.tienePermiso("seguimiento.ver"));
		assertFalse(usuario.isEnabled());
	}

	/** El nombre completo usa el nombre de usuario si no hay nombres. */
	@Test
	void nombreCompletoUsaNombreUsuarioSiNoHayNombres() {
		Usuario usuario = UsuariosDePrueba.conPermisos();
		assertEquals("operario", usuario.obtenerNombreCompleto());

		usuario.setNombres("Dylan");
		usuario.setApellidos("Mancipe");
		assertEquals("Dylan Mancipe", usuario.obtenerNombreCompleto());
	}
}
