package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.configuration.ModelMapperConfig;
import co.edu.unbosque.pollos_hermanos_back.dto.RolDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.RolRepository;

/**
 * Pruebas de {@link RolService}.
 */
class RolServiceTest {

	/** Repositorio simulado. */
	private final RolRepository rolRepository = mock(RolRepository.class);

	/** Servicio de permisos simulado. */
	private final PermisoService permisoService = mock(PermisoService.class);

	/** Servicio bajo prueba, con el ModelMapper real. */
	private final RolService rolService = new RolService(rolRepository, permisoService,
			new ModelMapperConfig().modelMapper());

	/** Crear un rol le asigna los permisos por código. */
	@Test
	void creaRolConSusPermisos() {
		RolDTO dto = new RolDTO();
		dto.setNombre("Operario");
		dto.setPermisos(List.of("seguimiento.registrar"));
		when(permisoService.buscarPorCodigos(List.of("seguimiento.registrar")))
				.thenReturn(Set.of(new Permiso("seguimiento.registrar", null)));
		when(rolRepository.save(any(Rol.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		RolDTO creado = rolService.crear(dto);

		assertEquals("Operario", creado.getNombre());
		assertEquals(List.of("seguimiento.registrar"), creado.getPermisos());
	}

	/** Actualizar sin enviar permisos conserva los actuales. */
	@Test
	void actualizarSinPermisosConservaLosActuales() {
		Rol rol = new Rol("Operario", "Antes");
		rol.setId(3L);
		rol.getPermisos().add(new Permiso("seguimiento.ver", null));
		when(rolRepository.findById(3L)).thenReturn(Optional.of(rol));
		when(rolRepository.save(any(Rol.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		RolDTO cambios = new RolDTO();
		cambios.setDescripcion("Después");

		RolDTO actualizado = rolService.actualizar(3L, cambios);

		assertEquals("Operario", actualizado.getNombre());
		assertEquals("Después", actualizado.getDescripcion());
		assertEquals(List.of("seguimiento.ver"), actualizado.getPermisos());
		verify(permisoService, never()).buscarPorCodigos(any());
	}

	/** Eliminar un rol inexistente lanza 404. */
	@Test
	void eliminarRolInexistenteLanzaExcepcion() {
		when(rolRepository.findById(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> rolService.eliminar(9L));
	}
}
