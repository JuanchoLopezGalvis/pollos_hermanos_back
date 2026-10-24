package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para programar la entrega de una venta con {@code sp_registrar_entrega}.
 *
 * @param idEmpleado    transportista activo (S26)
 * @param direccion     dirección; si está vacía, la del cliente
 * @param observaciones observaciones, opcional
 */
public record RegistroEntregaDTO(
		@NotNull(message = "es obligatorio") Long idEmpleado,
		@Size(max = 200, message = "admite máximo 200 caracteres") String direccion,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones) {
}
