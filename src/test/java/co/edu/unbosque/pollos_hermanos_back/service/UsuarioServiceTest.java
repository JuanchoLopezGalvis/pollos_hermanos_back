package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.configuration.ModelMapperConfig;
import co.edu.unbosque.pollos_hermanos_back.dto.UsuarioDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Pruebas de {@link UsuarioService}.
 */
class UsuarioServiceTest {

	/** Repositorio simulado. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Servicio de roles simulado. */
	private final RolService rolService = mock(RolService.class);

	/** Servicio de permisos simulado. */
	private final PermisoService permisoService = mock(PermisoService.class);

	/** Codificador real de BCrypt. */
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	/** Servicio bajo prueba, con el ModelMapper real. */
	private final UsuarioService usuarioService = new UsuarioService(usuarioRepository, rolService, permisoService,
			passwordEncoder, new ModelMapperConfig().modelMapper());

	/** Crear cifra la contraseña, asigna roles y no devuelve la contraseña. */
	@Test
	void creaUsuarioConContrasenaCifradaYRoles() {
		UsuarioDTO dto = new UsuarioDTO();
		dto.setNombreUsuario("operario");
		dto.setCorreo("operario@pollos.co");
		dto.setContrasena("Clave123*");
		dto.setRoles(List.of("Operario"));
		Rol rol = new Rol("Operario", null);
		rol.getPermisos().add(new Permiso("seguimiento.registrar", null));
		when(rolService.buscarPorNombres(List.of("Operario"))).thenReturn(Set.of(rol));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		UsuarioDTO creado = usuarioService.crear(dto);

		verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(
				usuario -> passwordEncoder.matches("Clave123*", usuario.getContrasena())));
		assertNull(creado.getContrasena());
		assertEquals(List.of("Operario"), creado.getRoles());
		assertEquals(Set.of("seguimiento.registrar"), creado.getPermisosEfectivos());
	}

	/** Actualizar solo cambia los campos enviados. */
	@Test
	void actualizarSoloCambiaLosCamposEnviados() {
		Usuario usuario = UsuariosDePrueba.conPermisos("seguimiento.ver");
		usuario.setContrasena("hash-anterior");
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		UsuarioDTO cambios = new UsuarioDTO();
		cambios.setNombres("Gabriela");

		UsuarioDTO actualizado = usuarioService.actualizar(1L, cambios);

		assertEquals("Gabriela", actualizado.getNombres());
		assertEquals("operario", actualizado.getNombreUsuario());
		assertEquals("hash-anterior", usuario.getContrasena());
		assertEquals(List.of("Operario"), actualizado.getRoles());
	}

	/** Eliminar desactiva al usuario en lugar de borrarlo. */
	@Test
	void eliminarDesactivaAlUsuario() {
		Usuario usuario = UsuariosDePrueba.conPermisos();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

		usuarioService.eliminar(1L);

		assertFalse(usuario.isActivo());
		verify(usuarioRepository).save(usuario);
	}

	/** Consultar un usuario inexistente lanza 404. */
	@Test
	void obtenerUsuarioInexistenteLanzaExcepcion() {
		when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.obtenerPorId(5L));
	}
}
