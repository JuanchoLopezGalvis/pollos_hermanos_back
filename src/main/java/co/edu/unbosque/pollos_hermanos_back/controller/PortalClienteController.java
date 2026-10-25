package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.CheckoutPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ConfirmacionPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.DisponibilidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.service.PortalClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Portal de clientes (D24). Requiere {@code portal.cliente}, el único permiso
 * del rol Cliente. Cada ruta trabaja solo con los datos del cliente
 * autenticado: no recibe el id del cliente.
 */
@RestController
@RequestMapping("/portal")
@PreAuthorize("@permisos.tiene('portal.cliente')")
@Tag(name = "Portal de clientes", description = "Pedidos y pagos del cliente que inició sesión")
public class PortalClienteController {

	/** Servicio del portal. */
	private final PortalClienteService portalService;

	/**
	 * Crea el controlador.
	 *
	 * @param portalService servicio del portal
	 */
	public PortalClienteController(PortalClienteService portalService) {
		this.portalService = portalService;
	}

	/**
	 * Datos del cliente autenticado.
	 *
	 * @return el cliente
	 */
	@Operation(summary = "Mis datos")
	@ApiResponse(responseCode = "200", description = "Datos del cliente")
	@ApiResponse(responseCode = "404", description = "El usuario no está vinculado a un cliente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@GetMapping("/perfil")
	public ResponseEntity<ClienteDTO> obtenerPerfil() {
		return ResponseEntity.ok(portalService.perfil());
	}

	/**
	 * Aves que se pueden pedir ahora.
	 *
	 * @return aves disponibles, sin los lotes
	 */
	@Operation(summary = "Aves disponibles", description = "Total de aves libres de los lotes listos para venta. No dice de qué lotes son.")
	@ApiResponse(responseCode = "200", description = "Aves disponibles")
	@GetMapping("/disponibilidad")
	public ResponseEntity<DisponibilidadDTO> obtenerDisponibilidad() {
		return ResponseEntity.ok(portalService.disponibilidad());
	}

	/**
	 * Pedidos del cliente.
	 *
	 * @return pedidos, sin los lotes
	 */
	@Operation(summary = "Mis pedidos")
	@ApiResponse(responseCode = "200", description = "Pedidos del cliente")
	@GetMapping("/pedidos")
	public ResponseEntity<List<PedidoDTO>> obtenerPedidos() {
		return ResponseEntity.ok(portalService.pedidos());
	}

	/**
	 * Pide una cantidad de aves.
	 *
	 * @param dto cantidad, fecha deseada y observaciones
	 * @return el pedido, pendiente de aprobación
	 */
	@Operation(summary = "Hacer un pedido", description = "El sistema toma las aves del lote que más tiempo lleva disponible y sigue con el siguiente. Si no alcanzan, rechaza el pedido de inmediato.")
	@ApiResponse(responseCode = "201", description = "Pedido creado, pendiente de aprobación")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "No hay aves suficientes", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PostMapping("/pedidos")
	public ResponseEntity<PedidoDTO> crearPedido(@Valid @RequestBody PedidoClienteDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(portalService.crearPedido(dto));
	}

	/**
	 * Cancela un pedido pendiente.
	 *
	 * @param id pedido
	 * @return el pedido cancelado
	 */
	@Operation(summary = "Cancelar mi pedido", description = "Solo mientras la finca no lo haya aprobado. Libera las aves.")
	@ApiResponse(responseCode = "200", description = "Pedido cancelado")
	@ApiResponse(responseCode = "404", description = "No es un pedido del cliente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "El pedido ya no está pendiente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PostMapping("/pedidos/{id}/cancelar")
	public ResponseEntity<PedidoDTO> cancelarPedido(@Parameter(description = "Pedido") @PathVariable Long id) {
		return ResponseEntity.ok(portalService.cancelarPedido(id));
	}

	/**
	 * Ventas del cliente con su valor y saldo.
	 *
	 * @return ventas del cliente
	 */
	@Operation(summary = "Mis compras", description = "Ventas registradas por la finca con el peso, el precio por libra, el valor y el saldo.")
	@ApiResponse(responseCode = "200", description = "Ventas del cliente")
	@GetMapping("/ventas")
	public ResponseEntity<List<VentaDTO>> obtenerVentas() {
		return ResponseEntity.ok(portalService.ventas());
	}

	/**
	 * Pagos de una venta propia.
	 *
	 * @param id venta
	 * @return pagos
	 */
	@Operation(summary = "Pagos de mi compra")
	@ApiResponse(responseCode = "200", description = "Pagos de la venta")
	@ApiResponse(responseCode = "404", description = "No es una venta del cliente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@GetMapping("/ventas/{id}/pagos")
	public ResponseEntity<List<PagoDTO>> obtenerPagos(@Parameter(description = "Venta") @PathVariable Long id) {
		return ResponseEntity.ok(portalService.pagos(id));
	}

	/**
	 * Prepara el pago con Wompi de una venta propia.
	 *
	 * @param id  venta
	 * @param dto monto y URL de retorno
	 * @return URL firmada del checkout
	 */
	@Operation(summary = "Pagar mi compra en línea", description = "Devuelve la URL firmada del checkout de Wompi (sandbox). El monto no puede superar el saldo.")
	@ApiResponse(responseCode = "200", description = "Checkout listo")
	@ApiResponse(responseCode = "404", description = "No es una venta del cliente", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "Venta pagada o monto mayor que el saldo", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PostMapping("/ventas/{id}/pago-en-linea")
	public ResponseEntity<CheckoutPagoDTO> iniciarPago(@Parameter(description = "Venta") @PathVariable Long id,
			@Valid @RequestBody IniciarPagoEnLineaDTO dto) {
		return ResponseEntity.ok(portalService.iniciarPago(id, dto));
	}

	/**
	 * Confirma un pago en línea al volver de Wompi.
	 *
	 * @param dto transacción de Wompi
	 * @return resultado del pago
	 */
	@Operation(summary = "Confirmar mi pago en línea", description = "Consulta la transacción en Wompi y la registra si está aprobada y es de una venta del cliente.")
	@ApiResponse(responseCode = "200", description = "Resultado del pago")
	@PostMapping("/pagos-en-linea/confirmar")
	public ResponseEntity<ResultadoPagoEnLineaDTO> confirmarPago(@Valid @RequestBody ConfirmacionPagoEnLineaDTO dto) {
		return ResponseEntity.ok(portalService.confirmarPago(dto.idTransaccion()));
	}
}
