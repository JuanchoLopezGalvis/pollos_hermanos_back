package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.CurvaCrecimientoSemanalDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.SeguimientoCrecimientoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.SeguimientoCrecimientoService;

/**
 * Pruebas de {@link SeguimientoCrecimientoController}.
 */
@WebMvcTest(SeguimientoCrecimientoController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class SeguimientoCrecimientoControllerTest {

	/** Seguimiento válido en JSON. */
	private static final String SEGUIMIENTO = """
			{"codigoLote":"L-001","fecha":"2026-10-08","pesoPromedioGramos":185.5,
			 "temperaturaCelsius":31.0,"humedadRelativa":65.0,
			 "muestreo":{"avesPesadas":50,"uniformidadPorcentaje":82.0},
			 "novedades":[{"tipo":"Cama húmeda","descripcion":"Sector norte"}]}
			""";

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de seguimiento simulado. */
	@MockitoBean
	private SeguimientoCrecimientoService seguimientoService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/**
	 * Autenticación de un operario que puede registrar seguimientos.
	 *
	 * @return autenticación
	 */
	private static Authentication registrador() {
		return UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.registrar"));
	}

	/** Registrar un seguimiento responde 201 con la edad calculada. */
	@Test
	void registraSeguimiento() throws Exception {
		SeguimientoCrecimientoDTO creado = new SeguimientoCrecimientoDTO();
		creado.setId("abc");
		creado.setCodigoLote("L-001");
		creado.setEdadDias(8);
		when(seguimientoService.crear(any())).thenReturn(creado);

		mockMvc.perform(post("/seguimientos").contentType(MediaType.APPLICATION_JSON).content(SEGUIMIENTO)
				.with(authentication(registrador()))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value("abc")).andExpect(jsonPath("$.edadDias").value(8));
	}

	/** Si el lote no está activo responde 409 con el mensaje de la regla. */
	@Test
	void loteNoActivoResponde409() throws Exception {
		when(seguimientoService.crear(any())).thenThrow(new ReglaNegocioException("El lote L-001 no está activo."));

		mockMvc.perform(post("/seguimientos").contentType(MediaType.APPLICATION_JSON).content(SEGUIMIENTO)
				.with(authentication(registrador()))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("El lote L-001 no está activo."));
	}

	/** Una humedad mayor que 100 responde 400 sin llegar al servicio. */
	@Test
	void humedadInvalidaResponde400() throws Exception {
		mockMvc.perform(post("/seguimientos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoLote\":\"L-001\",\"humedadRelativa\":130}").with(authentication(registrador())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detalles[0]").value("humedadRelativa: no puede ser mayor que 100"));
		verify(seguimientoService, never()).crear(any());
	}

	/** Sin código de lote responde 400. */
	@Test
	void sinCodigoLoteResponde400() throws Exception {
		mockMvc.perform(post("/seguimientos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pesoPromedioGramos\":180}").with(authentication(registrador())))
				.andExpect(status().isBadRequest());
	}

	/** Quien solo puede ver no puede registrar. */
	@Test
	void usuarioDeSoloLecturaNoPuedeRegistrar() throws Exception {
		mockMvc.perform(post("/seguimientos").contentType(MediaType.APPLICATION_JSON).content(SEGUIMIENTO)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.ver")))))
				.andExpect(status().isForbidden());
	}

	/** La curva semanal se consulta con el permiso de lectura. */
	@Test
	void consultaCurvaSemanal() throws Exception {
		when(seguimientoService.calcularCurvaSemanal("L-001"))
				.thenReturn(List.of(new CurvaCrecimientoSemanalDTO(1, 160.2, 7)));

		mockMvc.perform(get("/seguimientos/lote/L-001/curva-semanal")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.ver")))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].semana").value(1))
				.andExpect(jsonPath("$[0].pesoPromedioGramos").value(160.2));
	}

	/** Un seguimiento inexistente responde 404. */
	@Test
	void seguimientoInexistenteResponde404() throws Exception {
		when(seguimientoService.obtenerPorId("nada"))
				.thenThrow(new RecursoNoEncontradoException("No existe el seguimiento con id nada."));

		mockMvc.perform(get("/seguimientos/nada")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.ver")))))
				.andExpect(status().isNotFound());
	}
}
