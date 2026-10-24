package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CambioEstadoLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadRegistradaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.service.LoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Lotes de pollos y su mortalidad diaria. Consultar requiere {@code lote.ver};
 * registrar lotes o cambiar su estado requiere {@code lote.gestionar};
 * registrar mortalidad requiere {@code mortalidad.registrar}.
 */
@RestController
@RequestMapping("/lotes")
@Tag(name = "Lotes", description = "Lotes de pollos, su estado y su mortalidad diaria (procedimientos almacenados de PostgreSQL)")
public class LoteController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('lote.ver', 'lote.gestionar', 'mortalidad.registrar', 'seguimiento.registrar')";

	/** Permiso para registrar lotes y cambiar su estado. */
	private static final String GESTIONAR = "@permisos.tiene('lote.gestionar')";

	/** Permiso para registrar la mortalidad. */
	private static final String MORTALIDAD = "@permisos.tieneAlguno('mortalidad.registrar', 'lote.gestionar')";

	/** Servicio de lotes. */
	private final LoteService loteService;

	/**
	 * Crea el controlador.
	 *
	 * @param loteService servicio de lotes
	 */
	public LoteController(LoteService loteService) {
		this.loteService = loteService;
	}

	/**
	 * Registra un lote.
	 *
	 * @param dto datos del lote
	 * @return el lote creado
	 */
	@Operation(summary = "Registrar lote", description = "Invoca sp_registrar_lote. Un galpón aloja varios lotes: PostgreSQL valida que la suma de aves de sus lotes activos no supere la capacidad y que la venta estimada esté entre 35 y 45 días; si no se envía, usa el ingreso más 40 días.")
	@ApiResponse(responseCode = "201", description = "Lote registrado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el lote", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<LoteDTO> crear(@Valid @RequestBody RegistroLoteDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(loteService.crear(dto));
	}

	/**
	 * Lista los lotes con su edad, mortalidad y aves disponibles.
	 *
	 * @return lotes registrados
	 */
	@Operation(summary = "Listar lotes", description = "Lee la vista v_lote_resumen: edad, mortalidad acumulada, aves vivas, vendidas, consumo de alimento y alerta del 5%.")
	@ApiResponse(responseCode = "200", description = "Lista de lotes")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<LoteDTO>> obtenerTodos() {
		return ResponseEntity.ok(loteService.obtenerTodos());
	}

	/**
	 * Obtiene un lote.
	 *
	 * @param id identificador del lote
	 * @return el lote
	 */
	@Operation(summary = "Obtener lote")
	@ApiResponse(responseCode = "200", description = "Lote encontrado")
	@ApiResponse(responseCode = "404", description = "El lote no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/{id}")
	public ResponseEntity<LoteDTO> obtenerPorId(@Parameter(description = "Identificador del lote") @PathVariable Long id) {
		return ResponseEntity.ok(loteService.obtenerPorId(id));
	}

	/**
	 * Corrige los datos editables de un lote.
	 *
	 * @param id  identificador del lote
	 * @param dto nuevos datos
	 * @return el lote actualizado
	 */
	@Operation(summary = "Corregir lote", description = "Cambia la cantidad inicial, el costo del pollito o la fecha estimada de venta. El código, el galpón y la fecha de ingreso no cambian.")
	@ApiResponse(responseCode = "200", description = "Lote actualizado")
	@ApiResponse(responseCode = "404", description = "El lote no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el cambio", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<LoteDTO> actualizar(@Parameter(description = "Identificador del lote") @PathVariable Long id,
			@Valid @RequestBody ActualizacionLoteDTO dto) {
		return ResponseEntity.ok(loteService.actualizar(id, dto));
	}

	/**
	 * Cambia el estado de un lote.
	 *
	 * @param id  identificador del lote
	 * @param dto nuevo estado
	 * @return el lote actualizado
	 */
	@Operation(summary = "Cambiar estado del lote", description = "Invoca sp_cambiar_estado_lote. Transiciones válidas: EN_ENGORDE → LISTO_PARA_VENTA; VENDIDO o CERRADO solo cuando las aves disponibles llegan a cero.")
	@ApiResponse(responseCode = "200", description = "Estado cambiado")
	@ApiResponse(responseCode = "409", description = "Transición no permitida", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PatchMapping("/{id}/estado")
	public ResponseEntity<LoteDTO> cambiarEstado(@Parameter(description = "Identificador del lote") @PathVariable Long id,
			@Valid @RequestBody CambioEstadoLoteDTO dto) {
		return ResponseEntity.ok(loteService.cambiarEstado(id, dto));
	}

	/**
	 * Lista la mortalidad registrada de un lote.
	 *
	 * @param id identificador del lote
	 * @return registros de mortalidad
	 */
	@Operation(summary = "Mortalidad del lote")
	@ApiResponse(responseCode = "200", description = "Registros, del día más reciente al más antiguo")
	@ApiResponse(responseCode = "404", description = "El lote no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/{id}/mortalidad")
	public ResponseEntity<List<MortalidadDTO>> obtenerMortalidad(@Parameter(description = "Identificador del lote") @PathVariable Long id) {
		return ResponseEntity.ok(loteService.obtenerMortalidad(id));
	}

	/**
	 * Registra la mortalidad de un día.
	 *
	 * @param id  identificador del lote
	 * @param dto datos de la mortalidad
	 * @return registro creado y alerta del 5%
	 */
	@Operation(summary = "Registrar mortalidad", description = "Invoca sp_registrar_mortalidad. Devuelve el porcentaje acumulado y alerta = true cuando supera el 5% de referencia.")
	@ApiResponse(responseCode = "201", description = "Mortalidad registrada")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el registro", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(MORTALIDAD)
	@PostMapping("/{id}/mortalidad")
	public ResponseEntity<MortalidadRegistradaDTO> registrarMortalidad(
			@Parameter(description = "Identificador del lote") @PathVariable Long id, @Valid @RequestBody MortalidadDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(loteService.registrarMortalidad(id, dto));
	}

	/**
	 * Corrige un registro de mortalidad.
	 *
	 * @param id           identificador del lote
	 * @param idMortalidad identificador del registro
	 * @param dto          nuevos datos
	 * @return el lote con su mortalidad recalculada
	 */
	@Operation(summary = "Corregir mortalidad", description = "Cambia la cantidad o la causa; la fecha no cambia.")
	@ApiResponse(responseCode = "200", description = "Mortalidad corregida")
	@ApiResponse(responseCode = "404", description = "El registro no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el cambio", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(MORTALIDAD)
	@PutMapping("/{id}/mortalidad/{idMortalidad}")
	public ResponseEntity<LoteDTO> actualizarMortalidad(@Parameter(description = "Identificador del lote") @PathVariable Long id,
			@Parameter(description = "Identificador del registro") @PathVariable Long idMortalidad,
			@Valid @RequestBody MortalidadDTO dto) {
		return ResponseEntity.ok(loteService.actualizarMortalidad(id, idMortalidad, dto));
	}
}
