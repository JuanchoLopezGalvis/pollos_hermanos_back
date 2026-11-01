package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import co.edu.unbosque.pollos_hermanos_back.dto.RolDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.RolService;

/**
 * Pruebas de {@link RolController}.
 */
@WebMvcTest(RolController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class RolControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de roles simulado. */
	@MockitoBean
	private RolService rolService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Crear un rol con permiso responde 201. */
	@Test
	void creaRol() throws Exception {
		RolDTO creado = new RolDTO();
		creado.setNombre("Operario");
		creado.setPermisos(List.of("seguimiento.registrar"));
		when(rolService.crear(any())).thenReturn(creado);

		mockMvc.perform(post("/roles").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Operario\",\"permisos\":[\"seguimiento.registrar\"]}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("rol.gestionar")))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.permisos[0]").value("seguimiento.registrar"));
	}

	/** Sin el permiso rol.gestionar responde 403. */
	@Test
	void sinPermisoResponde403() throws Exception {
		mockMvc.perform(get("/roles")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isForbidden());
	}

	/** Asignar un permiso inexistente responde 404 con el mensaje del servicio. */
	@Test
	void permisoInexistenteResponde404() throws Exception {
		when(rolService.actualizar(eq(2L), any()))
				.thenThrow(new RecursoNoEncontradoException("No existen los permisos: pago.inventado."));

		mockMvc.perform(put("/roles/2").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Operario\",\"permisos\":[\"pago.inventado\"]}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("rol.gestionar")))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("No existen los permisos: pago.inventado."));
	}
}
