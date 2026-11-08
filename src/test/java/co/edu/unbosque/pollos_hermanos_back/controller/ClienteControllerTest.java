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
import co.edu.unbosque.pollos_hermanos_back.service.ClienteService;
import co.edu.unbosque.pollos_hermanos_back.service.CuentaClienteService;

/**
 * Pruebas de {@link ClienteController}.
 */
@WebMvcTest(ClienteController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class ClienteControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private ClienteService servicio;

	/** Cuentas de clientes simuladas. */
	@MockitoBean
	private CuentaClienteService cuentaClienteService;

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

	/** Un correo inválido no pasa la validación. */
	@Test
	void correoInvalidoResponde400() throws Exception {
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idTipoCliente\":1,\"documento\":\"1\",\"nombre\":\"Ana\",\"correo\":\"no-es-correo\"}")
				.with(authentication(con("cliente.gestionar")))).andExpect(status().isBadRequest());
	}

	/** Quien gestiona pedidos consulta clientes. */
	@Test
	void pedidosConsultaClientes() throws Exception {
		when(servicio.obtenerTodos()).thenReturn(List.of());
		mockMvc.perform(get("/clientes").with(authentication(con("pedido.gestionar")))).andExpect(status().isOk());
	}

	/** Crear la cuenta de un cliente exige cliente.gestionar. */
	@Test
	void crearCuentaExigeGestionarClientes() throws Exception {
		mockMvc.perform(post("/clientes/4/cuenta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombreUsuario\":\"asadero\",\"contrasena\":\"Clave123*\"}")
				.with(authentication(con("pedido.gestionar")))).andExpect(status().isForbidden());
	}

	/** Quien gestiona clientes les crea la cuenta. */
	@Test
	void creaCuentaDelCliente() throws Exception {
		when(cuentaClienteService.crearCuenta(eq(4L), any())).thenReturn(
				new ClienteDTO(4L, 2L, "Restaurante", "900123", "Asadero", null, null, "a@b.co", true, 9L, "asadero"));

		mockMvc.perform(post("/clientes/4/cuenta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombreUsuario\":\"asadero\",\"contrasena\":\"Clave123*\"}")
				.with(authentication(con("cliente.gestionar")))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuario").value("asadero"));
	}
}
