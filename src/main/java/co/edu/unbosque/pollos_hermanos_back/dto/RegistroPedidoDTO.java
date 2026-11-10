package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un pedido con {@code sp_crear_pedido}.
 *
 * @param idCliente            cliente activo
 * @param fechaEstimadaEntrega fecha estimada de entrega
 * @param observaciones        observaciones, opcional
 * @param detalle              aves por lote
 */
public record RegistroPedidoDTO(
		@NotNull(message = "es obligatorio") Long idCliente,
		@NotNull(message = "es obligatoria") LocalDate fechaEstimadaEntrega,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones,
		@NotEmpty(message = "debe incluir aves de al menos un lote") List<@Valid LineaPedidoDTO> detalle) {
}
