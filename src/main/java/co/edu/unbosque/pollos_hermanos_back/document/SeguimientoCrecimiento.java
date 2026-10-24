package co.edu.unbosque.pollos_hermanos_back.document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Seguimiento diario del crecimiento de un lote. Cada documento corresponde a
 * un lote en un día.
 * <p>
 * El documento se vincula al lote de PostgreSQL por su código y solo se guarda
 * si ese lote existe y está activo. La edad se calcula con la fecha de ingreso
 * registrada en PostgreSQL. Aquí no se guarda ningún dato transaccional
 * (mortalidad, alimento, sanidad ni costos). Un lote puede tener varios
 * seguimientos el mismo día (por ejemplo, mañana y tarde); el índice por lote y
 * fecha solo acelera las consultas.
 */
@Document(collection = "seguimientos_crecimiento")
@CompoundIndex(name = "lote_fecha", def = "{'codigoLote': 1, 'fecha': 1}")
public class SeguimientoCrecimiento {

	/** Identificador del documento en MongoDB. */
	@Id
	private String id;

	/** Código del lote en PostgreSQL. */
	private String codigoLote;

	/** Día del seguimiento. */
	private LocalDate fecha;

	/** Edad del lote en días ese día; el día de ingreso es el día 1. */
	private Integer edadDias;

	/** Peso promedio de las aves en gramos. */
	private Double pesoPromedioGramos;

	/** Temperatura del galpón en grados Celsius. */
	private Double temperaturaCelsius;

	/** Humedad relativa del galpón en porcentaje. */
	private Double humedadRelativa;

	/** Observaciones del operario. */
	private String observaciones;

	/** Muestreo de peso; solo algunos días lo tienen. */
	private MuestreoPeso muestreo;

	/** Novedades del galpón; puede estar vacía. */
	private List<NovedadGalpon> novedades = new ArrayList<>();

	/** Constructor vacío requerido por Spring Data y ModelMapper. */
	public SeguimientoCrecimiento() {
	}

	/**
	 * Obtiene el identificador del documento.
	 *
	 * @return identificador
	 */
	public String getId() {
		return id;
	}

	/**
	 * Cambia el identificador del documento.
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
	 * Obtiene el día del seguimiento.
	 *
	 * @return fecha
	 */
	public LocalDate getFecha() {
		return fecha;
	}

	/**
	 * Cambia el día del seguimiento.
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
	 * Obtiene el peso promedio en gramos.
	 *
	 * @return peso promedio
	 */
	public Double getPesoPromedioGramos() {
		return pesoPromedioGramos;
	}

	/**
	 * Cambia el peso promedio en gramos.
	 *
	 * @param pesoPromedioGramos peso promedio
	 */
	public void setPesoPromedioGramos(Double pesoPromedioGramos) {
		this.pesoPromedioGramos = pesoPromedioGramos;
	}

	/**
	 * Obtiene la temperatura del galpón.
	 *
	 * @return temperatura en grados Celsius
	 */
	public Double getTemperaturaCelsius() {
		return temperaturaCelsius;
	}

	/**
	 * Cambia la temperatura del galpón.
	 *
	 * @param temperaturaCelsius temperatura en grados Celsius
	 */
	public void setTemperaturaCelsius(Double temperaturaCelsius) {
		this.temperaturaCelsius = temperaturaCelsius;
	}

	/**
	 * Obtiene la humedad relativa del galpón.
	 *
	 * @return humedad en porcentaje
	 */
	public Double getHumedadRelativa() {
		return humedadRelativa;
	}

	/**
	 * Cambia la humedad relativa del galpón.
	 *
	 * @param humedadRelativa humedad en porcentaje
	 */
	public void setHumedadRelativa(Double humedadRelativa) {
		this.humedadRelativa = humedadRelativa;
	}

	/**
	 * Obtiene las observaciones del operario.
	 *
	 * @return observaciones
	 */
	public String getObservaciones() {
		return observaciones;
	}

	/**
	 * Cambia las observaciones del operario.
	 *
	 * @param observaciones observaciones
	 */
	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}

	/**
	 * Obtiene el muestreo de peso.
	 *
	 * @return muestreo, o {@code null} si ese día no hubo
	 */
	public MuestreoPeso getMuestreo() {
		return muestreo;
	}

	/**
	 * Cambia el muestreo de peso.
	 *
	 * @param muestreo muestreo
	 */
	public void setMuestreo(MuestreoPeso muestreo) {
		this.muestreo = muestreo;
	}

	/**
	 * Obtiene las novedades del galpón.
	 *
	 * @return novedades
	 */
	public List<NovedadGalpon> getNovedades() {
		return novedades;
	}

	/**
	 * Cambia las novedades del galpón.
	 *
	 * @param novedades novedades
	 */
	public void setNovedades(List<NovedadGalpon> novedades) {
		this.novedades = novedades;
	}
}
