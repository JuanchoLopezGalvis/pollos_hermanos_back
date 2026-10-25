package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Venta con su saldo y su entrega, leída de {@code v_venta}.
 *
 * @param idVenta         identificador
 * @param idPedido        pedido del que nace
 * @param idCliente       cliente
 * @param cliente         nombre del cliente
 * @param fechaVenta      fecha de la venta
 * @param cantidadAves    aves vendidas
 * @param pesoTotalLibras peso total en libras
 * @param precioLibra     precio por libra fijado al vender
 * @param valorTotal      libras por precio
 * @param pagado          suma de los pagos
 * @param saldo           valor total menos pagos
 * @param fechaLimitePago fecha límite de pago
 * @param estado          PENDIENTE_PAGO, PAGADA o VENCIDA
 * @param vencida         {@code true} si tiene saldo después de la fecha límite
 * @param idEntrega       entrega, o {@code null}
 * @param estadoEntrega   estado de la entrega, o {@code null}
 */
public record VentaDTO(Long idVenta, Long idPedido, Long idCliente, String cliente, LocalDate fechaVenta, Integer cantidadAves,
		BigDecimal pesoTotalLibras, BigDecimal precioLibra, BigDecimal valorTotal, BigDecimal pagado, BigDecimal saldo,
		LocalDate fechaLimitePago, String estado, Boolean vencida, Long idEntrega, String estadoEntrega,
		String direccionCliente) {
}
