package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

/**
 * Entrega de una venta, leída de {@code v_entrega}.
 *
 * @param idEntrega     identificador
 * @param idVenta       venta
 * @param cliente       nombre del cliente
 * @param idEmpleado    transportista
 * @param transportista nombre del transportista
 * @param direccion     dirección de entrega
 * @param fechaEntrega  fecha real de entrega, o {@code null}
 * @param estado        PENDIENTE, ENTREGADO o DEVUELTO
 * @param observaciones observaciones
 */
public record EntregaDTO(Long idEntrega, Long idVenta, String cliente, Long idEmpleado, String transportista,
		String direccion, LocalDate fechaEntrega, String estado, String observaciones) {
}
