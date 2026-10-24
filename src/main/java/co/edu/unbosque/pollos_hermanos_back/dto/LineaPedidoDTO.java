package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Aves de un lote al crear un pedido.
 *
 * @param idLote       lote listo para venta
 * @param cantidadAves aves pedidas de ese lote
 */
public record LineaPedidoDTO(
		@NotNull(message = "es obligatorio") Long idLote,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") Integer cantidadAves) {
}
