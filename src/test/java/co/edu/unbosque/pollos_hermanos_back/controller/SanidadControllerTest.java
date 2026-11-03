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
import co.edu.unbosque.pollos_hermanos_back.service.SanidadService;

/**
 * Pruebas de {@link SanidadController}.
 */
@WebMvcTest(SanidadController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class SanidadControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private SanidadService servicio;

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

	/** Una aplicación por alguien sin el cargo responde 409. */
	@Test
	void responsableSinCargoResponde409() throws Exception {
		String mensaje = "El empleado Tomás Transporte no tiene el cargo de encargado de sanidad.";
		when(servicio.registrarAplicacion(any())).thenThrow(rechazo(mensaje));

		mockMvc.perform(post("/sanidad/aplicaciones").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idLote\":1,\"idProducto\":2,\"idEmpleado\":3,\"dosis\":2}")
				.with(authentication(con("sanidad.registrar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}

	/** Un tipo de producto inválido no pasa la validación. */
	@Test
	void tipoInvalidoResponde400() throws Exception {
		mockMvc.perform(post("/sanidad/productos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"X\",\"tipo\":\"ALIMENTO\",\"presentacion\":\"1 L\",\"unidadMedida\":\"ml\",\"idProveedor\":1}")
				.with(authentication(con("sanidad.registrar")))).andExpect(status().isBadRequest());
	}
}
