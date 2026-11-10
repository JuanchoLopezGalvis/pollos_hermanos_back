package co.edu.unbosque.pollos_hermanos_back.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee el token del encabezado {@code Authorization: Bearer <token>} y, si es
 * válido, carga el usuario desde PostgreSQL y autentica la solicitud con sus
 * permisos actuales.
 * <p>
 * Si no hay token, no es válido, el usuario no existe o está inactivo, la
 * solicitud sigue sin autenticación y Spring Security responde 401 en los
 * endpoints protegidos. Se registra desde {@link SecurityConfig} y no como
 * {@code @Component}, para que no se aplique dos veces.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	/** Prefijo del encabezado de autorización. */
	private static final String BEARER = "Bearer ";

	/** Utilidad para validar y leer los tokens. */
	private final JwtUtil jwtUtil;

	/** Servicio que carga el usuario con sus roles y permisos. */
	private final UserDetailsService userDetailsService;

	/**
	 * Crea el filtro.
	 *
	 * @param jwtUtil            utilidad para validar y leer los tokens
	 * @param userDetailsService servicio que carga el usuario
	 */
	public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
	}

	/**
	 * Autentica la solicitud cuando trae un token válido de un usuario activo.
	 *
	 * @param request     solicitud HTTP
	 * @param response    respuesta HTTP
	 * @param filterChain cadena de filtros
	 * @throws ServletException si falla un filtro posterior
	 * @throws IOException      si ocurre un error de entrada o salida
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String encabezado = request.getHeader("Authorization");
		if (encabezado != null && encabezado.startsWith(BEARER)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = encabezado.substring(BEARER.length());
			if (jwtUtil.esValido(token)) {
				autenticar(jwtUtil.extraerUsuario(token), request);
			}
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * Carga el usuario y, si está activo, lo deja autenticado en el contexto de
	 * seguridad.
	 *
	 * @param nombreUsuario nombre de usuario del token
	 * @param request       solicitud HTTP
	 */
	private void autenticar(String nombreUsuario, HttpServletRequest request) {
		try {
			UserDetails usuario = userDetailsService.loadUserByUsername(nombreUsuario);
			if (usuario.isEnabled()) {
				UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(usuario,
						null, usuario.getAuthorities());
				autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(autenticacion);
			}
		} catch (UsernameNotFoundException e) {
			// El usuario del token ya no existe: la solicitud sigue sin autenticación.
		}
	}
}
