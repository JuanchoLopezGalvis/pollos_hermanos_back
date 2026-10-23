package co.edu.unbosque.pollos_hermanos_back.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuración de Spring Security: API sin sesión, autenticación con JWT,
 * contraseñas con BCrypt y CORS para el frontend en Angular.
 * <p>
 * Toda ruta fuera de {@link #RUTAS_PUBLICAS} exige un token válido. El permiso
 * concreto de cada operación se declara en el controlador con
 * {@code @PreAuthorize("@permisos.tiene('modulo.accion')")}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/** Rutas que no requieren token: documentación, errores y autenticación. */
	private static final String[] RUTAS_PUBLICAS = { "/auth/**", "/swagger-ui.html", "/swagger-ui/**",
			"/v3/api-docs/**", "/error" };

	/**
	 * Define la cadena de filtros de seguridad.
	 *
	 * @param http               constructor de la configuración HTTP
	 * @param jwtUtil            utilidad para validar los tokens
	 * @param userDetailsService servicio que carga los usuarios
	 * @return la cadena de filtros
	 * @throws Exception si la configuración es inválida
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil,
			UserDetailsService userDetailsService) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable).cors(cors -> {
		}).sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(RUTAS_PUBLICAS).permitAll().anyRequest().authenticated())
				.exceptionHandling(errores -> errores
						.authenticationEntryPoint((request, response, e) -> escribirError(response,
								HttpStatus.UNAUTHORIZED, "Debe iniciar sesión: el token falta, es inválido o expiró."))
						.accessDeniedHandler((request, response, e) -> escribirError(response, HttpStatus.FORBIDDEN,
								"No tiene permisos para realizar esta operación.")))
				.addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Valida usuario y contraseña contra PostgreSQL con BCrypt. Rechaza a los
	 * usuarios inactivos.
	 *
	 * @param userDetailsService servicio que carga los usuarios
	 * @param passwordEncoder    codificador de contraseñas
	 * @return el administrador de autenticación
	 */
	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(userDetailsService);
		proveedor.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(proveedor);
	}

	/**
	 * Codificador de contraseñas con BCrypt.
	 *
	 * @return el codificador
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Orígenes, métodos y encabezados que puede usar el frontend.
	 *
	 * @param origenes orígenes permitidos, separados por comas
	 * @return la configuración de CORS
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") List<String> origenes) {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(origenes);
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

	/**
	 * Escribe una respuesta de error en JSON con el mismo formato de
	 * {@link co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO}.
	 *
	 * @param response respuesta HTTP
	 * @param estado   código de estado
	 * @param mensaje  mensaje en español
	 * @throws IOException si no se puede escribir la respuesta
	 */
	private static void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje)
			throws IOException {
		response.setStatus(estado.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"estado\":" + estado.value() + ",\"mensaje\":\"" + mensaje
				+ "\",\"detalles\":[],\"fecha\":\"" + LocalDateTime.now() + "\"}");
	}
}
