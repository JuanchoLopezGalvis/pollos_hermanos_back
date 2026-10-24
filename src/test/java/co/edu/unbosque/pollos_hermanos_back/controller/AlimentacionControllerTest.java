package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.*;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.AlimentacionService;

/**
 * Pruebas de {@link AlimentacionController}.
 */
@WebMvcTest(AlimentacionController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class AlimentacionControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private AlimentacionService servicio;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/**
	 * Autenticación con los permisos indicados.
	 *
	 * @param permisos códigos de permiso
	 * @return autenticación de prueba
	 */
	private static Authentication con(String... permisos) {
		return UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos(permisos));
	}

	/**
	 * Error de PostgreSQL lanzado por un {@code RAISE EXCEPTION}.
	 *
	 * @param mensaje mensaje del procedimiento o trigger
	 * @return excepción como la traduce Spring
	 */
	private static DataIntegrityViolationException rechazo(String mensaje) {
		return new DataIntegrityViolationException("rechazo", new SQLException("ERROR: " + mensaje, "P0001"));
	}

	/** Un consumo de la etapa equivocada responde 409 con el mensaje del trigger. */
	@Test
	void etapaEquivocadaResponde409() throws Exception {
		String mensaje = "El lote L-1 tiene 11 días ese día y está en la etapa de iniciación: el alimento \"Finalizador\" no corresponde.";
		when(servicio.registrarConsumo(any())).thenThrow(rechazo(mensaje));

		mockMvc.perform(post("/alimentacion/consumos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idLote\":1,\"idTipoAlimento\":4,\"cantidadBultos\":2}")
				.with(authentication(con("alimentacion.registrar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}

	/** Con solo alimentacion.ver no se registran compras. */
	@Test
	void verNoRegistra() throws Exception {
		when(servicio.obtenerInventario()).thenReturn(List.of());
		mockMvc.perform(get("/alimentacion/inventario").with(authentication(con("alimentacion.ver")))).andExpect(status().isOk());
		mockMvc.perform(post("/alimentacion/compras").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idProveedor\":1,\"idTipoAlimento\":1,\"cantidadBultos\":10,\"costoUnitario\":100000}")
				.with(authentication(con("alimentacion.ver")))).andExpect(status().isForbidden());
	}
}
