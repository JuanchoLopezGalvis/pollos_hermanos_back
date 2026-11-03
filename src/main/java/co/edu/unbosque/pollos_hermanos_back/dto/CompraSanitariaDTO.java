package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compra de un producto sanitario, leída de {@code v_compra_sanitaria}.
 *
 * @param idCompraSanitaria identificador
 * @param fecha             fecha de la compra
 * @param idProducto        producto
 * @param producto          nombre del producto
 * @param unidadMedida      unidad del producto
 * @param cantidad          cantidad comprada
 * @param costoUnitario     costo por unidad
 * @param total             valor de la compra
 */
public record CompraSanitariaDTO(Long idCompraSanitaria, LocalDate fecha, Long idProducto, String producto,
		String unidadMedida, BigDecimal cantidad, BigDecimal costoUnitario, BigDecimal total) {
}
