package co.edu.unbosque.pollos_hermanos_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoginDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TokenDTO;
import co.edu.unbosque.pollos_hermanos_back.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Inicio de sesión con usuario y contraseña.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y obtención del token JWT")
public class AuthController {

	/** Servicio de autenticación. */
	private final AuthService authService;

	/**
	 * Crea el controlador.
	 *
	 * @param authService servicio de autenticación
	 */
	public AuthController(AuthService authService) {
		this.authService = authService;
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
}
