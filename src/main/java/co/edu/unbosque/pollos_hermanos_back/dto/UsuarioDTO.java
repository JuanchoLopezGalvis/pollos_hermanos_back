package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada y salida de un usuario.
 * <p>
 * La contraseña solo se recibe, nunca se devuelve. Al actualizar, los campos en
 * {@code null} se dejan como estaban.
 */
public class UsuarioDTO {

	/** Identificador del usuario. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Long id;

	/** Nombre de usuario para iniciar sesión. */
	@NotBlank(groups = AlCrear.class, message = "es obligatorio")
	@Size(min = 3, max = 50, message = "debe tener entre 3 y 50 caracteres")
	private String nombreUsuario;

	/** Correo del usuario. */
	@NotBlank(groups = AlCrear.class, message = "es obligatorio")
	@Email(message = "no tiene un formato válido")
	@Size(max = 120, message = "admite máximo 120 caracteres")
	private String correo;

	/** Contraseña en texto plano; se cifra con BCrypt antes de guardarla. */
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@NotBlank(groups = AlCrear.class, message = "es obligatoria")
	@Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
	private String contrasena;

	/** Nombres del usuario. */
	@Size(max = 80, message = "admite máximo 80 caracteres")
	private String nombres;

	/** Apellidos del usuario. */
	@Size(max = 80, message = "admite máximo 80 caracteres")
	private String apellidos;

	/** Indica si el usuario puede iniciar sesión. */
	private Boolean activo;

	/** Indica si el usuario tiene todos los permisos. */
	private Boolean superusuario;

	/** Fecha de registro. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime fechaRegistro;

	/** Fecha del último inicio de sesión. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime ultimoIngreso;

	/** Nombres de los roles del usuario. */
	private List<String> roles;

	/** Códigos de los permisos asignados directamente al usuario. */
	private List<String> permisos;

	/** Todos los permisos del usuario: los directos y los de sus roles. */
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Set<String> permisosEfectivos;

	/** Constructor vacío requerido por Jackson y ModelMapper. */
	public UsuarioDTO() {
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
	 * Obtiene el nombre de usuario.
	 *
	 * @return nombre de usuario
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Cambia el nombre de usuario.
	 *
	 * @param nombreUsuario nombre de usuario
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	/**
	 * Obtiene el correo.
	 *
	 * @return correo
	 */
	public String getCorreo() {
		return correo;
	}

	/**
	 * Cambia el correo.
	 *
	 * @param correo correo
	 */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/**
	 * Obtiene la contraseña en texto plano recibida.
	 *
	 * @return contraseña
	 */
	public String getContrasena() {
		return contrasena;
	}

	/**
	 * Cambia la contraseña en texto plano.
	 *
	 * @param contrasena contraseña
	 */
	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	/**
	 * Obtiene los nombres.
	 *
	 * @return nombres
	 */
	public String getNombres() {
		return nombres;
	}

	/**
	 * Cambia los nombres.
	 *
	 * @param nombres nombres
	 */
	public void setNombres(String nombres) {
		this.nombres = nombres;
	}

	/**
	 * Obtiene los apellidos.
	 *
	 * @return apellidos
	 */
	public String getApellidos() {
		return apellidos;
	}

	/**
	 * Cambia los apellidos.
	 *
	 * @param apellidos apellidos
	 */
	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	/**
	 * Indica si el usuario está activo.
	 *
	 * @return estado activo
	 */
	public Boolean getActivo() {
		return activo;
	}

	/**
	 * Cambia el estado activo.
	 *
	 * @param activo estado activo
	 */
	public void setActivo(Boolean activo) {
		this.activo = activo;
	}

	/**
	 * Indica si el usuario es superusuario.
	 *
	 * @return marca de superusuario
	 */
	public Boolean getSuperusuario() {
		return superusuario;
	}

	/**
	 * Cambia la marca de superusuario.
	 *
	 * @param superusuario marca de superusuario
	 */
	public void setSuperusuario(Boolean superusuario) {
		this.superusuario = superusuario;
	}

	/**
	 * Obtiene la fecha de registro.
	 *
	 * @return fecha de registro
	 */
	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	/**
	 * Cambia la fecha de registro.
	 *
	 * @param fechaRegistro fecha de registro
	 */
	public void setFechaRegistro(LocalDateTime fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	/**
	 * Obtiene la fecha del último ingreso.
	 *
	 * @return fecha del último ingreso
	 */
	public LocalDateTime getUltimoIngreso() {
		return ultimoIngreso;
	}

	/**
	 * Cambia la fecha del último ingreso.
	 *
	 * @param ultimoIngreso fecha del último ingreso
	 */
	public void setUltimoIngreso(LocalDateTime ultimoIngreso) {
		this.ultimoIngreso = ultimoIngreso;
	}

	/**
	 * Obtiene los nombres de los roles.
	 *
	 * @return nombres de los roles
	 */
	public List<String> getRoles() {
		return roles;
	}

	/**
	 * Cambia los nombres de los roles.
	 *
	 * @param roles nombres de los roles
	 */
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	/**
	 * Obtiene los códigos de los permisos directos.
	 *
	 * @return códigos de permiso
	 */
	public List<String> getPermisos() {
		return permisos;
	}

	/**
	 * Cambia los códigos de los permisos directos.
	 *
	 * @param permisos códigos de permiso
	 */
	public void setPermisos(List<String> permisos) {
		this.permisos = permisos;
	}

	/**
	 * Obtiene todos los permisos del usuario.
	 *
	 * @return permisos efectivos
	 */
	public Set<String> getPermisosEfectivos() {
		return permisosEfectivos;
	}

	/**
	 * Cambia los permisos efectivos.
	 *
	 * @param permisosEfectivos permisos efectivos
	 */
	public void setPermisosEfectivos(Set<String> permisosEfectivos) {
		this.permisosEfectivos = permisosEfectivos;
	}
}
