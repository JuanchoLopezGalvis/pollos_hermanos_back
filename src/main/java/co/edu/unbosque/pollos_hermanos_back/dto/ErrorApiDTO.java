package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de error de la API.
 *
 * @param estado   código de estado HTTP
 * @param mensaje  mensaje en español para mostrar al usuario
 * @param detalles detalles adicionales, por ejemplo los campos inválidos; puede estar vacía
 * @param fecha    fecha y hora en que ocurrió el error
 */
public record ErrorApiDTO(int estado, String mensaje, List<String> detalles, LocalDateTime fecha) {

	/**
	 * Crea una respuesta de error sin detalles, con la fecha actual.
	 *
	 * @param estado  código de estado HTTP
	 * @param mensaje mensaje en español
	 * @return la respuesta de error
	 */
	public static ErrorApiDTO de(int estado, String mensaje) {
		return new ErrorApiDTO(estado, mensaje, List.of(), LocalDateTime.now());
	}
}
