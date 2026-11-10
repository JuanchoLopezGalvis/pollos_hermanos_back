package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.AuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PaginaAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.AuditoriaService;

/**
 * Pruebas de {@link AuditoriaController}.
 */
@WebMvcTest(AuditoriaController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class AuditoriaControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio simulado. */
	@MockitoBean
	private AuditoriaService servicio;

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

	/** Los datos antes y después llegan como objetos JSON, no como texto. */
	@Test
	void devuelveLosDatosComoObjetos() throws Exception {
		AuditoriaDTO cambio = new AuditoriaDTO(7L, "galpon", "UPDATE", "32", List.of("capacidad_maxima"),
				"{\"capacidad_maxima\": 1000}", "{\"capacidad_maxima\": 1200}", "gerente", "pollos_app", 930L,
				LocalDateTime.of(2026, 11, 1, 10, 30));
		when(servicio.buscar(any(), eq(0), eq(50))).thenReturn(new PaginaAuditoriaDTO(List.of(cambio), 1, 0, 50));

		mockMvc.perform(get("/auditoria").with(authentication(con("auditoria.ver")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.registros[0].datosAntes.capacidad_maxima").value(1000))
				.andExpect(jsonPath("$.registros[0].datosDespues.capacidad_maxima").value(1200))
				.andExpect(jsonPath("$.registros[0].columnasCambiadas[0]").value("capacidad_maxima"));
	}

	/** Los filtros de la URL llegan al servicio. */
	@Test
	void pasaLosFiltros() throws Exception {
		when(servicio.buscar(any(), eq(1), eq(20))).thenReturn(new PaginaAuditoriaDTO(List.of(), 0, 1, 20));

		mockMvc.perform(get("/auditoria").param("tabla", "lote").param("operacion", "DELETE").param("usuario", "ger")
				.param("registro", "52").param("desde", "2026-11-01").param("hasta", "2026-11-02").param("pagina", "1")
				.param("tamano", "20").with(authentication(con("auditoria.ver")))).andExpect(status().isOk());

		verify(servicio).buscar(new FiltroAuditoriaDTO("lote", "DELETE", "ger", false, "52", LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 2)), 1, 20);
	}

	/** Sin el permiso auditoria.ver no se consulta. */
	@Test
	void sinPermisoResponde403() throws Exception {
		mockMvc.perform(get("/auditoria").with(authentication(con("lote.ver")))).andExpect(status().isForbidden());
	}

	/** Las tablas con cambios también requieren el permiso. */
	@Test
	void listaLasTablas() throws Exception {
		when(servicio.tablas()).thenReturn(List.of("galpon", "lote"));

		mockMvc.perform(get("/auditoria/tablas").with(authentication(con("auditoria.ver")))).andExpect(status().isOk())
				.andExpect(jsonPath("$[1]").value("lote"));
	}
}
