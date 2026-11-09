package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

/**
 * Filtros para consultar la auditoría. Los que llegan vacíos no se aplican.
 *
 * @param tabla      tabla exacta
 * @param operacion  INSERT, UPDATE, DELETE o TRUNCATE
 * @param usuario    parte del nombre del usuario de la aplicación
 * @param soloSistema solo los cambios sin usuario de la aplicación (Wompi, tareas programadas, scripts)
 * @param idRegistro llave primaria exacta de la fila
 * @param desde      fecha inicial, inclusive
 * @param hasta      fecha final, inclusive
 */
public record FiltroAuditoriaDTO(String tabla, String operacion, String usuario, boolean soloSistema, String idRegistro,
		LocalDate desde, LocalDate hasta) {
}
