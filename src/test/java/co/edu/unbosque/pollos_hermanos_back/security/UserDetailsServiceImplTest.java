package co.edu.unbosque.pollos_hermanos_back.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Pruebas de {@link UserDetailsServiceImpl}.
 */
class UserDetailsServiceImplTest {

	/** Repositorio simulado. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Servicio bajo prueba. */
	private final UserDetailsServiceImpl servicio = new UserDetailsServiceImpl(usuarioRepository);

	/** Encuentra al usuario por nombre de usuario o correo. */
	@Test
	void cargaUsuarioPorNombreOCorreo() {
		when(usuarioRepository.findByNombreUsuarioOrCorreo("operario@pollos.co", "operario@pollos.co"))
				.thenReturn(Optional.of(UsuariosDePrueba.conPermisos("seguimiento.ver")));

		assertEquals("operario", servicio.loadUserByUsername("operario@pollos.co").getUsername());
	}

	/** Si no existe, lanza UsernameNotFoundException. */
	@Test
	void usuarioInexistenteLanzaExcepcion() {
		when(usuarioRepository.findByNombreUsuarioOrCorreo("nadie", "nadie")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> servicio.loadUserByUsername("nadie"));
	}
}
