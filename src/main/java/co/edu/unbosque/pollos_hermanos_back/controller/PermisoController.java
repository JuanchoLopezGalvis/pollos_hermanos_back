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
import co.edu.unbosque.pollos_hermanos_back.dto.PermisoDTO;
import co.edu.unbosque.pollos_hermanos_back.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Catálogo de permisos del sistema. Consultarlo requiere {@code rol.gestionar}
 * (para armar roles); modificarlo, ser superusuario.
 */
@RestController
@RequestMapping("/permisos")
@Tag(name = "Permisos", description = "Catálogo de permisos con la forma modulo.accion")
public class PermisoController {

	/** Servicio de permisos. */
	private final PermisoService permisoService;

	/**
	 * Crea el controlador.
	 *
	 * @param permisoService servicio de permisos
	 */
	public PermisoController(PermisoService permisoService) {
		this.permisoService = permisoService;
	}

	/**
	 * Lista los permisos.
	 *
	 * @return permisos registrados
	 */
	@Operation(summary = "Listar permisos")
	@ApiResponse(responseCode = "200", description = "Lista de permisos")
	@PreAuthorize("@permisos.tieneAlguno('rol.gestionar', 'usuario.gestionar')")
	@GetMapping
	public ResponseEntity<List<PermisoDTO>> obtenerTodos() {
		return ResponseEntity.ok(permisoService.obtenerTodos());
	}

	/**
	 * Crea un permiso.
	 *
	 * @param dto código y descripción del permiso
	 * @return el permiso creado
	 */
	@Operation(summary = "Crear permiso", description = "Solo superusuarios.")
	@ApiResponse(responseCode = "201", description = "Permiso creado")
	@ApiResponse(responseCode = "409", description = "Ya existe un permiso con ese código", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize("@permisos.esSuperusuario()")
	@PostMapping
	public ResponseEntity<PermisoDTO> crear(@Valid @RequestBody PermisoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(permisoService.crear(dto));
	}

	/**
	 * Actualiza un permiso.
	 *
	 * @param id  identificador del permiso
	 * @param dto nuevos datos
	 * @return el permiso actualizado
	 */
	@Operation(summary = "Actualizar permiso", description = "Solo superusuarios.")
	@ApiResponse(responseCode = "200", description = "Permiso actualizado")
	@ApiResponse(responseCode = "404", description = "El permiso no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize("@permisos.esSuperusuario()")
	@PutMapping("/{id}")
	public ResponseEntity<PermisoDTO> actualizar(@Parameter(description = "Identificador del permiso") @PathVariable Long id,
			@Valid @RequestBody PermisoDTO dto) {
		return ResponseEntity.ok(permisoService.actualizar(id, dto));
	}

	/**
	 * Elimina un permiso.
	 *
	 * @param id identificador del permiso
	 * @return respuesta sin contenido
	 */
	@Operation(summary = "Eliminar permiso", description = "Solo superusuarios.")
	@ApiResponse(responseCode = "204", description = "Permiso eliminado")
	@ApiResponse(responseCode = "409", description = "El permiso está asignado a roles o usuarios", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize("@permisos.esSuperusuario()")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@Parameter(description = "Identificador del permiso") @PathVariable Long id) {
		permisoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
