package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import co.edu.unbosque.pollos_hermanos_back.dto.DisponibilidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.PortalClienteService;

/**
 * Pruebas de {@link PortalClienteController}.
 */
@WebMvcTest(PortalClienteController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class PortalClienteControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private PortalClienteService servicio;

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

	/** Pedido que vuelve del portal, sin lotes. */
	private static final PedidoDTO PEDIDO = new PedidoDTO(5L, 4L, "Asadero", LocalDate.of(2026, 10, 25),
			LocalDate.of(2026, 10, 28), "PENDIENTE", null, 150, null, null);

	/** Sin sesión el portal responde 401. */
	@Test
	void sinSesionResponde401() throws Exception {
		mockMvc.perform(get("/portal/pedidos")).andExpect(status().isUnauthorized());
	}

	/** El personal sin el permiso del portal no entra. */
	@Test
	void personalSinPermisoDelPortalResponde403() throws Exception {
		mockMvc.perform(get("/portal/pedidos").with(authentication(con("pedido.gestionar")))).andExpect(status().isForbidden());
	}

	/** El cliente consulta sus pedidos. */
	@Test
	void clienteConsultaSusPedidos() throws Exception {
		when(servicio.pedidos()).thenReturn(List.of(PEDIDO));

		mockMvc.perform(get("/portal/pedidos").with(authentication(con("portal.cliente")))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].totalAves").value(150)).andExpect(jsonPath("$[0].lotes").doesNotExist());
	}

	/** El cliente ve cuántas aves hay, sin los lotes. */
	@Test
	void clienteConsultaDisponibilidad() throws Exception {
		when(servicio.disponibilidad()).thenReturn(new DisponibilidadDTO(89));

		mockMvc.perform(get("/portal/disponibilidad").with(authentication(con("portal.cliente")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.avesDisponibles").value(89));
	}

	/** El cliente hace un pedido de una cantidad de aves. */
	@Test
	void clienteHaceUnPedido() throws Exception {
		when(servicio.crearPedido(any())).thenReturn(PEDIDO);

		mockMvc.perform(post("/portal/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cantidadAves\":150,\"fechaEstimadaEntrega\":\"" + LocalDate.now().plusDays(3) + "\"}")
				.with(authentication(con("portal.cliente")))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	/** Una cantidad de cero aves no pasa la validación. */
	@Test
	void pedidoDeCeroAvesResponde400() throws Exception {
		mockMvc.perform(post("/portal/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cantidadAves\":0,\"fechaEstimadaEntrega\":\"" + LocalDate.now().plusDays(3) + "\"}")
				.with(authentication(con("portal.cliente")))).andExpect(status().isBadRequest());
	}

	/** Sin aves suficientes, PostgreSQL rechaza el pedido y el cliente recibe el mensaje. */
	@Test
	void sinExistenciasResponde409ConElMensaje() throws Exception {
		String mensaje = "Solo hay 89 aves disponibles: no alcanza para las 150 del pedido.";
		when(servicio.crearPedido(any()))
				.thenThrow(new DataIntegrityViolationException("rechazo", new SQLException("ERROR: " + mensaje, "P0001")));

		mockMvc.perform(post("/portal/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"cantidadAves\":150,\"fechaEstimadaEntrega\":\"" + LocalDate.now().plusDays(3) + "\"}")
				.with(authentication(con("portal.cliente")))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value(mensaje));
	}
}
