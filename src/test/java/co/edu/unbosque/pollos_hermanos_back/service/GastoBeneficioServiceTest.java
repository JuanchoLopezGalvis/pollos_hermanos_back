package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.JornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LineaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroJornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.GastoBeneficioRepository;

/**
 * Pruebas de {@link GastoBeneficioService}.
 */
class GastoBeneficioServiceTest {

	/** Repositorio simulado. */
	private final GastoBeneficioRepository repositorio = mock(GastoBeneficioRepository.class);

	/** Servicio bajo prueba. */
	private final GastoBeneficioService servicio = new GastoBeneficioService(repositorio);

	/** Registrar una jornada devuelve su costo total. */
	@Test
	void registraJornada() {
		RegistroJornadaBeneficioDTO dto = new RegistroJornadaBeneficioDTO(3L, null, null,
				List.of(new LineaBeneficioDTO(4L, 300, new BigDecimal("900"))));
		when(repositorio.registrarJornada(dto)).thenReturn(8L);
		when(repositorio.buscarJornada(8L)).thenReturn(Optional.of(
				new JornadaBeneficioDTO(8L, LocalDate.of(2026, 10, 20), 3L, "L-1", null, 300, 1, new BigDecimal("270000"))));

		assertEquals(new BigDecimal("270000"), servicio.registrarJornada(dto).costoTotal());
	}

	/** El detalle de una jornada inexistente responde 404 sin consultar el detalle. */
	@Test
	void detalleDeJornadaInexistente() {
		when(repositorio.buscarJornada(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerDetalle(9L));
		verify(repositorio, never()).obtenerDetalle(9L);
	}

	/** La rentabilidad de un lote inexistente responde 404. */
	@Test
	void rentabilidadDeLoteInexistente() {
		when(repositorio.buscarRentabilidad(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerRentabilidad(9L));
	}
}
