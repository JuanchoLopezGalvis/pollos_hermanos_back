package co.edu.unbosque.pollos_hermanos_back.exception;

/**
 * Se lanza cuando se consulta, actualiza o elimina un recurso que no existe. El
 * manejador global la responde con 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con un mensaje para el usuario.
	 *
	 * @param mensaje descripción en español del recurso que no se encontró
	 */
	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}
}
