package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.PermisoDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.PermisoService;

/**
 * Pruebas de {@link PermisoController}.
 */
@WebMvcTest(PermisoController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class PermisoControllerTest {

	/** Permiso válido en JSON. */
	private static final String PERMISO = "{\"codigo\":\"lote.crear\",\"descripcion\":\"Registrar lotes\"}";

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de permisos simulado. */
	@MockitoBean
	private PermisoService permisoService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Quien administra roles puede consultar el catálogo. */
	@Test
	void listaPermisosConRolGestionar() throws Exception {
		when(permisoService.obtenerTodos()).thenReturn(List.of(new PermisoDTO("lote.crear", null)));

		mockMvc.perform(get("/permisos")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("rol.gestionar")))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("lote.crear"));
	}

	/** Solo un superusuario crea permisos. */
	@Test
	void soloSuperusuarioCreaPermisos() throws Exception {
		when(permisoService.crear(any())).thenReturn(new PermisoDTO("lote.crear", "Registrar lotes"));

		mockMvc.perform(post("/permisos").contentType(MediaType.APPLICATION_JSON).content(PERMISO)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("rol.gestionar")))))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/permisos").contentType(MediaType.APPLICATION_JSON).content(PERMISO)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.superusuario()))))
				.andExpect(status().isCreated());
	}

	/** Un código sin la forma modulo.accion responde 400. */
	@Test
	void codigoConFormatoInvalidoResponde400() throws Exception {
		mockMvc.perform(post("/permisos").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"Crear Lote\"}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.superusuario()))))
				.andExpect(status().isBadRequest());
	}
}
