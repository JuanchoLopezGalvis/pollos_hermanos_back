package co.edu.unbosque.pollos_hermanos_back.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;

/**
 * Pruebas de {@link EvaluadorPermisos}.
 */
class EvaluadorPermisosTest {

	/** Evaluador bajo prueba. */
	private final EvaluadorPermisos permisos = new EvaluadorPermisos();

	/** Limpia el contexto de seguridad después de cada prueba. */
	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	/** Sin autenticación no hay permisos. */
	@Test
	void sinAutenticacionNoTienePermisos() {
		assertFalse(permisos.tiene("seguimiento.ver"));
		assertFalse(permisos.esSuperusuario());
		assertNull(permisos.usuarioActual());
	}

	/** Se consultan los permisos del usuario autenticado. */
	@Test
	void consultaLosPermisosDelUsuarioAutenticado() {
		SecurityContextHolder.getContext()
				.setAuthentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.ver")));

		assertTrue(permisos.tiene("seguimiento.ver"));
		assertFalse(permisos.tiene("seguimiento.registrar"));
		assertTrue(permisos.tieneAlguno("seguimiento.registrar", "seguimiento.ver"));
		assertFalse(permisos.esSuperusuario());
	}

	/** Un superusuario pasa cualquier verificación. */
	@Test
	void superusuarioPasaCualquierVerificacion() {
		SecurityContextHolder.getContext()
				.setAuthentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.superusuario()));

		assertTrue(permisos.tiene("usuario.gestionar"));
		assertTrue(permisos.esSuperusuario());
	}
}
