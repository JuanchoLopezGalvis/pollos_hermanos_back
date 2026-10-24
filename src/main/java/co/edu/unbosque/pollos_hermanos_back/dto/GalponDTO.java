package co.edu.unbosque.pollos_hermanos_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import co.edu.unbosque.pollos_hermanos_back.entity.EstadoGalpon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada y salida de un galpón.
 */
public class GalponDTO {

	/** Identificador del galpón. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Long id;

	/** Nombre único del galpón. */
	@NotBlank(message = "es obligatorio")
	@Size(max = 60, message = "admite máximo 60 caracteres")
	private String nombre;

	/** Número máximo de aves. */
	@NotNull(message = "es obligatoria")
	@Positive(message = "debe ser mayor que cero")
	private Integer capacidadMaxima;

	/**
	 * Estado del galpón. Al crear, si no se envía, queda disponible. El estado
	 * ocupado lo asigna la base de datos al registrar un lote.
	 */
	private EstadoGalpon estado;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public GalponDTO() {
	}

	/**
	 * Obtiene el identificador.
	 *
	 * @return identificador
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Cambia el identificador.
	 *
	 * @param id identificador
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Obtiene el nombre.
	 *
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Cambia el nombre.
	 *
	 * @param nombre nombre
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Obtiene la capacidad máxima.
	 *
	 * @return número máximo de aves
	 */
	public Integer getCapacidadMaxima() {
		return capacidadMaxima;
	}

	/**
	 * Cambia la capacidad máxima.
	 *
	 * @param capacidadMaxima número máximo de aves
	 */
	public void setCapacidadMaxima(Integer capacidadMaxima) {
		this.capacidadMaxima = capacidadMaxima;
	}

	/**
	 * Obtiene el estado.
	 *
	 * @return estado
	 */
	public EstadoGalpon getEstado() {
		return estado;
	}

	/**
	 * Cambia el estado.
	 *
	 * @param estado estado
	 */
	public void setEstado(EstadoGalpon estado) {
		this.estado = estado;
	}
}
