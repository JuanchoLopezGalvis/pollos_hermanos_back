package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RolDTO;
import co.edu.unbosque.pollos_hermanos_back.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Administración de roles y de los permisos que otorga cada uno. Requiere el
 * permiso {@code rol.gestionar}.
 */
@RestController
@RequestMapping("/roles")
@PreAuthorize("@permisos.tiene('rol.gestionar')")
@Tag(name = "Roles", description = "Roles del sistema y sus permisos (equivalentes a los grupos de Django)")
public class RolController {

	/** Servicio de roles. */
	private final RolService rolService;

	/**
	 * Crea el controlador.
	 *
	 * @param rolService servicio de roles
	 */
	public RolController(RolService rolService) {
		this.rolService = rolService;
	}

	/**
	 * Crea un rol.
	 *
	 * @param dto datos del rol, con los códigos de sus permisos
	 * @return el rol creado
	 */
	@Operation(summary = "Crear rol")
	@ApiResponse(responseCode = "201", description = "Rol creado")
	@ApiResponse(responseCode = "404", description = "Algún permiso no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "Ya existe un rol con ese nombre", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PostMapping
	public ResponseEntity<RolDTO> crear(@Valid @RequestBody RolDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(dto));
	}

	/**
	 * Lista los roles.
	 *
	 * @return roles registrados
	 */
	@Operation(summary = "Listar roles")
	@ApiResponse(responseCode = "200", description = "Lista de roles")
	@GetMapping
	public ResponseEntity<List<RolDTO>> obtenerTodos() {
		return ResponseEntity.ok(rolService.obtenerTodos());
	}

	/**
	 * Obtiene un rol.
	 *
	 * @param id identificador del rol
	 * @return el rol
	 */
	@Operation(summary = "Obtener rol")
	@ApiResponse(responseCode = "200", description = "Rol encontrado")
	@ApiResponse(responseCode = "404", description = "El rol no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@GetMapping("/{id}")
	public ResponseEntity<RolDTO> obtenerPorId(@Parameter(description = "Identificador del rol") @PathVariable Long id) {
		return ResponseEntity.ok(rolService.obtenerPorId(id));
	}

	/**
	 * Actualiza un rol. Si se envían permisos, reemplazan a los actuales.
	 *
	 * @param id  identificador del rol
	 * @param dto nuevos datos
	 * @return el rol actualizado
	 */
	@Operation(summary = "Actualizar rol", description = "Si se envía 'permisos', reemplaza los permisos actuales del rol.")
	@ApiResponse(responseCode = "200", description = "Rol actualizado")
	@ApiResponse(responseCode = "404", description = "El rol o algún permiso no existen", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PutMapping("/{id}")
	public ResponseEntity<RolDTO> actualizar(@Parameter(description = "Identificador del rol") @PathVariable Long id,
			@Valid @RequestBody RolDTO dto) {
		return ResponseEntity.ok(rolService.actualizar(id, dto));
	}

	/**
	 * Elimina un rol.
	 *
	 * @param id identificador del rol
	 * @return respuesta sin contenido
	 */
	@Operation(summary = "Eliminar rol")
	@ApiResponse(responseCode = "204", description = "Rol eliminado")
	@ApiResponse(responseCode = "404", description = "El rol no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El rol está asignado a usuarios", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@Parameter(description = "Identificador del rol") @PathVariable Long id) {
		rolService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
