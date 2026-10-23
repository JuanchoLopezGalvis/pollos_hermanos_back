package co.edu.unbosque.pollos_hermanos_back.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Rol del sistema, equivalente a {@code Group} de Django: agrupa permisos para
 * asignarlos juntos a varios usuarios (por ejemplo, "Administrador de la finca"
 * u "Operario").
 * <p>
 * No confundir con el cargo de un empleado (encargado de sanidad,
 * transportista), que es un dato del negocio y vive en el modelo de personal.
 */
@Entity
@Table(name = "rol")
public class Rol {

	/** Identificador del rol. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rol")
	private Long id;

	/** Nombre único del rol. */
	@Column(name = "nombre", nullable = false, unique = true, length = 60)
	private String nombre;

	/** Descripción del rol. */
	@Column(name = "descripcion", length = 200)
	private String descripcion;

	/** Permisos que otorga el rol. */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "id_rol"), inverseJoinColumns = @JoinColumn(name = "id_permiso"))
	private Set<Permiso> permisos = new HashSet<>();

	/** Constructor vacío requerido por JPA y ModelMapper. */
	public Rol() {
	}

	/**
	 * Crea un rol con su nombre y descripción.
	 *
	 * @param nombre      nombre del rol
	 * @param descripcion descripción del rol
	 */
	public Rol(String nombre, String descripcion) {
		this.nombre = nombre;
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
	 * Obtiene el nombre del rol.
	 *
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Cambia el nombre del rol.
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
	 * Obtiene los permisos del rol.
	 *
	 * @return permisos
	 */
	public Set<Permiso> getPermisos() {
		return permisos;
	}

	/**
	 * Cambia los permisos del rol.
	 *
	 * @param permisos permisos
	 */
	public void setPermisos(Set<Permiso> permisos) {
		this.permisos = permisos;
	}

	/**
	 * Dos roles son iguales si tienen el mismo nombre.
	 *
	 * @param obj objeto a comparar
	 * @return {@code true} si tienen el mismo nombre
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Rol otro && Objects.equals(nombre, otro.nombre));
	}

	/**
	 * Código hash basado en el nombre del rol.
	 *
	 * @return código hash
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(nombre);
	}
}
