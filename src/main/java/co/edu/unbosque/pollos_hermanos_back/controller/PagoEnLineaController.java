package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.service.EventosWompiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Avisos de Wompi. Es una ruta pública porque la llama Wompi, no un usuario:
 * en lugar del token se verifica la firma del aviso. Los pagos los inicia el
 * cliente desde {@code /portal}.
 */
@RestController
@RequestMapping("/pagos-en-linea")
@Tag(name = "Pago en línea", description = "Avisos de Wompi (sandbox) cuando cambia una transacción")
public class PagoEnLineaController {

	/** Procesa los avisos de Wompi. */
	private final EventosWompiService eventosWompiService;

	/**
	 * Crea el controlador.
	 *
	 * @param eventosWompiService avisos de Wompi
	 */
	public PagoEnLineaController(EventosWompiService eventosWompiService) {
		this.eventosWompiService = eventosWompiService;
	}

	/**
	 * Recibe el aviso de Wompi de una transacción actualizada y registra el pago
	 * si quedó aprobada.
	 *
	 * @param evento cuerpo del aviso
	 * @return resultado del pago; vacío si el evento no es de una transacción
	 */
	@Operation(summary = "Aviso de Wompi", description = "URL de eventos configurada en el panel de Wompi. Verifica la firma con WOMPI_SECRETO_EVENTOS, consulta la transacción en Wompi y registra el pago si está aprobada. Recibirlo varias veces no duplica el pago.")
	@ApiResponse(responseCode = "200", description = "Aviso procesado")
	@ApiResponse(responseCode = "403", description = "La firma no corresponde a Wompi", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@SecurityRequirements
	@PostMapping("/eventos")
	public ResponseEntity<ResultadoPagoEnLineaDTO> recibirEvento(@RequestBody Map<String, Object> evento) {
		ResultadoPagoEnLineaDTO resultado = eventosWompiService.procesar(evento);
		return resultado == null ? ResponseEntity.ok().build() : ResponseEntity.ok(resultado);
	}
}
