package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.CatalogoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.CatalogoRepository;

/**
 * Pruebas de {@link CatalogoService}.
 */
class CatalogoServiceTest {

	/** Repositorio simulado. */
	private final CatalogoRepository catalogoRepository = mock(CatalogoRepository.class);

	/** Servicio bajo prueba. */
	private final CatalogoService catalogoService = new CatalogoService(catalogoRepository);

	/** Un catálogo existente devuelve sus elementos. */
	@Test
	void obtieneCatalogo() {
		when(catalogoRepository.obtener("cargos")).thenReturn(Optional.of(List.of(new CatalogoDTO(5L, "BENEFICIADORA", "Beneficiadora"))));

		assertEquals("BENEFICIADORA", catalogoService.obtener("cargos").get(0).codigo());
	}

	/** Un catálogo desconocido responde 404. */
	@Test
	void catalogoInexistente() {
		when(catalogoRepository.obtener("planetas")).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> catalogoService.obtener("planetas"));
	}
}
