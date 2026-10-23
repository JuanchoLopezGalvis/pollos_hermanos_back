package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada y salida del seguimiento diario del crecimiento de un lote.
 * <p>
 * La edad del lote no se recibe: el servicio la calcula con la fecha de ingreso
 * registrada en PostgreSQL.
 */
public class SeguimientoCrecimientoDTO {

	/** Identificador del documento en MongoDB. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String id;

	/** Código del lote en PostgreSQL. */
	@NotBlank(groups = AlCrear.class, message = "es obligatorio")
	@Size(max = 30, message = "admite máximo 30 caracteres")
	private String codigoLote;

	/** Día del seguimiento; si no se envía, se usa la fecha actual. */
	@PastOrPresent(message = "no puede ser una fecha futura")
	private LocalDate fecha;

	/** Edad del lote en días, calculada por el sistema. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer edadDias;

	/** Peso promedio de las aves en gramos. */
	@Positive(message = "debe ser mayor que cero")
	private Double pesoPromedioGramos;

	/** Temperatura del galpón en grados Celsius. */
	@DecimalMin(value = "-10.0", message = "no puede ser menor que -10 °C")
	@DecimalMax(value = "60.0", message = "no puede ser mayor que 60 °C")
	private Double temperaturaCelsius;

	/** Humedad relativa del galpón en porcentaje. */
	@DecimalMin(value = "0.0", message = "no puede ser menor que 0")
	@DecimalMax(value = "100.0", message = "no puede ser mayor que 100")
	private Double humedadRelativa;

	/** Observaciones del operario. */
	@Size(max = 1000, message = "admite máximo 1000 caracteres")
	private String observaciones;

	/** Muestreo de peso, si ese día se hizo. */
	@Valid
	private MuestreoPesoDTO muestreo;

	/** Novedades del galpón. */
	@Valid
	private List<NovedadGalponDTO> novedades;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public SeguimientoCrecimientoDTO() {
	}

	/**
	 * Obtiene el identificador.
	 *
	 * @return identificador
	 */
	public String getId() {
		return id;
	}

	/**
	 * Cambia el identificador.
	 *
	 * @param id identificador
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Obtiene el código del lote.
	 *
	 * @return código del lote
	 */
	public String getCodigoLote() {
		return codigoLote;
	}

	/**
	 * Cambia el código del lote.
	 *
	 * @param codigoLote código del lote
	 */
	public void setCodigoLote(String codigoLote) {
		this.codigoLote = codigoLote;
	}

	/**
	 * Obtiene la fecha.
	 *
	 * @return fecha
	 */
	public LocalDate getFecha() {
		return fecha;
	}

	/**
	 * Cambia la fecha.
	 *
	 * @param fecha fecha
	 */
	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	/**
	 * Obtiene la edad del lote en días.
	 *
	 * @return edad en días
	 */
	public Integer getEdadDias() {
		return edadDias;
	}

	/**
	 * Cambia la edad del lote en días.
	 *
	 * @param edadDias edad en días
	 */
	public void setEdadDias(Integer edadDias) {
		this.edadDias = edadDias;
	}

	/**
	 * Obtiene el peso promedio.
	 *
	 * @return peso promedio en gramos
	 */
	public Double getPesoPromedioGramos() {
		return pesoPromedioGramos;
	}

	/**
	 * Cambia el peso promedio.
	 *
	 * @param pesoPromedioGramos peso promedio en gramos
	 */
	public void setPesoPromedioGramos(Double pesoPromedioGramos) {
		this.pesoPromedioGramos = pesoPromedioGramos;
	}

	/**
	 * Obtiene la temperatura.
	 *
	 * @return temperatura en grados Celsius
	 */
	public Double getTemperaturaCelsius() {
		return temperaturaCelsius;
	}

	/**
	 * Cambia la temperatura.
	 *
	 * @param temperaturaCelsius temperatura en grados Celsius
	 */
	public void setTemperaturaCelsius(Double temperaturaCelsius) {
		this.temperaturaCelsius = temperaturaCelsius;
	}

	/**
	 * Obtiene la humedad relativa.
	 *
	 * @return humedad en porcentaje
	 */
	public Double getHumedadRelativa() {
		return humedadRelativa;
	}

	/**
	 * Cambia la humedad relativa.
	 *
	 * @param humedadRelativa humedad en porcentaje
	 */
	public void setHumedadRelativa(Double humedadRelativa) {
		this.humedadRelativa = humedadRelativa;
	}

	/**
	 * Obtiene las observaciones.
	 *
	 * @return observaciones
	 */
	public String getObservaciones() {
		return observaciones;
	}

	/**
	 * Cambia las observaciones.
	 *
	 * @param observaciones observaciones
	 */
	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}

	/**
	 * Obtiene el muestreo de peso.
	 *
	 * @return muestreo
	 */
	public MuestreoPesoDTO getMuestreo() {
		return muestreo;
	}

	/**
	 * Cambia el muestreo de peso.
	 *
	 * @param muestreo muestreo
	 */
	public void setMuestreo(MuestreoPesoDTO muestreo) {
		this.muestreo = muestreo;
	}

	/**
	 * Obtiene las novedades del galpón.
	 *
	 * @return novedades
	 */
	public List<NovedadGalponDTO> getNovedades() {
		return novedades;
	}

	/**
	 * Cambia las novedades del galpón.
	 *
	 * @param novedades novedades
	 */
	public void setNovedades(List<NovedadGalponDTO> novedades) {
		this.novedades = novedades;
	}
}
