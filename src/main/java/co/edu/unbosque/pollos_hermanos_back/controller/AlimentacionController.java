package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.CompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EtapaAlimentacionDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.InventarioAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TipoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.service.AlimentacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Alimentación por etapas. Consultar requiere {@code alimentacion.ver};
 * registrar compras, consumos o tipos de alimento requiere
 * {@code alimentacion.registrar}.
 */
@RestController
@RequestMapping("/alimentacion")
@Tag(name = "Alimentación", description = "Inventario, compras por bultos y consumo por lote según su etapa")
public class AlimentacionController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('alimentacion.ver', 'alimentacion.registrar')";

	/** Permiso para registrar. */
	private static final String REGISTRAR = "@permisos.tiene('alimentacion.registrar')";

	/** Servicio de alimentación. */
	private final AlimentacionService alimentacionService;

	/**
	 * Crea el controlador.
	 *
	 * @param alimentacionService servicio de alimentación
	 */
	public AlimentacionController(AlimentacionService alimentacionService) {
		this.alimentacionService = alimentacionService;
	}

	/**
	 * Lista las etapas de alimentación.
	 *
	 * @return etapas
	 */
	@Operation(summary = "Etapas de alimentación", description = "Preiniciación, iniciación, engorde y finalización con sus días.")
	@ApiResponse(responseCode = "200", description = "Etapas en orden")
	@PreAuthorize(VER)
	@GetMapping("/etapas")
	public ResponseEntity<List<EtapaAlimentacionDTO>> obtenerEtapas() {
		return ResponseEntity.ok(alimentacionService.obtenerEtapas());
	}

	/**
	 * Muestra el inventario por tipo de alimento.
	 *
	 * @return inventario
	 */
	@Operation(summary = "Inventario de alimento", description = "Lee v_inventario_alimento: bultos comprados, consumidos, disponibles y costo promedio.")
	@ApiResponse(responseCode = "200", description = "Inventario")
	@PreAuthorize(VER)
	@GetMapping("/inventario")
	public ResponseEntity<List<InventarioAlimentoDTO>> obtenerInventario() {
		return ResponseEntity.ok(alimentacionService.obtenerInventario());
	}

	/**
	 * Registra un tipo de alimento.
	 *
	 * @param dto datos del tipo
	 * @return el tipo creado
	 */
	@Operation(summary = "Registrar tipo de alimento")
	@ApiResponse(responseCode = "201", description = "Tipo de alimento registrado")
	@ApiResponse(responseCode = "409", description = "Nombre repetido o etapa inexistente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/tipos")
	public ResponseEntity<InventarioAlimentoDTO> crearTipo(@Valid @RequestBody TipoAlimentoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(alimentacionService.crearTipo(dto));
	}

	/**
	 * Lista las compras de alimento.
	 *
	 * @return compras
	 */
	@Operation(summary = "Compras de alimento")
	@ApiResponse(responseCode = "200", description = "Compras, de la más reciente a la más antigua")
	@PreAuthorize(VER)
	@GetMapping("/compras")
	public ResponseEntity<List<CompraAlimentoDTO>> obtenerCompras() {
		return ResponseEntity.ok(alimentacionService.obtenerCompras());
	}

	/**
	 * Registra una compra de alimento.
	 *
	 * @param dto datos de la compra
	 * @return la compra registrada
	 */
	@Operation(summary = "Registrar compra de alimento", description = "Invoca sp_registrar_compra_alimento. El proveedor debe estar activo y suministrar alimento.")
	@ApiResponse(responseCode = "201", description = "Compra registrada")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó la compra", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/compras")
	public ResponseEntity<CompraAlimentoDTO> registrarCompra(@Valid @RequestBody RegistroCompraAlimentoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(alimentacionService.registrarCompra(dto));
	}

	/**
	 * Lista los consumos de alimento.
	 *
	 * @param lote lote para filtrar, opcional
	 * @return consumos
	 */
	@Operation(summary = "Consumos de alimento")
	@ApiResponse(responseCode = "200", description = "Consumos, del más reciente al más antiguo")
	@PreAuthorize(VER)
	@GetMapping("/consumos")
	public ResponseEntity<List<ConsumoAlimentoDTO>> obtenerConsumos(
			@Parameter(description = "Identificador del lote") @RequestParam(required = false) Long lote) {
		return ResponseEntity.ok(alimentacionService.obtenerConsumos(lote));
	}

	/**
	 * Registra el consumo de alimento de un lote.
	 *
	 * @param dto datos del consumo
	 * @return el consumo registrado
	 */
	@Operation(summary = "Registrar consumo de alimento", description = "Invoca sp_registrar_consumo_alimento. PostgreSQL valida que el alimento corresponda a la etapa del lote y que haya inventario, y valora el consumo al costo promedio ponderado.")
	@ApiResponse(responseCode = "201", description = "Consumo registrado")
	@ApiResponse(responseCode = "409", description = "Etapa equivocada o inventario insuficiente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/consumos")
	public ResponseEntity<ConsumoAlimentoDTO> registrarConsumo(@Valid @RequestBody RegistroConsumoAlimentoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(alimentacionService.registrarConsumo(dto));
	}
}
