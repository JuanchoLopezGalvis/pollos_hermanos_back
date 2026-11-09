package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import co.edu.unbosque.pollos_hermanos_back.dto.CurvaCrecimientoSemanalDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.SeguimientoCrecimientoDTO;
import co.edu.unbosque.pollos_hermanos_back.service.SeguimientoCrecimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Seguimiento diario del crecimiento de los lotes (MongoDB). Consultar requiere
 * {@code seguimiento.ver}; registrar, modificar o eliminar requiere
 * {@code seguimiento.registrar}.
 */
@RestController
@RequestMapping("/seguimientos")
@Tag(name = "Seguimiento del crecimiento", description = "Registro diario de peso, condiciones del galpón y novedades de cada lote activo (MongoDB)")
public class SeguimientoCrecimientoController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('seguimiento.ver', 'seguimiento.registrar')";

	/** Permiso para registrar, modificar o eliminar. */
	private static final String REGISTRAR = "@permisos.tiene('seguimiento.registrar')";

	/** Servicio de seguimiento. */
	private final SeguimientoCrecimientoService seguimientoService;

	/**
	 * Crea el controlador.
	 *
	 * @param seguimientoService servicio de seguimiento
	 */
	public SeguimientoCrecimientoController(SeguimientoCrecimientoService seguimientoService) {
		this.seguimientoService = seguimientoService;
	}

	/**
	 * Registra el seguimiento de un lote en un día.
	 *
	 * @param dto datos del seguimiento
	 * @return el seguimiento registrado, con la edad del lote
	 */
	@Operation(summary = "Registrar seguimiento", description = "Verifica en PostgreSQL que el lote exista y esté activo, y calcula su edad con la fecha de ingreso. Si no se envía la fecha, se usa la de hoy. Un lote puede tener varios seguimientos el mismo día.")
	@ApiResponse(responseCode = "201", description = "Seguimiento registrado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "404", description = "El lote no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El lote no está activo", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping
	public ResponseEntity<SeguimientoCrecimientoDTO> crear(@Validated(AlCrear.class) @RequestBody SeguimientoCrecimientoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(seguimientoService.crear(dto));
	}

	/**
	 * Lista todos los seguimientos.
	 *
	 * @return seguimientos registrados
	 */
	@Operation(summary = "Listar seguimientos")
	@ApiResponse(responseCode = "200", description = "Lista de seguimientos")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<SeguimientoCrecimientoDTO>> obtenerTodos() {
		return ResponseEntity.ok(seguimientoService.obtenerTodos());
	}

	/**
	 * Obtiene un seguimiento.
	 *
	 * @param id identificador del documento
	 * @return el seguimiento
	 */
	@Operation(summary = "Obtener seguimiento")
	@ApiResponse(responseCode = "200", description = "Seguimiento encontrado")
	@ApiResponse(responseCode = "404", description = "El seguimiento no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/{id}")
	public ResponseEntity<SeguimientoCrecimientoDTO> obtenerPorId(@Parameter(description = "Identificador del documento") @PathVariable String id) {
		return ResponseEntity.ok(seguimientoService.obtenerPorId(id));
	}

	/**
	 * Lista los seguimientos de un lote ordenados por fecha.
	 *
	 * @param codigoLote código del lote
	 * @return seguimientos del lote
	 */
	@Operation(summary = "Seguimientos de un lote")
	@ApiResponse(responseCode = "200", description = "Seguimientos del lote, ordenados por fecha")
	@PreAuthorize(VER)
	@GetMapping("/lote/{codigoLote}")
	public ResponseEntity<List<SeguimientoCrecimientoDTO>> obtenerPorLote(@Parameter(description = "Código del lote") @PathVariable String codigoLote) {
		return ResponseEntity.ok(seguimientoService.obtenerPorLote(codigoLote));
	}

	/**
	 * Calcula la curva de crecimiento semanal de un lote.
	 *
	 * @param codigoLote código del lote
	 * @return peso promedio por semana de vida
	 */
	@Operation(summary = "Curva de crecimiento semanal", description = "Agregación de MongoDB: peso promedio por semana de vida del lote (semana 1 = días 1 a 7).")
	@ApiResponse(responseCode = "200", description = "Curva semanal")
	@PreAuthorize(VER)
	@GetMapping("/lote/{codigoLote}/curva-semanal")
	public ResponseEntity<List<CurvaCrecimientoSemanalDTO>> calcularCurvaSemanal(@Parameter(description = "Código del lote") @PathVariable String codigoLote) {
		return ResponseEntity.ok(seguimientoService.calcularCurvaSemanal(codigoLote));
	}

	/**
	 * Actualiza las mediciones de un seguimiento. El lote y la fecha no cambian.
	 *
	 * @param id  identificador del documento
	 * @param dto nuevos datos
	 * @return el seguimiento actualizado
	 */
	@Operation(summary = "Actualizar seguimiento", description = "Solo cambian las mediciones enviadas. El lote y la fecha no se modifican.")
	@ApiResponse(responseCode = "200", description = "Seguimiento actualizado")
	@ApiResponse(responseCode = "404", description = "El seguimiento no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El lote ya no está activo", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PutMapping("/{id}")
	public ResponseEntity<SeguimientoCrecimientoDTO> actualizar(@Parameter(description = "Identificador del documento") @PathVariable String id,
			@Valid @RequestBody SeguimientoCrecimientoDTO dto) {
		return ResponseEntity.ok(seguimientoService.actualizar(id, dto));
	}

	/**
	 * Elimina un seguimiento.
	 *
	 * @param id identificador del documento
	 * @return respuesta sin contenido
	 */
	@Operation(summary = "Eliminar seguimiento")
	@ApiResponse(responseCode = "204", description = "Seguimiento eliminado")
	@ApiResponse(responseCode = "404", description = "El seguimiento no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@Parameter(description = "Identificador del documento") @PathVariable String id) {
		seguimientoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
