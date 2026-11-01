package co.edu.unbosque.pollos_hermanos_back.exception;

/**
 * Se lanza cuando no es posible construir o enviar un correo. El manejador
 * global la responde con 503.
 */
public class CorreoException extends RuntimeException {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con un mensaje y su causa original.
	 *
	 * @param mensaje descripción en español del error
	 * @param causa   excepción que originó el error
	 */
	public CorreoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
	// hols
	
}
