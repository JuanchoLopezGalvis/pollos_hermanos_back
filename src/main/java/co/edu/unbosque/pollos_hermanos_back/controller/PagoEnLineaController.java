package co.edu.unbosque.pollos_hermanos_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.CheckoutPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ConfirmacionPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.service.PagoEnLineaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Pago en línea con la pasarela en ambiente de pruebas. Requiere
 * {@code pago.registrar}.
 */
@RestController
@RequestMapping("/pagos-en-linea")
@Tag(name = "Pago en línea", description = "Checkout de Wompi en sandbox: sin dinero real y sin datos de tarjeta en el sistema")
public class PagoEnLineaController {

	/** Permiso para pagar. */
	private static final String PAGAR = "@permisos.tiene('pago.registrar')";

	/** Servicio de pago en línea. */
	private final PagoEnLineaService pagoEnLineaService;

	/**
	 * Crea el controlador.
	 *
	 * @param pagoEnLineaService servicio de pago en línea
	 */
	public PagoEnLineaController(PagoEnLineaService pagoEnLineaService) {
		this.pagoEnLineaService = pagoEnLineaService;
	}

	/**
	 * Prepara el checkout para pagar una venta.
	 *
	 * @param idVenta venta
	 * @param dto     monto y URL de retorno
	 * @return URL firmada del checkout
	 */
	@Operation(summary = "Iniciar pago en línea", description = "Devuelve la URL firmada del checkout de Wompi. El monto no puede superar el saldo.")
	@ApiResponse(responseCode = "200", description = "Checkout listo")
	@ApiResponse(responseCode = "409", description = "Pasarela sin configurar, venta pagada o monto mayor que el saldo", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(PAGAR)
	@PostMapping("/ventas/{idVenta}")
	public ResponseEntity<CheckoutPagoDTO> iniciar(@Parameter(description = "Venta") @PathVariable Long idVenta,
			@Valid @RequestBody IniciarPagoEnLineaDTO dto) {
		return ResponseEntity.ok(pagoEnLineaService.iniciar(idVenta, dto));
	}

	/**
	 * Confirma un pago al volver de la pasarela.
	 *
	 * @param dto transacción devuelta por la pasarela
	 * @return resultado del pago
	 */
	@Operation(summary = "Confirmar pago en línea", description = "Consulta la transacción directamente en Wompi y, si está aprobada, la registra con sp_registrar_pago. Confirmar dos veces no duplica el pago.")
	@ApiResponse(responseCode = "200", description = "Resultado del pago")
	@ApiResponse(responseCode = "409", description = "No se pudo consultar la pasarela o la base de datos rechazó el pago", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(PAGAR)
	@PostMapping("/confirmar")
	public ResponseEntity<ResultadoPagoEnLineaDTO> confirmar(@Valid @RequestBody ConfirmacionPagoEnLineaDTO dto) {
		return ResponseEntity.ok(pagoEnLineaService.confirmar(dto.idTransaccion()));
	}
}
