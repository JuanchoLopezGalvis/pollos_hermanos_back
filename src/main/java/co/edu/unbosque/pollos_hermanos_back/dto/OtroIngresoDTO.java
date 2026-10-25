package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ingreso de un lote distinto de la venta de aves, leído de {@code v_otro_ingreso}.
 *
 * @param idOtroIngreso identificador
 * @param fecha         fecha del ingreso
 * @param idLote        lote
 * @param codigoLote    código del lote
 * @param monto         valor
 * @param descripcion   descripción, por ejemplo "Venta de gallinaza"
 */
public record OtroIngresoDTO(Long idOtroIngreso, LocalDate fecha, Long idLote, String codigoLote, BigDecimal monto,
		String descripcion) {
}
