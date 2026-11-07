package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Resultado de una entrega, registrado con {@code sp_actualizar_entrega}.
 *
 * @param estado        ENTREGADO o DEVUELTO
 * @param fechaEntrega  fecha real; si es {@code null} al entregar, hoy
 * @param observaciones observaciones, opcional
 */
public record ActualizacionEntregaDTO(
		@NotNull(message = "es obligatorio") @Pattern(regexp = "ENTREGADO|DEVUELTO", message = "debe ser ENTREGADO o DEVUELTO") String estado,
		@PastOrPresent(message = "no puede ser futura") LocalDate fechaEntrega,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones) {
}
