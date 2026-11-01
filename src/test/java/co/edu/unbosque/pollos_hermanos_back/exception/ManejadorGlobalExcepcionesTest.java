package co.edu.unbosque.pollos_hermanos_back.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;

/**
 * Pruebas de la traducción de errores de PostgreSQL a respuestas HTTP.
 */
class ManejadorGlobalExcepcionesTest {

	/** Manejador bajo prueba. */
	private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

	/** Un RAISE EXCEPTION responde 409 con el mensaje escrito en PL/pgSQL. */
	@Test
	void raiseExceptionRespondeConflictoConMensajeDePostgres() {
		SQLException sql = new SQLException(
				"ERROR: El galpón G-01 ya tiene un lote activo\n  Dónde: función PL/pgSQL sp_registrar_lote", "P0001");

		ResponseEntity<ErrorApiDTO> respuesta = manejador
				.manejarAccesoDatos(new UncategorizedSQLException("CALL sp_registrar_lote", "CALL", sql));

		assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
		assertEquals("El galpón G-01 ya tiene un lote activo", respuesta.getBody().mensaje());
	}

	/** Una violación de CHECK responde 400. */
	@Test
	void violacionCheckRespondeSolicitudInvalida() {
		SQLException sql = new SQLException("ERROR: new row violates check constraint", "23514");

		ResponseEntity<ErrorApiDTO> respuesta = manejador
				.manejarAccesoDatos(new DataIntegrityViolationException("check", sql));

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
	}

	/** Una violación de llave foránea responde 409. */
	@Test
	void violacionLlaveForaneaRespondeConflicto() {
		SQLException sql = new SQLException("ERROR: violates foreign key constraint", "23503");

		ResponseEntity<ErrorApiDTO> respuesta = manejador
				.manejarAccesoDatos(new DataIntegrityViolationException("fk", sql));

		assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
	}

	/** Si la base de datos no responde, se informa 503. */
	@Test
	void baseDeDatosCaidaRespondeServicioNoDisponible() {
		ResponseEntity<ErrorApiDTO> respuesta = manejador
				.manejarAccesoDatos(new DataAccessResourceFailureException("sin conexión"));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
	}

	/** Una regla de negocio del backend responde 409 con su mensaje. */
	@Test
	void reglaNegocioRespondeConflicto() {
		ResponseEntity<ErrorApiDTO> respuesta = manejador
				.manejarReglaNegocio(new ReglaNegocioException("El lote no está activo."));

		assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
		assertEquals("El lote no está activo.", respuesta.getBody().mensaje());
	}

	/** Un mensaje vacío de PostgreSQL se reemplaza por uno genérico. */
	@Test
	void mensajeVacioDePostgresUsaMensajeGenerico() {
		assertEquals("La base de datos rechazó la operación.",
				ManejadorGlobalExcepciones.mensajePostgres(new SQLException("", "P0001")));
	}
}
