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
import co.edu.unbosque.pollos_hermanos_back.dto.GalponDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GalponOcupacionDTO;
import co.edu.unbosque.pollos_hermanos_back.service.GalponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Galpones de la finca. Consultar requiere {@code galpon.ver}; crear, modificar
 * o eliminar requiere {@code galpon.gestionar}.
 */
@RestController
@RequestMapping("/galpones")
@Tag(name = "Galpones", description = "Galpones de la finca: varios lotes por galpón, capacidad compartida y ocupación")
public class GalponController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('galpon.ver', 'galpon.gestionar', 'lote.gestionar')";

	/** Permiso para crear, modificar o eliminar. */
	private static final String GESTIONAR = "@permisos.tiene('galpon.gestionar')";

	/** Servicio de galpones. */
	private final GalponService galponService;

	/**
	 * Crea el controlador.
	 *
	 * @param galponService servicio de galpones
	 */
	public GalponController(GalponService galponService) {
		this.galponService = galponService;
	}

	/**
	 * Crea un galpón.
	 *
	 * @param dto datos del galpón
	 * @return el galpón creado
	 */
	@Operation(summary = "Crear galpón", description = "Si no se envía el estado, el galpón queda disponible. El estado OCUPADO lo asigna la base de datos al registrar un lote.")
	@ApiResponse(responseCode = "201", description = "Galpón creado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "Nombre repetido o estado no permitido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<GalponDTO> crear(@Valid @RequestBody GalponDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(galponService.crear(dto));
	}

	/**
	 * Lista los galpones.
	 *
	 * @return galpones registrados
	 */
	@Operation(summary = "Listar galpones")
	@ApiResponse(responseCode = "200", description = "Lista de galpones")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<GalponDTO>> obtenerTodos() {
		return ResponseEntity.ok(galponService.obtenerTodos());
	}

	/**
	 * Muestra la ocupación de los galpones para el panel general.
	 *
	 * @return ocupación por galpón
	 */
	@Operation(summary = "Ocupación de los galpones", description = "Lee v_galpon_ocupacion: lotes activos, aves alojadas, capacidad libre y próxima salida.")
	@ApiResponse(responseCode = "200", description = "Ocupación por galpón")
	@PreAuthorize(VER)
	@GetMapping("/ocupacion")
	public ResponseEntity<List<GalponOcupacionDTO>> obtenerOcupacion() {
		return ResponseEntity.ok(galponService.obtenerOcupacion());
	}

	/**
	 * Obtiene un galpón.
	 *
	 * @param id identificador del galpón
	 * @return el galpón
	 */
	@Operation(summary = "Obtener galpón")
	@ApiResponse(responseCode = "200", description = "Galpón encontrado")
	@ApiResponse(responseCode = "404", description = "El galpón no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/{id}")
	public ResponseEntity<GalponDTO> obtenerPorId(@Parameter(description = "Identificador del galpón") @PathVariable Long id) {
		return ResponseEntity.ok(galponService.obtenerPorId(id));
	}

	/**
	 * Actualiza un galpón.
	 *
	 * @param id  identificador del galpón
	 * @param dto nuevos datos
	 * @return el galpón actualizado
	 */
	@Operation(summary = "Actualizar galpón", description = "La capacidad no puede quedar por debajo de las aves de sus lotes activos, y un galpón con lotes activos no cambia de estado.")
	@ApiResponse(responseCode = "200", description = "Galpón actualizado")
	@ApiResponse(responseCode = "404", description = "El galpón no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el cambio", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<GalponDTO> actualizar(@Parameter(description = "Identificador del galpón") @PathVariable Long id,
			@Valid @RequestBody GalponDTO dto) {
		return ResponseEntity.ok(galponService.actualizar(id, dto));
	}

	/**
	 * Elimina un galpón sin lotes.
	 *
	 * @param id identificador del galpón
	 * @return respuesta sin contenido
	 */
	@Operation(summary = "Eliminar galpón", description = "Solo se pueden eliminar galpones que nunca han tenido lotes.")
	@ApiResponse(responseCode = "204", description = "Galpón eliminado")
	@ApiResponse(responseCode = "404", description = "El galpón no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El galpón tiene lotes registrados", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@Parameter(description = "Identificador del galpón") @PathVariable Long id) {
		galponService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
