package co.edu.unbosque.pollos_hermanos_back.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada y salida de un rol. Los permisos se envían y reciben por su
 * código.
 */
public class RolDTO {

	/** Identificador del rol. */
	private Long id;

	/** Nombre único del rol. */
	@NotBlank(message = "es obligatorio")
	@Size(max = 60, message = "admite máximo 60 caracteres")
	private String nombre;

	/** Descripción del rol. */
	@Size(max = 200, message = "admite máximo 200 caracteres")
	private String descripcion;

	/**
	 * Códigos de los permisos que otorga el rol. Al actualizar, {@code null} deja
	 * los permisos como estaban.
	 */
	private List<String> permisos;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public RolDTO() {
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

	/**
	 * Obtiene los códigos de permiso del rol.
	 *
	 * @return códigos de permiso
	 */
	public List<String> getPermisos() {
		return permisos;
	}

	/**
	 * Cambia los códigos de permiso del rol.
	 *
	 * @param permisos códigos de permiso
	 */
	public void setPermisos(List<String> permisos) {
		this.permisos = permisos;
	}
}
