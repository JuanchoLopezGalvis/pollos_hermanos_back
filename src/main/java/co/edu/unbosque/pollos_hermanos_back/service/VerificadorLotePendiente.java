package co.edu.unbosque.pollos_hermanos_back.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;

/**
 * Implementación provisional de {@link VerificadorLote} mientras se aprueba el
 * modelo relacional de la Fase 1.
 * <p>
 * Rechaza todos los seguimientos, porque ningún documento puede guardarse en
 * MongoDB sin comprobar antes el lote en PostgreSQL. Cuando exista la tabla de
 * lotes (o la función que lo consulte), se reemplaza por una implementación con
 * {@code JdbcTemplate} y se borra esta clase.
 */
@Service
public class VerificadorLotePendiente implements VerificadorLote {

	/**
	 * Siempre rechaza la operación.
	 *
	 * @param codigoLote código del lote
	 * @return nunca retorna
	 * @throws ReglaNegocioException siempre
	 */
	@Override
	public LocalDate obtenerFechaIngresoLoteActivo(String codigoLote) {
		throw new ReglaNegocioException("Todavía no es posible verificar el lote " + codigoLote
				+ " en PostgreSQL: el modelo de lotes aún no está implementado.");
	}
}
