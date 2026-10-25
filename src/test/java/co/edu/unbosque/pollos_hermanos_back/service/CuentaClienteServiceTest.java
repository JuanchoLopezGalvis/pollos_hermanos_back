package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CuentaClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCuentaClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.RolRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;
import co.edu.unbosque.pollos_hermanos_back.security.AuthService;

/**
 * Pruebas de {@link CuentaClienteService}.
 */
class CuentaClienteServiceTest {

	/** Usuarios simulados. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Roles simulados. */
	private final RolRepository rolRepository = mock(RolRepository.class);

	/** Clientes simulados. */
	private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

	/** Codificador simulado. */
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	/** Autenticación simulada. */
	private final AuthService authService = mock(AuthService.class);

	/** Servicio bajo prueba. */
	private final CuentaClienteService servicio = new CuentaClienteService(usuarioRepository, rolRepository,
			clienteRepository, passwordEncoder, authService);

	/** Registro de prueba. */
	private static final RegistroCuentaClienteDTO REGISTRO = new RegistroCuentaClienteDTO(2L, "900123", "Asadero El Pollo",
			"Calle 1", null, "asadero@correo.co", "asadero", "Clave123*");

	/** Un documento que ya es de un cliente no se vincula a la cuenta nueva (D25). */
	@Test
	void documentoExistenteSeRechaza() {
		when(clienteRepository.existeDocumento("900123")).thenReturn(true);

		ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(REGISTRO));

		assertTrue(e.getMessage().contains("Pide a la finca"));
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	/** El cliente nuevo recibe solo el rol Cliente y la contraseña cifrada. */
	@Test
	void registraConRolClienteYClaveCifrada() {
		Rol cliente = new Rol(CuentaClienteService.ROL_CLIENTE, null);
		when(rolRepository.findByNombre(CuentaClienteService.ROL_CLIENTE)).thenReturn(Optional.of(cliente));
		when(passwordEncoder.encode("Clave123*")).thenReturn("$2a$cifrada");
		when(usuarioRepository.saveAndFlush(any())).thenAnswer(i -> {
			Usuario u = i.getArgument(0);
			u.setId(9L);
			return u;
		});

		servicio.registrar(REGISTRO);

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).saveAndFlush(captor.capture());
		Usuario usuario = captor.getValue();
		assertEquals("$2a$cifrada", usuario.getContrasena());
		assertEquals(1, usuario.getRoles().size());
		assertTrue(usuario.getRoles().contains(cliente));
		verify(clienteRepository).crearConUsuario(REGISTRO, 9L);
		verify(authService).emitirToken(usuario);
	}

	/** Un nombre de usuario en uso se rechaza con un mensaje claro. */
	@Test
	void usuarioRepetidoSeRechaza() {
		when(usuarioRepository.existsByNombreUsuario("asadero")).thenReturn(true);

		ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(REGISTRO));

		assertTrue(e.getMessage().contains("ya está en uso"));
	}

	/** Un cliente sin correo necesita uno para su cuenta. */
	@Test
	void cuentaSinCorreoSeRechaza() {
		when(clienteRepository.buscar(4L)).thenReturn(Optional.of(
				new ClienteDTO(4L, 2L, "Tienda", "800", "Tienda Luz", null, "3001112233", null, true, null, null)));

		assertThrows(ReglaNegocioException.class,
				() -> servicio.crearCuenta(4L, new CuentaClienteDTO("luz", "Clave123*", null)));
	}

	/** Un cliente que ya tiene cuenta no recibe otra. */
	@Test
	void clienteConCuentaNoRecibeOtra() {
		when(clienteRepository.buscar(4L)).thenReturn(Optional.of(
				new ClienteDTO(4L, 2L, "Tienda", "800", "Tienda Luz", null, null, "luz@correo.co", true, 7L, "luz")));

		ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
				() -> servicio.crearCuenta(4L, new CuentaClienteDTO("luz2", "Clave123*", null)));

		assertTrue(e.getMessage().contains("ya tiene la cuenta 'luz'"));
	}
}
