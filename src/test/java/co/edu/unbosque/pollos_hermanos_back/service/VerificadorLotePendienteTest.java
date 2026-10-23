package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;

/**
 * Pruebas de {@link VerificadorLotePendiente}.
 */
class VerificadorLotePendienteTest {

	/** Mientras no exista el modelo de lotes, ningún seguimiento se acepta. */
	@Test
	void rechazaTodosLosLotes() {
		assertThrows(ReglaNegocioException.class,
				() -> new VerificadorLotePendiente().obtenerFechaIngresoLoteActivo("L-001"));
	}
}
