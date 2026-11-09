package co.edu.unbosque.pollos_hermanos_back.document;

/**
 * Novedad del galpón observada durante el seguimiento diario, como cama húmeda
 * o falla en bebederos. Se guarda embebida en {@link SeguimientoCrecimiento}.
 */
public class NovedadGalpon {

	/** Tipo de novedad, por ejemplo "Cama húmeda". */
	private String tipo;

	/** Descripción de lo observado. */
	private String descripcion;

	/** Constructor vacío requerido por Spring Data y ModelMapper. */
	public NovedadGalpon() {
	}

	/**
	 * Crea una novedad con todos sus datos.
	 *
	 * @param tipo        tipo de novedad
	 * @param descripcion descripción de lo observado
	 */
	public NovedadGalpon(String tipo, String descripcion) {
		this.tipo = tipo;
		this.descripcion = descripcion;
	}

	/**
	 * Obtiene el tipo de novedad.
	 *
	 * @return tipo de novedad
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Cambia el tipo de novedad.
	 *
	 * @param tipo tipo de novedad
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Obtiene la descripción de la novedad.
	 *
	 * @return descripción
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Cambia la descripción de la novedad.
	 *
	 * @param descripcion descripción
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
}
