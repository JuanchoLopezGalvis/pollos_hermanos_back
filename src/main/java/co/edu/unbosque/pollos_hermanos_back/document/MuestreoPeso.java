package co.edu.unbosque.pollos_hermanos_back.document;

/**
 * Muestreo de peso que se hace algunos días dentro del seguimiento del
 * crecimiento. Se guarda embebido en {@link SeguimientoCrecimiento}.
 */
public class MuestreoPeso {

	/** Cantidad de aves pesadas en el muestreo. */
	private Integer avesPesadas;

	/** Uniformidad del lote en porcentaje (0 a 100). */
	private Double uniformidadPorcentaje;

	/** Constructor vacío requerido por Spring Data y ModelMapper. */
	public MuestreoPeso() {
	}

	/**
	 * Crea un muestreo con todos sus datos.
	 *
	 * @param avesPesadas           cantidad de aves pesadas
	 * @param uniformidadPorcentaje uniformidad en porcentaje
	 */
	public MuestreoPeso(Integer avesPesadas, Double uniformidadPorcentaje) {
		this.avesPesadas = avesPesadas;
		this.uniformidadPorcentaje = uniformidadPorcentaje;
	}

	/**
	 * Obtiene la cantidad de aves pesadas.
	 *
	 * @return cantidad de aves pesadas
	 */
	public Integer getAvesPesadas() {
		return avesPesadas;
	}

	/**
	 * Cambia la cantidad de aves pesadas.
	 *
	 * @param avesPesadas cantidad de aves pesadas
	 */
	public void setAvesPesadas(Integer avesPesadas) {
		this.avesPesadas = avesPesadas;
	}

	/**
	 * Obtiene la uniformidad en porcentaje.
	 *
	 * @return uniformidad en porcentaje
	 */
	public Double getUniformidadPorcentaje() {
		return uniformidadPorcentaje;
	}

	/**
	 * Cambia la uniformidad en porcentaje.
	 *
	 * @param uniformidadPorcentaje uniformidad en porcentaje
	 */
	public void setUniformidadPorcentaje(Double uniformidadPorcentaje) {
		this.uniformidadPorcentaje = uniformidadPorcentaje;
	}
}
