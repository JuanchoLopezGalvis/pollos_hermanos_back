package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PaginaAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.AuditoriaRepository;

/**
 * Pruebas de {@link AuditoriaService}.
 */
class AuditoriaServiceTest {

	/** Repositorio simulado. */
	private final AuditoriaRepository auditoriaRepository = mock(AuditoriaRepository.class);

	/** Servicio bajo prueba. */
	private final AuditoriaService auditoriaService = new AuditoriaService(auditoriaRepository);

	/** Sin filtros. */
	private final FiltroAuditoriaDTO sinFiltros = new FiltroAuditoriaDTO(null, null, null, false, null, null, null);

	/** La página 2 de 50 salta los primeros 100 cambios y trae el total. */
	@Test
	void calculaElDesplazamiento() {
		when(auditoriaRepository.buscar(sinFiltros, 50, 100L)).thenReturn(List.of());
		when(auditoriaRepository.contar(sinFiltros)).thenReturn(130L);

		PaginaAuditoriaDTO pagina = auditoriaService.buscar(sinFiltros, 2, 50);

		assertEquals(130L, pagina.total());
		assertEquals(2, pagina.pagina());
	}

	/** Un tamaño exagerado se limita al máximo y una página negativa pasa a 0. */
	@Test
	void ajustaPaginaYTamano() {
		when(auditoriaRepository.buscar(any(), eq(AuditoriaService.TAMANO_MAXIMO), eq(0L))).thenReturn(List.of());

		PaginaAuditoriaDTO pagina = auditoriaService.buscar(sinFiltros, -3, 10_000);

		assertEquals(0, pagina.pagina());
		assertEquals(AuditoriaService.TAMANO_MAXIMO, pagina.tamano());
		verify(auditoriaRepository).buscar(sinFiltros, AuditoriaService.TAMANO_MAXIMO, 0L);
	}

	/** Una operación que no existe se rechaza. */
	@Test
	void operacionInexistenteSeRechaza() {
		FiltroAuditoriaDTO filtro = new FiltroAuditoriaDTO(null, "BORRAR", null, false, null, null, null);

		assertThrows(ReglaNegocioException.class, () -> auditoriaService.buscar(filtro, 0, 50));
	}

	/** La fecha inicial no puede ser posterior a la final. */
	@Test
	void fechasInvertidasSeRechazan() {
		FiltroAuditoriaDTO filtro = new FiltroAuditoriaDTO(null, null, null, false, null, LocalDate.of(2026, 11, 2),
				LocalDate.of(2026, 11, 1));

		assertThrows(ReglaNegocioException.class, () -> auditoriaService.buscar(filtro, 0, 50));
	}
}
