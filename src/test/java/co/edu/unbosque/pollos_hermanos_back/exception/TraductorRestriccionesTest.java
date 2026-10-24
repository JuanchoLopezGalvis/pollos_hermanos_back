package co.edu.unbosque.pollos_hermanos_back.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link TraductorRestricciones}.
 */
class TraductorRestriccionesTest {

	/** Extrae el nombre de la restricción del mensaje de PostgreSQL. */
	@Test
	void extraeNombreRestriccion() {
		assertEquals("uq_cliente_documento", TraductorRestricciones
				.nombreRestriccion("ERROR: duplicate key value violates unique constraint \"uq_cliente_documento\""));
		assertNull(TraductorRestricciones.nombreRestriccion("ERROR: otro error"));
	}
}
