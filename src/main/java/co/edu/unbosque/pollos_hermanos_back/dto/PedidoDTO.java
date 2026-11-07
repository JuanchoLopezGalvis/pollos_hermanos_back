package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

/**
 * Pedido con sus totales, leído de {@code v_pedido}.
 *
 * @param idPedido             identificador
 * @param idCliente            cliente
 * @param cliente              nombre del cliente
 * @param fechaPedido          fecha del pedido
 * @param fechaEstimadaEntrega fecha estimada de entrega
 * @param estado               PENDIENTE, CONFIRMADO, ENTREGADO o CANCELADO
 * @param observaciones        observaciones
 * @param totalAves            aves pedidas (suma del detalle)
 * @param lotes                lotes y cantidades, en texto
 * @param idVenta              venta generada, o {@code null}
 */
public record PedidoDTO(Long idPedido, Long idCliente, String cliente, LocalDate fechaPedido, LocalDate fechaEstimadaEntrega,
		String estado, String observaciones, Integer totalAves, String lotes, Long idVenta) {
}
