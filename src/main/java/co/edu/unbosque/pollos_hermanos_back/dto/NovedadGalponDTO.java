package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de una novedad del galpón dentro del seguimiento diario.
 */
public class NovedadGalponDTO {

	/** Tipo de novedad, por ejemplo "Cama húmeda". */
	@NotBlank(message = "es obligatorio")
	@Size(max = 60, message = "admite máximo 60 caracteres")
	private String tipo;

	/** Descripción de lo observado. */
	@Size(max = 500, message = "admite máximo 500 caracteres")
	private String descripcion;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public NovedadGalponDTO() {
	}

	/**
	 * Obtiene el tipo de novedad.
	 *
	 * @return tipo
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Cambia el tipo de novedad.
	 *
	 * @param tipo tipo
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Obtiene la descripción.
	 *
	 * @return descripción
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Cambia la descripción.
	 *
	 * @param descripcion descripción
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
}
