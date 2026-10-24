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
import co.edu.unbosque.pollos_hermanos_back.service.GastoBeneficioService;

/**
 * Pruebas de {@link GastoBeneficioController}.
 */
@WebMvcTest(GastoBeneficioController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class GastoBeneficioControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private GastoBeneficioService servicio;

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

	/** Una jornada sin beneficiadoras no pasa la validación. */
	@Test
	void jornadaSinDetalleResponde400() throws Exception {
		mockMvc.perform(post("/beneficio/jornadas").contentType(MediaType.APPLICATION_JSON).content("{\"idLote\":1,\"detalle\":[]}")
				.with(authentication(con("beneficio.registrar")))).andExpect(status().isBadRequest());
	}

	/** El balance del lote requiere costo.ver. */
	@Test
	void balanceRequiereCostoVer() throws Exception {
		when(servicio.obtenerRentabilidad()).thenReturn(List.of());
		mockMvc.perform(get("/costos/lotes").with(authentication(con("costo.ver")))).andExpect(status().isOk());
		mockMvc.perform(get("/costos/lotes").with(authentication(con("gasto.registrar")))).andExpect(status().isForbidden());
	}

	/** Registrar otro ingreso responde 201. */
	@Test
	void registraOtroIngreso() throws Exception {
		when(servicio.registrarOtroIngreso(any())).thenReturn(
				new OtroIngresoDTO(1L, LocalDate.of(2026, 10, 20), 3L, "L-1", new BigDecimal("300000"), "Gallinaza"));

		mockMvc.perform(post("/ingresos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idLote\":3,\"monto\":300000,\"descripcion\":\"Gallinaza\"}")
				.with(authentication(con("venta.gestionar")))).andExpect(status().isCreated());
	}
}
