package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.EventosWompiService;

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
	private EventosWompiService servicio;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Wompi llama sin token: la ruta es pública y registra el pago aprobado. */
	@Test
	void wompiAvisaSinToken() throws Exception {
		when(servicio.procesar(any())).thenReturn(new ResultadoPagoEnLineaDTO("tx-1", "APPROVED", true, 7L,
				new BigDecimal("44900"), BigDecimal.ZERO, "Pago aprobado y registrado en la venta 7."));

		mockMvc.perform(post("/pagos-en-linea/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"event\":\"transaction.updated\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.registrado").value(true));
	}

	/** Un aviso con firma falsa responde 403. */
	@Test
	void firmaFalsaResponde403() throws Exception {
		when(servicio.procesar(any())).thenThrow(new AccessDeniedException("La firma del aviso no corresponde a Wompi."));

		mockMvc.perform(post("/pagos-en-linea/eventos").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}
}
