package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CuentaClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCuentaClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.RolRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;
import co.edu.unbosque.pollos_hermanos_back.security.AuthService;

/**
 * Cuentas de los clientes para el portal (D21, D25).
 * <p>
 * Un cliente puede crear su cuenta él mismo o pedírsela a la finca. En los dos
 * casos el usuario recibe solo el rol {@value #ROL_CLIENTE}, que no da acceso a
 * nada fuera del portal. El usuario y el cliente se guardan en una sola
 * transacción: si falla uno, no queda ninguno.
 */
@Service
public class CuentaClienteService {

	/** Rol que reciben las cuentas de cliente; lo crea {@code modulo_08_portal_clientes.sql}. */
	static final String ROL_CLIENTE = "Cliente";

	/** Longitud máxima de {@code usuario.nombres}. */
	private static final int MAX_NOMBRES = 80;

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarioRepository;

	/** Repositorio de roles. */
	private final RolRepository rolRepository;

	/** Repositorio de clientes. */
	private final ClienteRepository clienteRepository;

	/** Codificador de contraseñas con BCrypt. */
	private final PasswordEncoder passwordEncoder;

	/** Emite el token de la sesión. */
	private final AuthService authService;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarioRepository repositorio de usuarios
	 * @param rolRepository     repositorio de roles
	 * @param clienteRepository repositorio de clientes
	 * @param passwordEncoder   codificador de contraseñas
	 * @param authService       servicio de autenticación
	 */
	public CuentaClienteService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
			ClienteRepository clienteRepository, PasswordEncoder passwordEncoder, AuthService authService) {
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.clienteRepository = clienteRepository;
		this.passwordEncoder = passwordEncoder;
		this.authService = authService;
	}

	/**
	 * Registra a un cliente nuevo con su cuenta y le abre la sesión. Si el
	 * documento ya pertenece a un cliente, no se vincula (D25): la finca le crea
	 * la cuenta, para que nadie se apropie de la ficha de otro.
	 *
	 * @param dto datos del cliente y de la cuenta
	 * @return la sesión del cliente recién registrado
	 * @throws ReglaNegocioException si el documento, el usuario o el correo ya existen
	 */
	@Transactional
	public TokenDTO registrar(RegistroCuentaClienteDTO dto) {
		if (clienteRepository.existeDocumento(dto.documento())) {
			throw new ReglaNegocioException("Ya estás registrado como cliente con el documento " + dto.documento().trim()
					+ ". Pide a la finca que te cree la cuenta.");
		}
		Usuario usuario = crearUsuario(dto.nombreUsuario(), dto.correo(), dto.contrasena(), dto.nombre());
		clienteRepository.crearConUsuario(dto, usuario.getId());
		return authService.emitirToken(usuario);
	}

	/**
	 * Crea la cuenta de un cliente que la finca ya tenía registrado.
	 *
	 * @param idCliente identificador del cliente
	 * @param dto       usuario, contraseña inicial y, si el cliente no lo tiene, correo
	 * @return el cliente con su cuenta
	 * @throws RecursoNoEncontradoException si el cliente no existe
	 * @throws ReglaNegocioException        si ya tiene cuenta, no hay correo o el usuario ya existe
	 */
	@Transactional
	public ClienteDTO crearCuenta(Long idCliente, CuentaClienteDTO dto) {
		ClienteDTO cliente = clienteRepository.buscar(idCliente)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con id " + idCliente + "."));
		if (cliente.idUsuario() != null) {
			throw new ReglaNegocioException("El cliente " + cliente.nombre() + " ya tiene la cuenta '" + cliente.usuario() + "'.");
		}
		String correo = dto.correo() == null || dto.correo().isBlank() ? cliente.correo() : dto.correo();
		if (correo == null || correo.isBlank()) {
			throw new ReglaNegocioException("El cliente " + cliente.nombre() + " no tiene correo: indica uno para su cuenta.");
		}
		Usuario usuario = crearUsuario(dto.nombreUsuario(), correo, dto.contrasena(), cliente.nombre());
		if (!clienteRepository.vincularUsuario(idCliente, usuario.getId())) {
			throw new ReglaNegocioException("El cliente " + cliente.nombre() + " ya tiene una cuenta.");
		}
		return clienteRepository.buscar(idCliente).orElseThrow();
	}

	/**
	 * Crea el usuario con el rol Cliente y lo escribe de inmediato, para que el
	 * cliente pueda referenciarlo en la misma transacción.
	 *
	 * @param nombreUsuario nombre de usuario
	 * @param correo        correo
	 * @param contrasena    contraseña en texto plano
	 * @param nombre        nombre del cliente
	 * @return el usuario guardado
	 */
	private Usuario crearUsuario(String nombreUsuario, String correo, String contrasena, String nombre) {
		String usuarioLimpio = nombreUsuario.trim();
		String correoLimpio = correo.trim();
		if (usuarioRepository.existsByNombreUsuario(usuarioLimpio)) {
			throw new ReglaNegocioException("El nombre de usuario '" + usuarioLimpio + "' ya está en uso. Elige otro.");
		}
		if (usuarioRepository.existsByCorreo(correoLimpio)) {
			throw new ReglaNegocioException("Ya existe una cuenta con el correo " + correoLimpio + ".");
		}
		Rol rol = rolRepository.findByNombre(ROL_CLIENTE).orElseThrow(() -> new ReglaNegocioException(
				"No existe el rol " + ROL_CLIENTE + ": falta instalar modulo_08_portal_clientes.sql."));

		Usuario usuario = new Usuario();
		usuario.setNombreUsuario(usuarioLimpio);
		usuario.setCorreo(correoLimpio);
		usuario.setContrasena(passwordEncoder.encode(contrasena));
		String nombres = nombre.trim();
		usuario.setNombres(nombres.length() > MAX_NOMBRES ? nombres.substring(0, MAX_NOMBRES) : nombres);
		usuario.setActivo(true);
		usuario.setRoles(new HashSet<>(Set.of(rol)));
		return usuarioRepository.saveAndFlush(usuario);
	}
}
