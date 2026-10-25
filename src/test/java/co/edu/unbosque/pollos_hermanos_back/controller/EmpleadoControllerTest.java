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
import co.edu.unbosque.pollos_hermanos_back.service.EmpleadoService;

/**
 * Pruebas de {@link EmpleadoController}.
 */
@WebMvcTest(EmpleadoController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class EmpleadoControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private EmpleadoService servicio;

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

	/** Registrar un empleado con permiso responde 201. */
	@Test
	void registraEmpleado() throws Exception {
		when(servicio.crear(any())).thenReturn(new EmpleadoDTO(4L, "101", "Berta", "Uno", null, 5L, "BENEFICIADORA",
				"Beneficiadora", LocalDate.of(2026, 1, 1), null, true));

		mockMvc.perform(post("/empleados").contentType(MediaType.APPLICATION_JSON)
				.content("{\"documento\":\"101\",\"nombres\":\"Berta\",\"apellidos\":\"Uno\",\"idCargo\":5,\"fechaIngreso\":\"2026-01-01\"}")
				.with(authentication(con("personal.gestionar"))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.codigoCargo").value("BENEFICIADORA"));
	}

	/** Retirar dos veces muestra el mensaje del procedimiento. */
	@Test
	void retiroRepetidoResponde409() throws Exception {
		when(servicio.retirar(eq(4L), any())).thenThrow(rechazo("El empleado ya está retirado."));

		mockMvc.perform(patch("/empleados/4/retiro").contentType(MediaType.APPLICATION_JSON).content("{}")
				.with(authentication(con("personal.gestionar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("El empleado ya está retirado."));
	}

	/** Quien registra beneficio puede consultar empleados, pero no crearlos. */
	@Test
	void beneficioConsultaPeroNoCrea() throws Exception {
		when(servicio.obtener(true, "BENEFICIADORA")).thenReturn(List.of());
		mockMvc.perform(get("/empleados?activos=true&cargo=BENEFICIADORA").with(authentication(con("beneficio.registrar"))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/empleados").contentType(MediaType.APPLICATION_JSON)
				.content("{\"documento\":\"101\",\"nombres\":\"Berta\",\"apellidos\":\"Uno\",\"idCargo\":5,\"fechaIngreso\":\"2026-01-01\"}")
				.with(authentication(con("beneficio.registrar")))).andExpect(status().isForbidden());
	}
}
