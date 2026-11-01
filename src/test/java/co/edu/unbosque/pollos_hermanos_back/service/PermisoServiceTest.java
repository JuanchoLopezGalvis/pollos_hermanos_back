package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.configuration.ModelMapperConfig;
import co.edu.unbosque.pollos_hermanos_back.dto.PermisoDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.PermisoRepository;

/**
 * Pruebas de {@link PermisoService}.
 */
class PermisoServiceTest {

	/** Repositorio simulado. */
	private final PermisoRepository permisoRepository = mock(PermisoRepository.class);

	/** Servicio bajo prueba, con el ModelMapper real. */
	private final PermisoService permisoService = new PermisoService(permisoRepository,
			new ModelMapperConfig().modelMapper());

	/** Crear devuelve el permiso guardado. */
	@Test
	void creaPermiso() {
		when(permisoRepository.save(any(Permiso.class))).thenAnswer(invocacion -> {
			Permiso permiso = invocacion.getArgument(0);
			permiso.setId(7L);
			return permiso;
		});

		PermisoDTO creado = permisoService.crear(new PermisoDTO("lote.crear", "Registrar lotes"));

		assertEquals(7L, creado.getId());
		assertEquals("lote.crear", creado.getCodigo());
	}

	/** Buscar por códigos devuelve los permisos si existen todos. */
	@Test
	void buscaPermisosPorCodigo() {
		when(permisoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(new Permiso("lote.crear", null)));

		assertEquals(1, permisoService.buscarPorCodigos(List.of("lote.crear")).size());
	}

	/** Si algún código no existe, se informa cuál. */
	@Test
	void informaLosCodigosQueNoExisten() {
		when(permisoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(new Permiso("lote.crear", null)));

		RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
				() -> permisoService.buscarPorCodigos(List.of("lote.crear", "pago.inventado")));
		assertTrue(error.getMessage().contains("pago.inventado"));
	}

	/** Consultar un id inexistente lanza 404. */
	@Test
	void obtenerPermisoInexistenteLanzaExcepcion() {
		when(permisoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> permisoService.obtenerPorId(99L));
	}
}
