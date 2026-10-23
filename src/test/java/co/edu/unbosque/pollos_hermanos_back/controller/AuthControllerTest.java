package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.security.AuthService;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;

/**
 * Pruebas de {@link AuthController}.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class AuthControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de autenticación simulado. */
	@MockitoBean
	private AuthService authService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** El login es público y devuelve el token. */
	@Test
	void iniciarSesionDevuelveToken() throws Exception {
		when(authService.iniciarSesion(any())).thenReturn(
				new TokenDTO("token.jwt", "Bearer", "operario", "Operario", false, Set.of("seguimiento.ver")));

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"operario\",\"contrasena\":\"Clave123*\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("token.jwt"))
				.andExpect(jsonPath("$.permisos[0]").value("seguimiento.ver"));
	}

	/** Con credenciales incorrectas responde 401 en español. */
	@Test
	void credencialesIncorrectasResponden401() throws Exception {
		when(authService.iniciarSesion(any())).thenThrow(new BadCredentialsException("mal"));

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"usuario\":\"operario\",\"contrasena\":\"mala\"}")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
	}

	/** Sin contraseña responde 400 con el campo inválido. */
	@Test
	void faltaContrasenaResponde400() throws Exception {
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"operario\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles[0]").value("contrasena: es obligatoria"));
	}
}
