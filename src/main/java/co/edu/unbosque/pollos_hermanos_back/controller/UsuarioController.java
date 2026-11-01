package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.AlCrear;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.UsuarioDTO;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;
import co.edu.unbosque.pollos_hermanos_back.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Administración de usuarios, sus roles y sus permisos directos.
 * <p>
 * Requiere el permiso {@code usuario.gestionar}, salvo {@code /usuarios/yo}.
 * Solo un superusuario puede crear superusuarios o cambiar esa marca.
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Administración de usuarios, roles asignados y permisos directos")
public class UsuarioController {

	/** Permiso que exige la administración de usuarios. */
	private static final String GESTIONAR = "@permisos.tiene('usuario.gestionar')";

	/** Servicio de usuarios. */
	private final UsuarioService usuarioService;

	/** Evaluador de permisos del usuario autenticado. */
	private final EvaluadorPermisos permisos;

	/**
	 * Crea el controlador.
	 *
	 * @param usuarioService servicio de usuarios
	 * @param permisos       evaluador de permisos
	 */
	public UsuarioController(UsuarioService usuarioService, EvaluadorPermisos permisos) {
		this.usuarioService = usuarioService;
		this.permisos = permisos;
	}

	/**
	 * Devuelve los datos y permisos del usuario autenticado.
	 *
	 * @return el usuario autenticado
	 */
	@Operation(summary = "Usuario autenticado", description = "Devuelve los datos, roles y permisos efectivos de quien hace la solicitud.")
	@ApiResponse(responseCode = "200", description = "Datos del usuario")
	@GetMapping("/yo")
	public ResponseEntity<UsuarioDTO> obtenerActual() {
		return ResponseEntity.ok(usuarioService.obtenerPorId(permisos.usuarioActual().getId()));
	}

	/**
	 * Crea un usuario.
	 *
	 * @param dto datos del usuario; la contraseña es obligatoria
	 * @return el usuario creado
	 */
	@Operation(summary = "Crear usuario", description = "Crea un usuario con sus roles (por nombre) y permisos directos (por código).")
	@ApiResponse(responseCode = "201", description = "Usuario creado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "404", description = "Algún rol o permiso no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El nombre de usuario o el correo ya existen", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<UsuarioDTO> crear(@Validated(AlCrear.class) @RequestBody UsuarioDTO dto) {
		exigirSuperusuarioSiCambiaMarca(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto));
	}

	/**
	 * Lista todos los usuarios, activos e inactivos.
	 *
	 * @return usuarios registrados
	 */
	@Operation(summary = "Listar usuarios")
	@ApiResponse(responseCode = "200", description = "Lista de usuarios")
	@PreAuthorize(GESTIONAR)
	@GetMapping
	public ResponseEntity<List<UsuarioDTO>> obtenerTodos() {
		return ResponseEntity.ok(usuarioService.obtenerTodos());
	}

	/**
	 * Obtiene un usuario.
	 *
	 * @param id identificador del usuario
	 * @return el usuario
	 */
	@Operation(summary = "Obtener usuario")
	@ApiResponse(responseCode = "200", description = "Usuario encontrado")
	@ApiResponse(responseCode = "404", description = "El usuario no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@GetMapping("/{id}")
	public ResponseEntity<UsuarioDTO> obtenerPorId(@Parameter(description = "Identificador del usuario") @PathVariable Long id) {
		return ResponseEntity.ok(usuarioService.obtenerPorId(id));
	}

	/**
	 * Actualiza un usuario. Solo cambian los campos enviados.
	 *
	 * @param id  identificador del usuario
	 * @param dto nuevos datos
	 * @return el usuario actualizado
	 */
	@Operation(summary = "Actualizar usuario", description = "Solo cambian los campos enviados. Si se envía 'roles' o 'permisos', reemplazan a los actuales.")
	@ApiResponse(responseCode = "200", description = "Usuario actualizado")
	@ApiResponse(responseCode = "404", description = "El usuario, un rol o un permiso no existen", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<UsuarioDTO> actualizar(@Parameter(description = "Identificador del usuario") @PathVariable Long id,
			@Valid @RequestBody UsuarioDTO dto) {
		exigirSuperusuarioSiCambiaMarca(dto);
		return ResponseEntity.ok(usuarioService.actualizar(id, dto));
	}

	/**
	 * Desactiva un usuario. No se borra, para conservar su historial.
	 *
	 * @param id identificador del usuario
	 * @return respuesta sin contenido
	 */
	@Operation(summary = "Desactivar usuario", description = "El usuario no se borra: queda inactivo y conserva su historial.")
	@ApiResponse(responseCode = "204", description = "Usuario desactivado")
	@ApiResponse(responseCode = "404", description = "El usuario no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desactivar(@Parameter(description = "Identificador del usuario") @PathVariable Long id) {
		usuarioService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Impide que alguien que no es superusuario otorgue o quite esa marca.
	 *
	 * @param dto datos recibidos
	 * @throws AccessDeniedException si se intenta cambiar la marca sin ser superusuario
	 */
	private void exigirSuperusuarioSiCambiaMarca(UsuarioDTO dto) {
		if (dto.getSuperusuario() != null && !permisos.esSuperusuario()) {
			throw new AccessDeniedException("Solo un superusuario puede cambiar la marca de superusuario.");
		}
	}
}
