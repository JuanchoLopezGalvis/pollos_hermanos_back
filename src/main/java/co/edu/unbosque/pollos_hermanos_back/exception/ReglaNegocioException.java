package co.edu.unbosque.pollos_hermanos_back.exception;

/**
 * Se lanza cuando una operación incumple una regla del negocio que el backend
 * debe comprobar fuera de PostgreSQL (por ejemplo, guardar un seguimiento en
 * MongoDB de un lote que no está activo). El manejador global la responde con
 * 409.
 * <p>
 * Las reglas que viven en PostgreSQL no se duplican aquí: llegan como
 * {@code RAISE EXCEPTION} y se traducen en {@link ManejadorGlobalExcepciones}.
 */
public class ReglaNegocioException extends RuntimeException {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con un mensaje para el usuario.
	 *
	 * @param mensaje descripción en español de la regla incumplida
	 */
	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
