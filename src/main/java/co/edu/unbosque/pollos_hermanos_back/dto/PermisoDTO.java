package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada y salida de un permiso.
 */
public class PermisoDTO {

	/** Identificador del permiso. */
	private Long id;

	/** Código del permiso, con la forma {@code modulo.accion}. */
	@NotBlank(message = "es obligatorio")
	@Size(max = 80, message = "admite máximo 80 caracteres")
	@Pattern(regexp = "^[a-z_]+\\.[a-z_]+$", message = "debe tener la forma modulo.accion, en minúsculas")
	private String codigo;

	/** Descripción del permiso. */
	@Size(max = 200, message = "admite máximo 200 caracteres")
	private String descripcion;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public PermisoDTO() {
	}

	/**
	 * Crea el DTO con su código y descripción.
	 *
	 * @param codigo      código del permiso
	 * @param descripcion descripción del permiso
	 */
	public PermisoDTO(String codigo, String descripcion) {
		this.codigo = codigo;
		this.descripcion = descripcion;
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
	 * Obtiene el código.
	 *
	 * @return código
	 */
	public String getCodigo() {
		return codigo;
	}

	/**
	 * Cambia el código.
	 *
	 * @param codigo código
	 */
	public void setCodigo(String codigo) {
		this.codigo = codigo;
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
