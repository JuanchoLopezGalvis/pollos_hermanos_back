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
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CarteraVencidaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroVentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoCarteraDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentasClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Ventas, pagos, entregas y cartera. Registrar ventas y entregas requiere
 * {@code venta.gestionar}; registrar pagos requiere {@code pago.registrar};
 * consultar requiere alguno de esos permisos o {@code cartera.ver}.
 */
@RestController
@Tag(name = "Ventas, pagos y cartera", description = "Ventas desde pedidos confirmados, pagos parciales, entregas y cartera vencida")
public class VentaController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('venta.gestionar', 'pago.registrar', 'cartera.ver')";

	/** Permiso para registrar ventas y entregas. */
	private static final String GESTIONAR = "@permisos.tiene('venta.gestionar')";

	/** Permiso para registrar pagos. */
	private static final String PAGAR = "@permisos.tiene('pago.registrar')";

	/** Permiso para la cartera. */
	private static final String CARTERA = "@permisos.tieneAlguno('cartera.ver', 'pago.registrar')";

	/** Servicio de ventas. */
	private final VentaService ventaService;

	/**
	 * Crea el controlador.
	 *
	 * @param ventaService servicio de ventas
	 */
	public VentaController(VentaService ventaService) {
		this.ventaService = ventaService;
	}

	/**
	 * Lista las ventas.
	 *
	 * @return ventas con saldo y entrega
	 */
	@Operation(summary = "Listar ventas")
	@ApiResponse(responseCode = "200", description = "Ventas con saldo, estado y entrega")
	@PreAuthorize(VER)
	@GetMapping("/ventas")
	public ResponseEntity<List<VentaDTO>> obtenerVentas() {
		return ResponseEntity.ok(ventaService.obtenerVentas());
	}

	/**
	 * Obtiene una venta.
	 *
	 * @param id venta
	 * @return la venta
	 */
	@Operation(summary = "Obtener venta")
	@ApiResponse(responseCode = "200", description = "Venta")
	@ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping("/ventas/{id}")
	public ResponseEntity<VentaDTO> obtenerVenta(@Parameter(description = "Venta") @PathVariable Long id) {
		return ResponseEntity.ok(ventaService.obtenerVenta(id));
	}

	/**
	 * Registra la venta de un pedido confirmado.
	 *
	 * @param dto datos de la venta
	 * @return la venta registrada
	 */
	@Operation(summary = "Registrar venta", description = "Invoca sp_registrar_venta. El pedido debe estar confirmado y sus lotes listos para venta; se pesa en libras, el precio por libra queda fijo y los lotes sin aves pasan a vendidos.")
	@ApiResponse(responseCode = "201", description = "Venta registrada")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó la venta", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping("/ventas")
	public ResponseEntity<VentaDTO> registrarVenta(@Valid @RequestBody RegistroVentaDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarVenta(dto));
	}

	/**
	 * Lista los pagos de una venta.
	 *
	 * @param id venta
	 * @return pagos
	 */
	@Operation(summary = "Pagos de una venta")
	@ApiResponse(responseCode = "200", description = "Pagos")
	@PreAuthorize(VER)
	@GetMapping("/ventas/{id}/pagos")
	public ResponseEntity<List<PagoDTO>> obtenerPagos(@Parameter(description = "Venta") @PathVariable Long id) {
		return ResponseEntity.ok(ventaService.obtenerPagos(id));
	}

	/**
	 * Registra un pago manual.
	 *
	 * @param id  venta
	 * @param dto método, fecha y monto
	 * @return la venta con su saldo actualizado
	 */
	@Operation(summary = "Registrar pago", description = "Invoca sp_registrar_pago para pagos en efectivo, transferencia o crédito. El monto no puede superar el saldo.")
	@ApiResponse(responseCode = "201", description = "Pago registrado")
	@ApiResponse(responseCode = "409", description = "Monto mayor que el saldo o venta pagada", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(PAGAR)
	@PostMapping("/ventas/{id}/pagos")
	public ResponseEntity<VentaDTO> registrarPago(@Parameter(description = "Venta") @PathVariable Long id,
			@Valid @RequestBody RegistroPagoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarPago(id, dto));
	}

	/**
	 * Programa la entrega de una venta.
	 *
	 * @param id  venta
	 * @param dto transportista y dirección
	 * @return la entrega
	 */
	@Operation(summary = "Programar entrega", description = "Invoca sp_registrar_entrega. Una entrega por venta, a cargo de un transportista activo.")
	@ApiResponse(responseCode = "201", description = "Entrega programada")
	@ApiResponse(responseCode = "409", description = "Ya tiene entrega o el responsable no es transportista", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping("/ventas/{id}/entrega")
	public ResponseEntity<EntregaDTO> registrarEntrega(@Parameter(description = "Venta") @PathVariable Long id,
			@Valid @RequestBody RegistroEntregaDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrarEntrega(id, dto));
	}

	/**
	 * Lista las entregas.
	 *
	 * @return entregas
	 */
	@Operation(summary = "Listar entregas")
	@ApiResponse(responseCode = "200", description = "Entregas")
	@PreAuthorize(VER)
	@GetMapping("/entregas")
	public ResponseEntity<List<EntregaDTO>> obtenerEntregas() {
		return ResponseEntity.ok(ventaService.obtenerEntregas());
	}

	/**
	 * Marca una entrega como entregada o devuelta.
	 *
	 * @param id  entrega
	 * @param dto resultado
	 * @return la entrega actualizada
	 */
	@Operation(summary = "Registrar resultado de la entrega", description = "Invoca sp_actualizar_entrega. Al entregar, el pedido queda entregado.")
	@ApiResponse(responseCode = "200", description = "Entrega actualizada")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el cambio", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PatchMapping("/entregas/{id}")
	public ResponseEntity<EntregaDTO> actualizarEntrega(@Parameter(description = "Entrega") @PathVariable Long id,
			@Valid @RequestBody ActualizacionEntregaDTO dto) {
		return ResponseEntity.ok(ventaService.actualizarEntrega(id, dto));
	}

	/**
	 * Muestra la cartera vencida.
	 *
	 * @return ventas con saldo después de su fecha límite
	 */
	@Operation(summary = "Cartera vencida", description = "Lee fn_cartera_vencida.")
	@ApiResponse(responseCode = "200", description = "Cartera vencida")
	@PreAuthorize(CARTERA)
	@GetMapping("/cartera/vencida")
	public ResponseEntity<List<CarteraVencidaDTO>> obtenerCarteraVencida() {
		return ResponseEntity.ok(ventaService.obtenerCarteraVencida());
	}

	/**
	 * Marca la cartera vencida ahora, sin esperar la tarea diaria.
	 *
	 * @return ventas marcadas
	 */
	@Operation(summary = "Marcar cartera vencida", description = "Invoca sp_marcar_cartera_vencida, que también corre cada día de forma automática.")
	@ApiResponse(responseCode = "200", description = "Ventas marcadas")
	@PreAuthorize(CARTERA)
	@PostMapping("/cartera/marcar-vencidas")
	public ResponseEntity<ResultadoCarteraDTO> marcarCarteraVencida() {
		return ResponseEntity.ok(ventaService.marcarCarteraVencida());
	}

	/**
	 * Muestra el reporte de ventas por cliente.
	 *
	 * @return ventas por cliente
	 */
	@Operation(summary = "Ventas por cliente")
	@ApiResponse(responseCode = "200", description = "Ventas, valor vendido y saldo por cliente")
	@PreAuthorize(CARTERA)
	@GetMapping("/reportes/ventas-por-cliente")
	public ResponseEntity<List<VentasClienteDTO>> obtenerVentasPorCliente() {
		return ResponseEntity.ok(ventaService.obtenerVentasPorCliente());
	}
}
