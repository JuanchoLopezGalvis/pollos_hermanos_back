package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transacción que la pasarela devolvió al volver al frontend.
 *
 * @param idTransaccion identificador de la transacción en la pasarela
 */
public record ConfirmacionPagoEnLineaDTO(
		@NotBlank(message = "es obligatorio") @Size(max = 100, message = "admite máximo 100 caracteres") String idTransaccion) {
}
