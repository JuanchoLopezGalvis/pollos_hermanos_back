package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Reporte de ventas por cliente, leído de {@code v_ventas_por_cliente}.
 *
 * @param idCliente      cliente
 * @param cliente        nombre del cliente
 * @param tipoCliente    tipo de cliente
 * @param ventas         número de ventas
 * @param valorVendido   valor total vendido
 * @param saldoPendiente saldo por cobrar
 */
public record VentasClienteDTO(Long idCliente, String cliente, String tipoCliente, Integer ventas, BigDecimal valorVendido,
		BigDecimal saldoPendiente) {
}
