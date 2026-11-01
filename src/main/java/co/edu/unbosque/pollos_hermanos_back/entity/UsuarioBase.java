package co.edu.unbosque.pollos_hermanos_back.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * Usuario abstracto del sistema, inspirado en {@code AbstractUser} y
 * {@code PermissionsMixin} de Django.
 * <p>
 * Reúne lo que todo usuario necesita para autenticarse y autorizarse: nombre de
 * usuario, correo, contraseña con BCrypt, estado activo, marca de superusuario,
 * fechas de registro y último ingreso, roles (como los <em>groups</em> de
 * Django) y permisos directos (como los <em>user_permissions</em>).
 * <p>
 * Las clases concretas, como {@link Usuario}, solo agregan sus propios datos.
 * Los permisos se consultan con {@link #tienePermiso(String)}, el equivalente a
 * {@code user.has_perm()}.
 */
@MappedSuperclass
public abstract class UsuarioBase implements UserDetails {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/** Autoridad que reciben los superusuarios. */
	public static final String ROL_SUPERUSUARIO = "ROLE_SUPERUSUARIO";

	/** Identificador del usuario. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	/** Nombre de usuario único para iniciar sesión. */
	@Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
	private String nombreUsuario;

	/** Correo único del usuario. */
	@Column(name = "correo", nullable = false, unique = true, length = 120)
	private String correo;

	/** Contraseña cifrada con BCrypt; nunca en texto plano. */
	@Column(name = "contrasena", nullable = false, length = 100)
	private String contrasena;

	/** Nombres del usuario. */
	@Column(name = "nombres", length = 80)
	private String nombres;

	/** Apellidos del usuario. */
	@Column(name = "apellidos", length = 80)
	private String apellidos;

	/**
	 * Indica si el usuario puede iniciar sesión. Un usuario retirado se desactiva
	 * en lugar de borrarse, para conservar su historial.
	 */
	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	/** Indica si el usuario tiene todos los permisos sin asignárselos. */
	@Column(name = "superusuario", nullable = false)
	private boolean superusuario;

	/** Fecha y hora de registro del usuario. */
	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	/** Fecha y hora del último inicio de sesión. */
	@Column(name = "ultimo_ingreso")
	private LocalDateTime ultimoIngreso;

	/** Roles del usuario; cada rol le otorga sus permisos. */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_rol"))
	private Set<Rol> roles = new HashSet<>();

	/** Permisos asignados directamente al usuario, además de los de sus roles. */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "usuario_permiso", joinColumns = @JoinColumn(name = "id_usuario"), inverseJoinColumns = @JoinColumn(name = "id_permiso"))
	private Set<Permiso> permisos = new HashSet<>();

	/** Asigna la fecha de registro antes de guardar el usuario por primera vez. */
	@PrePersist
	protected void alRegistrar() {
		if (fechaRegistro == null) {
			fechaRegistro = LocalDateTime.now();
		}
	}

	/**
	 * Obtiene los códigos de todos los permisos del usuario: los directos y los de
	 * sus roles. Equivale a {@code get_all_permissions()} de Django.
	 *
	 * @return códigos de permiso ordenados alfabéticamente
	 */
	public Set<String> obtenerTodosLosPermisos() {
		Set<String> codigos = new TreeSet<>();
		permisos.forEach(permiso -> codigos.add(permiso.getCodigo()));
		roles.forEach(rol -> rol.getPermisos().forEach(permiso -> codigos.add(permiso.getCodigo())));
		return codigos;
	}

	/**
	 * Indica si el usuario tiene un permiso. Un usuario inactivo no tiene ninguno y
	 * un superusuario los tiene todos. Equivale a {@code has_perm()} de Django.
	 *
	 * @param codigo código del permiso, por ejemplo {@code lote.crear}
	 * @return {@code true} si el usuario tiene el permiso
	 */
	public boolean tienePermiso(String codigo) {
		if (!activo) {
			return false;
		}
		return superusuario || obtenerTodosLosPermisos().contains(codigo);
	}

	/**
	 * Devuelve las autoridades para Spring Security: un {@code GrantedAuthority}
	 * por cada código de permiso y {@link #ROL_SUPERUSUARIO} si es superusuario.
	 *
	 * @return autoridades del usuario
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> autoridades = new ArrayList<>();
		if (superusuario) {
			autoridades.add(new SimpleGrantedAuthority(ROL_SUPERUSUARIO));
		}
		obtenerTodosLosPermisos().forEach(codigo -> autoridades.add(new SimpleGrantedAuthority(codigo)));
		return autoridades;
	}

	/**
	 * Contraseña cifrada que usa Spring Security para autenticar.
	 *
	 * @return contraseña cifrada
	 */
	@Override
	public String getPassword() {
		return contrasena;
	}

	/**
	 * Nombre con el que el usuario inicia sesión.
	 *
	 * @return nombre de usuario
	 */
	@Override
	public String getUsername() {
		return nombreUsuario;
	}

	/**
	 * Un usuario inactivo no puede iniciar sesión.
	 *
	 * @return {@code true} si el usuario está activo
	 */
	@Override
	public boolean isEnabled() {
		return activo;
	}

	/**
	 * Obtiene el nombre completo del usuario. Equivale a {@code get_full_name()}.
	 *
	 * @return nombres y apellidos separados por un espacio
	 */
	public String obtenerNombreCompleto() {
		String completo = ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
		return completo.isEmpty() ? nombreUsuario : completo;
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
	 * Obtiene la contraseña cifrada.
	 *
	 * @return contraseña cifrada
	 */
	public String getContrasena() {
		return contrasena;
	}

	/**
	 * Cambia la contraseña. Debe recibir el valor ya cifrado con BCrypt.
	 *
	 * @param contrasena contraseña cifrada
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
	 * @return {@code true} si está activo
	 */
	public boolean isActivo() {
		return activo;
	}

	/**
	 * Activa o desactiva el usuario.
	 *
	 * @param activo nuevo estado
	 */
	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	/**
	 * Indica si el usuario es superusuario.
	 *
	 * @return {@code true} si es superusuario
	 */
	public boolean isSuperusuario() {
		return superusuario;
	}

	/**
	 * Marca o desmarca al usuario como superusuario.
	 *
	 * @param superusuario nuevo valor
	 */
	public void setSuperusuario(boolean superusuario) {
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
	 * @return fecha del último ingreso, o {@code null} si nunca ingresó
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
	 * Obtiene los roles del usuario.
	 *
	 * @return roles
	 */
	public Set<Rol> getRoles() {
		return roles;
	}

	/**
	 * Cambia los roles del usuario.
	 *
	 * @param roles roles
	 */
	public void setRoles(Set<Rol> roles) {
		this.roles = roles;
	}

	/**
	 * Obtiene los permisos asignados directamente.
	 *
	 * @return permisos directos
	 */
	public Set<Permiso> getPermisos() {
		return permisos;
	}

	/**
	 * Cambia los permisos asignados directamente.
	 *
	 * @param permisos permisos directos
	 */
	public void setPermisos(Set<Permiso> permisos) {
		this.permisos = permisos;
	}
}
