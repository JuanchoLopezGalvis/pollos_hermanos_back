package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.LotesDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadRegistradaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Pruebas de {@link LoteService}.
 */
class LoteServiceTest {

	/** Repositorio simulado. */
	private final LoteRepository loteRepository = mock(LoteRepository.class);

	/** Servicio bajo prueba. */
	private final LoteService loteService = new LoteService(loteRepository);

	/** Registrar un lote llama al procedimiento y devuelve el lote leído de la vista. */
	@Test
	void creaLoteConElProcedimiento() {
		LocalDate ingreso = LocalDate.of(2026, 10, 1);
		RegistroLoteDTO dto = new RegistroLoteDTO("L-2026-01", 1L, ingreso, 4000, new BigDecimal("2600"), null);
		when(loteRepository.registrar("L-2026-01", 1L, ingreso, 4000, new BigDecimal("2600"), null)).thenReturn(7L);
		when(loteRepository.buscarPorId(7L)).thenReturn(Optional.of(LotesDePrueba.lote(7L, EstadoLote.EN_ENGORDE)));

		LoteDTO creado = loteService.crear(dto);

		assertEquals(7L, creado.id());
		assertEquals(EstadoLote.EN_ENGORDE, creado.estado());
	}

	/** Corregir un lote que no existe lanza 404. */
	@Test
	void corregirLoteInexistenteLanzaExcepcion() {
		ActualizacionLoteDTO dto = new ActualizacionLoteDTO(100, BigDecimal.ONE, LocalDate.of(2026, 11, 20));
		when(loteRepository.actualizar(9L, 100, BigDecimal.ONE, LocalDate.of(2026, 11, 20))).thenReturn(false);

		assertThrows(RecursoNoEncontradoException.class, () -> loteService.actualizar(9L, dto));
	}

	/** La alerta del 5% que calcula el procedimiento llega sin cambios. */
	@Test
	void registrarMortalidadDevuelveLaAlerta() {
		MortalidadDTO dto = new MortalidadDTO(null, null, 80, "calor");
		when(loteRepository.registrarMortalidad(7L, null, 80, "calor"))
				.thenReturn(new MortalidadRegistradaDTO(3L, new BigDecimal("5.75"), true));

		MortalidadRegistradaDTO registrada = loteService.registrarMortalidad(7L, dto);

		assertTrue(registrada.alerta());
		assertEquals(new BigDecimal("5.75"), registrada.porcentajeMortalidad());
	}

	/** Consultar la mortalidad de un lote inexistente lanza 404 sin consultar los registros. */
	@Test
	void mortalidadDeLoteInexistenteLanzaExcepcion() {
		when(loteRepository.buscarPorId(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> loteService.obtenerMortalidad(9L));
		verify(loteRepository, never()).obtenerMortalidad(9L);
	}
}
