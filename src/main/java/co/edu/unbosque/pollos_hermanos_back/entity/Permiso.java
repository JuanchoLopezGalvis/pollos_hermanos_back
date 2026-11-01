package co.edu.unbosque.pollos_hermanos_back.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Permiso del sistema, equivalente a {@code Permission} de Django. Su código
 * sigue la forma {@code modulo.accion}, por ejemplo {@code lote.crear} o
 * {@code pago.registrar}.
 */
@Entity
@Table(name = "permiso")
public class Permiso {

	/** Identificador del permiso. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_permiso")
	private Long id;

	/** Código único del permiso, con la forma {@code modulo.accion}. */
	@Column(name = "codigo", nullable = false, unique = true, length = 80)
	private String codigo;

	/** Descripción legible del permiso. */
	@Column(name = "descripcion", length = 200)
	private String descripcion;

	/** Constructor vacío requerido por JPA y ModelMapper. */
	public Permiso() {
	}

	/**
	 * Crea un permiso con su código y descripción.
	 *
	 * @param codigo      código del permiso
	 * @param descripcion descripción del permiso
	 */
	public Permiso(String codigo, String descripcion) {
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
	 * Obtiene el código del permiso.
	 *
	 * @return código
	 */
	public String getCodigo() {
		return codigo;
	}

	/**
	 * Cambia el código del permiso.
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

	/**
	 * Dos permisos son iguales si tienen el mismo código.
	 *
	 * @param obj objeto a comparar
	 * @return {@code true} si tienen el mismo código
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Permiso otro && Objects.equals(codigo, otro.codigo));
	}

	/**
	 * Código hash basado en el código del permiso.
	 *
	 * @return código hash
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(codigo);
	}
}
