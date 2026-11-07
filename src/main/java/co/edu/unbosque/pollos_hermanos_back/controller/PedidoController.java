package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.DetallePedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Pedidos de aves. Consultar requiere {@code pedido.gestionar},
 * {@code venta.gestionar} o {@code cartera.ver}; crear, confirmar o cancelar
 * requiere {@code pedido.gestionar}.
 */
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Pedidos con aves de uno o varios lotes (procedimientos almacenados)")
public class PedidoController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('pedido.gestionar', 'venta.gestionar', 'cartera.ver')";

	/** Permiso para crear, confirmar o cancelar. */
	private static final String GESTIONAR = "@permisos.tiene('pedido.gestionar')";

	/** Servicio de pedidos. */
	private final PedidoService pedidoService;

	/**
	 * Crea el controlador.
	 *
	 * @param pedidoService servicio de pedidos
	 */
	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	/**
	 * Lista los pedidos.
	 *
	 * @param estado estado para filtrar, opcional
	 * @return pedidos
	 */
	@Operation(summary = "Listar pedidos")
	@ApiResponse(responseCode = "200", description = "Pedidos con total de aves, lotes y venta")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<PedidoDTO>> obtener(
			@Parameter(description = "PENDIENTE, CONFIRMADO, ENTREGADO o CANCELADO") @RequestParam(required = false) String estado) {
		return ResponseEntity.ok(pedidoService.obtener(estado));
	}

	/**
	 * Muestra las aves por lote de un pedido.
	 *
	 * @param id pedido
	 * @return detalle
	 */
	@Operation(summary = "Detalle del pedido")
	@ApiResponse(responseCode = "200", description = "Aves por lote")
	@ApiResponse(responseCode = "404", description = "El pedido no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/{id}/detalle")
	public ResponseEntity<List<DetallePedidoDTO>> obtenerDetalle(@Parameter(description = "Pedido") @PathVariable Long id) {
		return ResponseEntity.ok(pedidoService.obtenerDetalle(id));
	}

	/**
	 * Crea un pedido.
	 *
	 * @param dto cliente, fecha estimada y aves por lote
	 * @return el pedido creado
	 */
	@Operation(summary = "Crear pedido", description = "Invoca sp_crear_pedido. Solo se piden aves de lotes listos para venta y sin superar sus aves libres.")
	@ApiResponse(responseCode = "201", description = "Pedido creado como pendiente")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el pedido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<PedidoDTO> crear(@Valid @RequestBody RegistroPedidoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(dto));
	}

	/**
	 * Confirma un pedido.
	 *
	 * @param id pedido
	 * @return el pedido confirmado
	 */
	@Operation(summary = "Confirmar pedido", description = "Invoca sp_confirmar_pedido: vuelve a validar las aves libres y las aparta.")
	@ApiResponse(responseCode = "200", description = "Pedido confirmado")
	@ApiResponse(responseCode = "409", description = "Ya no hay aves suficientes o el estado no lo permite", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PatchMapping("/{id}/confirmar")
	public ResponseEntity<PedidoDTO> confirmar(@Parameter(description = "Pedido") @PathVariable Long id) {
		return ResponseEntity.ok(pedidoService.confirmar(id));
	}

	/**
	 * Cancela un pedido.
	 *
	 * @param id pedido
	 * @return el pedido cancelado
	 */
	@Operation(summary = "Cancelar pedido", description = "Invoca sp_cancelar_pedido. Un pedido con venta no se cancela.")
	@ApiResponse(responseCode = "200", description = "Pedido cancelado")
	@ApiResponse(responseCode = "409", description = "El pedido ya tiene venta o ya terminó", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PatchMapping("/{id}/cancelar")
	public ResponseEntity<PedidoDTO> cancelar(@Parameter(description = "Pedido") @PathVariable Long id) {
		return ResponseEntity.ok(pedidoService.cancelar(id));
	}
}
