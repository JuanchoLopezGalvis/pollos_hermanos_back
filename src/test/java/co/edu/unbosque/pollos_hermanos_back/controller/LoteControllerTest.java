package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.LotesDePrueba;
import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadRegistradaDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.LoteService;

/**
 * Pruebas de {@link LoteController}.
 */
@WebMvcTest(LoteController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class LoteControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de lotes simulado. */
	@MockitoBean
	private LoteService loteService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Registrar un lote con permiso responde 201. */
	@Test
	void registraLote() throws Exception {
		when(loteService.crear(any())).thenReturn(LotesDePrueba.lote(7L, EstadoLote.EN_ENGORDE));

		mockMvc.perform(post("/lotes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"L-2026-01\",\"idGalpon\":1,\"fechaIngreso\":\"2026-10-01\",\"cantidadInicial\":4000,\"costoPollito\":2600}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("lote.gestionar")))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("EN_ENGORDE"));
	}

	/** Un código con espacios no pasa la validación de formato. */
	@Test
	void codigoInvalidoResponde400() throws Exception {
		mockMvc.perform(post("/lotes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"lote 1\",\"idGalpon\":1,\"fechaIngreso\":\"2026-10-01\",\"cantidadInicial\":4000,\"costoPollito\":2600}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("lote.gestionar")))))
				.andExpect(status().isBadRequest());
	}

	/** Una transición rechazada por el procedimiento responde 409 con el mensaje de PostgreSQL. */
	@Test
	void transicionRechazadaResponde409() throws Exception {
		String mensaje = "El lote L-2026-01 no puede pasar de EN_ENGORDE a VENDIDO.";
		when(loteService.cambiarEstado(eq(7L), any())).thenThrow(
				new DataIntegrityViolationException("rechazo", new SQLException("ERROR: " + mensaje, "P0001")));

		mockMvc.perform(patch("/lotes/7/estado").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VENDIDO\"}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("lote.gestionar")))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}

	/** Un operario con mortalidad.registrar registra la mortalidad, pero no lotes. */
	@Test
	void operarioRegistraMortalidadPeroNoLotes() throws Exception {
		when(loteService.registrarMortalidad(eq(7L), any()))
				.thenReturn(new MortalidadRegistradaDTO(3L, new BigDecimal("5.75"), true));

		mockMvc.perform(post("/lotes/7/mortalidad").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":80}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("mortalidad.registrar")))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.alerta").value(true));
		mockMvc.perform(post("/lotes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"L-2026-02\",\"idGalpon\":1,\"fechaIngreso\":\"2026-10-01\",\"cantidadInicial\":4000,\"costoPollito\":2600}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("mortalidad.registrar")))))
				.andExpect(status().isForbidden());
	}
}
