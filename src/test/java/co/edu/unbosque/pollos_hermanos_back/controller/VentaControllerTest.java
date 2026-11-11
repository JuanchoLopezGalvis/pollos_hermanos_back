package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

	/** La cartera vencida se consulta con cartera.ver, pero no permite registrar ventas. */
	@Test
	void carteraNoRegistraVentas() throws Exception {
		when(servicio.obtenerCarteraVencida()).thenReturn(List.of());
		mockMvc.perform(get("/cartera/vencida").with(authentication(con("cartera.ver")))).andExpect(status().isOk());
		mockMvc.perform(post("/ventas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idPedido\":1,\"pesoTotalLibras\":576,\"precioLibra\":9000}")
				.with(authentication(con("cartera.ver")))).andExpect(status().isForbidden());
	}

	/** No hay registro manual de pagos con método y monto libres: solo Wompi o el pago en efectivo del saldo. */
	@Test
	void elPersonalNoRegistraPagos() throws Exception {
		mockMvc.perform(post("/ventas/7/pagos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoMetodo\":\"EFECTIVO\",\"monto\":1000}")
				.with(authentication(con("pago.registrar", "venta.gestionar")))).andExpect(status().isMethodNotAllowed());
	}

	/** Con pago.registrar se registra el pago en efectivo del saldo. */
	@Test
	void registraPagoEnEfectivo() throws Exception {
		mockMvc.perform(post("/ventas/7/pago-efectivo").with(authentication(con("pago.registrar")))).andExpect(status().isOk());
		verify(servicio).registrarPagoEfectivo(7L);
	}

	/** Sin pago.registrar no se registra el pago en efectivo, aunque gestione ventas. */
	@Test
	void sinPermisoNoRegistraPagoEnEfectivo() throws Exception {
		mockMvc.perform(post("/ventas/7/pago-efectivo").with(authentication(con("venta.gestionar"))))
				.andExpect(status().isForbidden());
	}

	/** Si la base rechaza el pago (venta ya pagada), responde 409 con el mensaje. */
	@Test
	void pagoEnEfectivoRechazadoResponde409() throws Exception {
		when(servicio.registrarPagoEfectivo(7L)).thenThrow(rechazo("La venta 7 ya está pagada."));
		mockMvc.perform(post("/ventas/7/pago-efectivo").with(authentication(con("pago.registrar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("La venta 7 ya está pagada."));
	}

	/** Una ruta que no existe responde 404, no 500. */
	@Test
	void rutaInexistenteResponde404() throws Exception {
		mockMvc.perform(post("/pagos-en-linea/ventas/7").contentType(MediaType.APPLICATION_JSON).content("{}")
				.with(authentication(con("pago.registrar")))).andExpect(status().isNotFound());
	}
}
