package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Aves de un lote dentro de un pedido.
 *
 * @param idPedido     pedido
 * @param idLote       lote
 * @param codigoLote   código del lote
 * @param cantidadAves aves pedidas de ese lote
 */
public record DetallePedidoDTO(Long idPedido, Long idLote, String codigoLote, Integer cantidadAves) {
}
