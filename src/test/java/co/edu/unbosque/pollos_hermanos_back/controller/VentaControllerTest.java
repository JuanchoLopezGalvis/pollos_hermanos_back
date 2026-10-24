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
import co.edu.unbosque.pollos_hermanos_back.service.VentaService;

/**
 * Pruebas de {@link VentaController}.
 */
@WebMvcTest(VentaController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class VentaControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private VentaService servicio;

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

	/** Un pago con método en línea no entra por el pago manual. */
	@Test
	void pagoManualEnLineaResponde400() throws Exception {
		mockMvc.perform(post("/ventas/7/pagos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoMetodo\":\"EN_LINEA\",\"monto\":1000}")
				.with(authentication(con("pago.registrar")))).andExpect(status().isBadRequest());
	}

	/** Un pago mayor que el saldo responde 409 con el mensaje del trigger. */
	@Test
	void pagoMayorQueSaldoResponde409() throws Exception {
		String mensaje = "El pago (9000000) supera el saldo pendiente de la venta 7 (8500000.00).";
		when(servicio.registrarPago(eq(7L), any())).thenThrow(rechazo(mensaje));

		mockMvc.perform(post("/ventas/7/pagos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoMetodo\":\"EFECTIVO\",\"monto\":9000000}")
				.with(authentication(con("pago.registrar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}

	/** La cartera vencida se consulta con cartera.ver, pero no permite registrar ventas. */
	@Test
	void carteraNoRegistraVentas() throws Exception {
		when(servicio.obtenerCarteraVencida()).thenReturn(List.of());
		mockMvc.perform(get("/cartera/vencida").with(authentication(con("cartera.ver")))).andExpect(status().isOk());
		mockMvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idPedido\":1,\"pesoTotalLibras\":576,\"precioLibra\":9000}")
				.with(authentication(con("cartera.ver")))).andExpect(status().isForbidden());
	}
}
