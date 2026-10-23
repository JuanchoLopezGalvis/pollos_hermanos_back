package co.edu.unbosque.pollos_hermanos_back.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.UsuarioDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.security.JwtUtil;
import co.edu.unbosque.pollos_hermanos_back.security.SecurityConfig;
import co.edu.unbosque.pollos_hermanos_back.service.UsuarioService;

/**
 * Pruebas de {@link UsuarioController}, incluidas las reglas de autorización.
 */
@WebMvcTest(UsuarioController.class)
@Import({ SecurityConfig.class, EvaluadorPermisos.class })
class UsuarioControllerTest {

	/** Usuario nuevo válido en JSON. */
	private static final String USUARIO_NUEVO = """
			{"nombreUsuario":"operario2","correo":"op2@pollos.co","contrasena":"Clave123*","roles":["Operario"]}
			""";

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mockMvc;

	/** Servicio de usuarios simulado. */
	@MockitoBean
	private UsuarioService usuarioService;

	/** Utilidad JWT simulada. */
	@MockitoBean
	private JwtUtil jwtUtil;

	/** Carga de usuarios simulada. */
	@MockitoBean
	private UserDetailsService userDetailsService;

	/** Sin token responde 401. */
	@Test
	void sinAutenticacionResponde401() throws Exception {
		mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.estado").value(401));
	}

	/** Sin el permiso usuario.gestionar responde 403. */
	@Test
	void sinPermisoResponde403() throws Exception {
		mockMvc.perform(get("/usuarios")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("seguimiento.ver")))))
				.andExpect(status().isForbidden());
	}

	/** Con el permiso lista los usuarios. */
	@Test
	void conPermisoListaUsuarios() throws Exception {
		UsuarioDTO dto = new UsuarioDTO();
		dto.setNombreUsuario("operario");
		when(usuarioService.obtenerTodos()).thenReturn(List.of(dto));

		mockMvc.perform(get("/usuarios")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].nombreUsuario").value("operario"));
	}

	/** Crear un usuario responde 201 y nunca devuelve la contraseña. */
	@Test
	void creaUsuarioSinDevolverContrasena() throws Exception {
		UsuarioDTO creado = new UsuarioDTO();
		creado.setNombreUsuario("operario2");
		creado.setContrasena("Clave123*");
		when(usuarioService.crear(any())).thenReturn(creado);

		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_NUEVO)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nombreUsuario").value("operario2"))
				.andExpect(jsonPath("$.contrasena").doesNotExist());
	}

	/** Al crear, la contraseña es obligatoria. */
	@Test
	void crearSinContrasenaResponde400() throws Exception {
		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombreUsuario\":\"operario2\",\"correo\":\"op2@pollos.co\"}")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isBadRequest());
	}

	/** Solo un superusuario puede crear superusuarios. */
	@Test
	void soloSuperusuarioPuedeCrearSuperusuarios() throws Exception {
		String superusuario = """
				{"nombreUsuario":"jefe","correo":"jefe@pollos.co","contrasena":"Clave123*","superusuario":true}
				""";

		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(superusuario)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isForbidden());
		verify(usuarioService, never()).crear(any());

		mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(superusuario)
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.superusuario()))))
				.andExpect(status().isCreated());
	}

	/** Cualquier usuario autenticado puede consultar sus propios datos. */
	@Test
	void consultaElUsuarioAutenticado() throws Exception {
		UsuarioDTO yo = new UsuarioDTO();
		yo.setNombreUsuario("operario");
		when(usuarioService.obtenerPorId(1L)).thenReturn(yo);

		mockMvc.perform(get("/usuarios/yo")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos()))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombreUsuario").value("operario"));
	}

	/** Desactivar responde 204. */
	@Test
	void desactivaUsuario() throws Exception {
		mockMvc.perform(delete("/usuarios/4")
				.with(authentication(UsuariosDePrueba.autenticacion(UsuariosDePrueba.conPermisos("usuario.gestionar")))))
				.andExpect(status().isNoContent());
		verify(usuarioService).eliminar(4L);
	}
}
