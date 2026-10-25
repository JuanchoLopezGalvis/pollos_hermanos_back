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
import co.edu.unbosque.pollos_hermanos_back.service.PedidoService;

/**
 * Pruebas de {@link PedidoController}.
 */
@WebMvcTest(PedidoController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class PedidoControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private PedidoService servicio;

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

	/** Un pedido sin lotes no pasa la validación. */
	@Test
	void pedidoSinLotesResponde400() throws Exception {
		mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idCliente\":1,\"fechaEstimadaEntrega\":\"2026-11-01\",\"detalle\":[]}")
				.with(authentication(con("pedido.gestionar")))).andExpect(status().isBadRequest());
	}

	/** Si ya no hay aves libres, la confirmación responde 409 con el mensaje del procedimiento. */
	@Test
	void confirmarSinAvesResponde409() throws Exception {
		String mensaje = "El lote L-1 tiene 20 aves libres: no alcanza para las 50 del pedido.";
		when(servicio.confirmar(5L)).thenThrow(rechazo(mensaje));

		mockMvc.perform(patch("/pedidos/5/confirmar").with(authentication(con("pedido.gestionar"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(mensaje));
	}
}
