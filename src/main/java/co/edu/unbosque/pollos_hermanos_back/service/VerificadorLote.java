package co.edu.unbosque.pollos_hermanos_back.service;

import java.time.LocalDate;

/**
 * Consulta en PostgreSQL el lote al que se le va a registrar un seguimiento en
 * MongoDB, para que ambas bases manejen el mismo lote, el mismo día y la misma
 * edad.
 */
public interface VerificadorLote {

	/**
	 * Verifica que el lote exista y esté activo, y devuelve su fecha de ingreso.
	 *
	 * @param codigoLote código del lote
	 * @return fecha de ingreso del lote registrada en PostgreSQL
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException si el lote no existe
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException si el lote no está activo
	 */
	LocalDate obtenerFechaIngresoLoteActivo(String codigoLote);
}
