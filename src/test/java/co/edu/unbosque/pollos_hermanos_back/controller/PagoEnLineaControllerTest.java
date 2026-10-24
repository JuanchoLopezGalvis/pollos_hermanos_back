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
import co.edu.unbosque.pollos_hermanos_back.service.PagoEnLineaService;

/**
 * Pruebas de {@link PagoEnLineaController}.
 */
@WebMvcTest(PagoEnLineaController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class PagoEnLineaControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private PagoEnLineaService servicio;

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

	/** Iniciar un pago devuelve la URL del checkout. */
	@Test
	void iniciaPago() throws Exception {
		when(servicio.iniciar(eq(7L), any())).thenReturn(new CheckoutPagoDTO("https://checkout.wompi.co/p/?x", "PH-V7-1", 100L, "COP"));

		mockMvc.perform(post("/pagos-en-linea/ventas/7").contentType(MediaType.APPLICATION_JSON)
				.content("{\"monto\":1,\"urlRetorno\":\"http://localhost/pago-resultado\"}")
				.with(authentication(con("pago.registrar"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.referencia").value("PH-V7-1"));
	}

	/** Una URL de retorno que no es http no pasa la validación. */
	@Test
	void urlRetornoInvalidaResponde400() throws Exception {
		mockMvc.perform(post("/pagos-en-linea/ventas/7").contentType(MediaType.APPLICATION_JSON)
				.content("{\"monto\":1,\"urlRetorno\":\"javascript:alert(1)\"}")
				.with(authentication(con("pago.registrar")))).andExpect(status().isBadRequest());
	}

	/** Sin pago.registrar no se confirma un pago. */
	@Test
	void sinPermisoResponde403() throws Exception {
		mockMvc.perform(post("/pagos-en-linea/confirmar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idTransaccion\":\"tx-1\"}").with(authentication(con("cartera.ver"))))
				.andExpect(status().isForbidden());
	}
}
