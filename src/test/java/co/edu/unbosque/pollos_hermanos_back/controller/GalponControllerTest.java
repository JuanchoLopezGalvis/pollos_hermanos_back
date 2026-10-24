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

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.GalponDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.GalponService;

/**
 * Pruebas de {@link GalponController}.
 */
@WebMvcTest(GalponController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class GalponControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de galpones simulado. */
	@MockitoBean
	private GalponService galponService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Crear un galpón con permiso responde 201. */
	@Test
	void creaGalpon() throws Exception {
		GalponDTO creado = new GalponDTO();
		creado.setNombre("Galpón 1");
		creado.setCapacidadMaxima(5000);
		when(galponService.crear(any())).thenReturn(creado);

		mockMvc.perform(post("/galpones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Galpón 1\",\"capacidadMaxima\":5000}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("galpon.gestionar")))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.capacidadMaxima").value(5000));
	}

	/** Una capacidad en cero no llega al servicio: responde 400. */
	@Test
	void capacidadCeroResponde400() throws Exception {
		mockMvc.perform(post("/galpones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Galpón 1\",\"capacidadMaxima\":0}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("galpon.gestionar")))))
				.andExpect(status().isBadRequest());
	}

	/** Consultar con solo galpon.ver funciona; crear no. */
	@Test
	void verNoPermiteCrear() throws Exception {
		when(galponService.obtenerTodos()).thenReturn(List.of());
		mockMvc.perform(get("/galpones")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("galpon.ver")))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/galpones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Galpón 1\",\"capacidadMaxima\":5000}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("galpon.ver")))))
				.andExpect(status().isForbidden());
	}

	/** Si el trigger de PostgreSQL rechaza el cambio, responde 409 con su mensaje. */
	@Test
	void rechazoDelTriggerResponde409() throws Exception {
		String mensaje = "La capacidad del galpón \"Galpón 1\" (700) no puede ser menor que las aves de su lote activo (800).";
		when(galponService.actualizar(eq(1L), any())).thenThrow(new DataIntegrityViolationException("rechazo",
				new SQLException("ERROR: " + mensaje + "\n  Where: PL/pgSQL function", "P0001")));

		mockMvc.perform(put("/galpones/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Galpón 1\",\"capacidadMaxima\":700}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("galpon.gestionar")))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}
}
