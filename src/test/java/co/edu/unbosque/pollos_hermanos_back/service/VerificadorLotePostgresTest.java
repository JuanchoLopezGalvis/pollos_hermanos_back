package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.LotesDePrueba;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Pruebas de {@link VerificadorLotePostgres}.
 */
class VerificadorLotePostgresTest {

	/** Repositorio simulado. */
	private final LoteRepository loteRepository = mock(LoteRepository.class);

	/** Verificador bajo prueba. */
	private final VerificadorLotePostgres verificador = new VerificadorLotePostgres(loteRepository);

	/** Un lote activo devuelve su fecha de ingreso. */
	@Test
	void loteActivoDevuelveFechaIngreso() {
		when(loteRepository.buscarPorCodigo("L-2026-01"))
				.thenReturn(Optional.of(LotesDePrueba.lote(1L, EstadoLote.LISTO_PARA_VENTA)));

		assertEquals(LocalDate.of(2026, 10, 1), verificador.obtenerFechaIngresoLoteActivo("L-2026-01"));
	}

	/** Un lote cerrado no admite seguimiento. */
	@Test
	void loteCerradoSeRechaza() {
		when(loteRepository.buscarPorCodigo("L-2026-01"))
				.thenReturn(Optional.of(LotesDePrueba.lote(1L, EstadoLote.CERRADO)));

		assertThrows(ReglaNegocioException.class, () -> verificador.obtenerFechaIngresoLoteActivo("L-2026-01"));
	}

	/** Un lote inexistente responde 404. */
	@Test
	void loteInexistenteSeRechaza() {
		when(loteRepository.buscarPorCodigo("X")).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> verificador.obtenerFechaIngresoLoteActivo("X"));
	}
}
