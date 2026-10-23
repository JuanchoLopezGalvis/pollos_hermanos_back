package co.edu.unbosque.pollos_hermanos_back.security;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	/**
	 * Crea el servicio.
	 *
	 * @param authenticationManager valida las credenciales
	 * @param jwtUtil               genera el token
	 * @param usuarioRepository     repositorio de usuarios
	 */
	public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioRepository usuarioRepository) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioRepository = usuarioRepository;
	}

	/**
	 * Valida las credenciales, registra el último ingreso y genera el token.
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
		usuario.setUltimoIngreso(LocalDateTime.now());
		usuarioRepository.save(usuario);
		return new TokenDTO(jwtUtil.generarToken(usuario.getNombreUsuario()), "Bearer", usuario.getNombreUsuario(),
				usuario.obtenerNombreCompleto(), usuario.isSuperusuario(), usuario.obtenerTodosLosPermisos());
	}
}
