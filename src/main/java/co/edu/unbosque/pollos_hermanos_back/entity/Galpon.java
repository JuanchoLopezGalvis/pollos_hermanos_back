package co.edu.unbosque.pollos_hermanos_back.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Galpón de la finca. Aloja varios lotes a la vez (uno nuevo cada semana) que
 * comparten su capacidad.
 * <p>
 * Las reglas (ocupado solo con lotes activos, capacidad no menor que las aves
 * de sus lotes activos) las valida el trigger {@code trg_galpon_validar} de
 * PostgreSQL.
 */
@Entity
@Table(name = "galpon")
public class Galpon {

	/** Identificador del galpón. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_galpon")
	private Long id;

	/** Nombre único del galpón. */
	@Column(name = "nombre", nullable = false, unique = true, length = 60)
	private String nombre;

	/** Número máximo de aves que puede alojar. */
	@Column(name = "capacidad_maxima", nullable = false)
	private Integer capacidadMaxima;

	/** Estado actual del galpón. */
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "estado", nullable = false, columnDefinition = "estado_galpon")
	private EstadoGalpon estado = EstadoGalpon.DISPONIBLE;

	/** Fecha y hora de creación del registro. */
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	/** Constructor vacío requerido por JPA y ModelMapper. */
	public Galpon() {
	}

	/**
	 * Crea un galpón disponible.
	 *
	 * @param nombre          nombre del galpón
	 * @param capacidadMaxima número máximo de aves
	 */
	public Galpon(String nombre, Integer capacidadMaxima) {
		this.nombre = nombre;
		this.capacidadMaxima = capacidadMaxima;
	}

	/** Asigna la fecha de creación antes de insertar. */
	@PrePersist
	void alCrear() {
		if (fechaCreacion == null) {
			fechaCreacion = LocalDateTime.now();
		}
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

	/**
	 * Obtiene la fecha de creación.
	 *
	 * @return fecha y hora de creación
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Cambia la fecha de creación.
	 *
	 * @param fechaCreacion fecha y hora de creación
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}
}
