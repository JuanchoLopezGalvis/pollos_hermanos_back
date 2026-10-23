package co.edu.unbosque.pollos_hermanos_back.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.LoginDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Pruebas de {@link AuthService}.
 */
class AuthServiceTest {

	/** Administrador de autenticación simulado. */
	private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

	/** Repositorio simulado. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Servicio bajo prueba. */
	private final AuthService authService = new AuthService(authenticationManager,
			new JwtUtil("secreto-de-prueba-de-32-caracteres", 60_000), usuarioRepository);

	/** Con credenciales correctas devuelve el token y registra el ingreso. */
	@Test
	void iniciaSesionYRegistraUltimoIngreso() {
		Usuario usuario = UsuariosDePrueba.conPermisos("seguimiento.ver");
		when(authenticationManager.authenticate(any())).thenReturn(UsuariosDePrueba.autenticacion(usuario));

		TokenDTO token = authService.iniciarSesion(new LoginDTO("operario", "Clave123*"));

		assertNotNull(token.token());
		assertEquals("Bearer", token.tipo());
		assertEquals(Set.of("seguimiento.ver"), token.permisos());
		assertNotNull(usuario.getUltimoIngreso());
		verify(usuarioRepository).save(usuario);
	}

	/** Con credenciales incorrectas propaga el error y no guarda nada. */
	@Test
	void credencialesIncorrectasNoGuardanNada() {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

		assertThrows(BadCredentialsException.class,
				() -> authService.iniciarSesion(new LoginDTO("operario", "incorrecta")));
		verify(usuarioRepository, never()).save(any());
	}
}
