package co.edu.unbosque.pollos_hermanos_back.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Pruebas de {@link InicializadorSuperusuario}.
 */
class InicializadorSuperusuarioTest {

	/** Repositorio simulado. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Codificador real de BCrypt. */
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	/** Crea el superusuario con la contraseña cifrada si no existe. */
	@Test
	void creaSuperusuarioSiNoExiste() {
		when(usuarioRepository.existsByNombreUsuario("admin")).thenReturn(false);

		new InicializadorSuperusuario(usuarioRepository, passwordEncoder, "admin", "admin@pollos.co", "Clave123*")
				.run(new DefaultApplicationArguments());

		verify(usuarioRepository).save(argThat(usuario -> usuario.isSuperusuario()
				&& passwordEncoder.matches("Clave123*", usuario.getContrasena())));
	}

	/** No hace nada si el superusuario ya existe. */
	@Test
	void noDuplicaSuperusuarioExistente() {
		when(usuarioRepository.existsByNombreUsuario("admin")).thenReturn(true);

		new InicializadorSuperusuario(usuarioRepository, passwordEncoder, "admin", "admin@pollos.co", "Clave123*")
				.run(new DefaultApplicationArguments());

		verify(usuarioRepository, never()).save(any());
	}

	/** No hace nada si faltan variables de entorno. */
	@Test
	void noHaceNadaSinVariablesDeEntorno() {
		new InicializadorSuperusuario(usuarioRepository, passwordEncoder, "", "", "")
				.run(new DefaultApplicationArguments());

		verify(usuarioRepository, never()).save(any());
	}
}
