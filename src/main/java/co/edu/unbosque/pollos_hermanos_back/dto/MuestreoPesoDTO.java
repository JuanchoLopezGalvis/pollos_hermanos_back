package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos de un muestreo de peso dentro del seguimiento diario.
 */
public class MuestreoPesoDTO {

	/** Cantidad de aves pesadas. */
	@NotNull(message = "es obligatorio")
	@Positive(message = "debe ser mayor que cero")
	@Max(value = 100000, message = "admite máximo 100.000 aves")
	private Integer avesPesadas;

	/** Uniformidad del lote en porcentaje. */
	@NotNull(message = "es obligatoria")
	@DecimalMin(value = "0.0", message = "no puede ser menor que 0")
	@DecimalMax(value = "100.0", message = "no puede ser mayor que 100")
	private Double uniformidadPorcentaje;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public MuestreoPesoDTO() {
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
	 * @return uniformidad
	 */
	public Double getUniformidadPorcentaje() {
		return uniformidadPorcentaje;
	}

	/**
	 * Cambia la uniformidad en porcentaje.
	 *
	 * @param uniformidadPorcentaje uniformidad
	 */
	public void setUniformidadPorcentaje(Double uniformidadPorcentaje) {
		this.uniformidadPorcentaje = uniformidadPorcentaje;
	}
}
