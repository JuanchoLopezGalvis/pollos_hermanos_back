package co.edu.unbosque.pollos_hermanos_back.security;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.configuration.UsuarioAuditoria;
import co.edu.unbosque.pollos_hermanos_back.dto.LoginDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Autentica a los usuarios y genera su token JWT.
 */
@Service
public class AuthService {

	/** Valida usuario y contraseña con BCrypt. */
	private final AuthenticationManager authenticationManager;

	/** Utilidad para generar el token. */
	private final JwtUtil jwtUtil;

	/** Repositorio de usuarios, para registrar el último ingreso. */
	private final UsuarioRepository usuarioRepository;

	/** Deja al usuario como autor del último ingreso en la auditoría. */
	private final UsuarioAuditoria usuarioAuditoria;

	/**
	 * Crea el servicio.
	 *
	 * @param authenticationManager valida las credenciales
	 * @param jwtUtil               genera el token
	 * @param usuarioRepository     repositorio de usuarios
	 * @param usuarioAuditoria      autor de los cambios en la auditoría
	 */
	public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioRepository usuarioRepository, UsuarioAuditoria usuarioAuditoria) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioRepository = usuarioRepository;
		this.usuarioAuditoria = usuarioAuditoria;
	}

	/**
	 * Valida las credenciales, registra el último ingreso y genera el token. En
	 * la auditoría, el último ingreso queda a nombre del usuario que entró.
	 *
	 * @param login nombre de usuario o correo, y contraseña
	 * @return el token y los permisos del usuario
	 * @throws org.springframework.security.core.AuthenticationException si las
	 *         credenciales son incorrectas o el usuario está inactivo
	 */
	@Transactional
	public TokenDTO iniciarSesion(LoginDTO login) {
		Authentication autenticacion = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(login.usuario(), login.contrasena()));
		Usuario usuario = (Usuario) autenticacion.getPrincipal();
		usuarioAuditoria.fijarEnTransaccion(usuario.getNombreUsuario());
		usuario.setUltimoIngreso(LocalDateTime.now());
		usuarioRepository.save(usuario);
		return emitirToken(usuario);
	}

	/**
	 * Genera el token y la respuesta de sesión de un usuario ya autenticado,
	 * por ejemplo el cliente que acaba de crear su cuenta.
	 *
	 * @param usuario usuario con sus roles cargados
	 * @return token, datos del usuario y sus permisos
	 */
	public TokenDTO emitirToken(Usuario usuario) {
		return new TokenDTO(jwtUtil.generarToken(usuario.getNombreUsuario()), "Bearer", usuario.getNombreUsuario(),
				usuario.obtenerNombreCompleto(), usuario.isSuperusuario(), usuario.obtenerTodosLosPermisos());
	}
}
