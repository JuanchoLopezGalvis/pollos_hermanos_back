package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.CatalogoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoginDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCuentaClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.security.AuthService;
import co.edu.unbosque.pollos_hermanos_back.service.CatalogoService;
import co.edu.unbosque.pollos_hermanos_back.service.CuentaClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Inicio de sesión con usuario y contraseña, y registro de clientes en el
 * portal. Todas estas rutas son públicas.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y obtención del token JWT")
public class AuthController {

	/** Servicio de autenticación. */
	private final AuthService authService;

	/** Cuentas de los clientes. */
	private final CuentaClienteService cuentaClienteService;

	/** Catálogos, para los tipos de cliente del registro. */
	private final CatalogoService catalogoService;

	/**
	 * Crea el controlador.
	 *
	 * @param authService          servicio de autenticación
	 * @param cuentaClienteService cuentas de los clientes
	 * @param catalogoService      catálogos
	 */
	public AuthController(AuthService authService, CuentaClienteService cuentaClienteService, CatalogoService catalogoService) {
		this.authService = authService;
		this.cuentaClienteService = cuentaClienteService;
		this.catalogoService = catalogoService;
	}

	/**
	 * Valida las credenciales y devuelve el token JWT con los permisos del usuario.
	 *
	 * @param login nombre de usuario o correo, y contraseña
	 * @return el token y los datos básicos del usuario
	 */
	@Operation(summary = "Iniciar sesión", description = "Acepta el nombre de usuario o el correo. Devuelve el token JWT y los permisos del usuario, para que el frontend arme el menú.")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada")
	@ApiResponse(responseCode = "400", description = "Faltan datos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@SecurityRequirements
	@PostMapping("/login")
	public ResponseEntity<TokenDTO> iniciarSesion(@Valid @RequestBody LoginDTO login) {
		return ResponseEntity.ok(authService.iniciarSesion(login));
	}

	/**
	 * Registra a un cliente nuevo con su cuenta y le abre la sesión.
	 *
	 * @param dto datos del cliente y de la cuenta
	 * @return el token de la sesión del cliente
	 */
	@Operation(summary = "Registrarse como cliente", description = "Crea el usuario (rol Cliente, solo entra al portal) y el cliente en una sola transacción, y devuelve la sesión. Si el documento ya es de un cliente, se rechaza: la finca le crea la cuenta.")
	@ApiResponse(responseCode = "201", description = "Cuenta creada y sesión iniciada")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "Documento, usuario o correo ya registrados", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@SecurityRequirements
	@PostMapping("/registro")
	public ResponseEntity<TokenDTO> registrarCliente(@Valid @RequestBody RegistroCuentaClienteDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(cuentaClienteService.registrar(dto));
	}

	/**
	 * Tipos de cliente para el formulario de registro.
	 *
	 * @return tipos de cliente activos
	 */
	@Operation(summary = "Tipos de cliente", description = "Para el formulario público de registro.")
	@ApiResponse(responseCode = "200", description = "Tipos de cliente activos")
	@SecurityRequirements
	@GetMapping("/tipos-cliente")
	public ResponseEntity<List<CatalogoDTO>> obtenerTiposCliente() {
		return ResponseEntity.ok(catalogoService.obtener("tipos-cliente"));
	}
}
