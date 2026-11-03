package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compra de alimento, leída de {@code v_compra_alimento}.
 *
 * @param idCompraAlimento identificador
 * @param fecha            fecha de la compra
 * @param idProveedor      proveedor
 * @param proveedor        nombre del proveedor
 * @param idTipoAlimento   tipo de alimento
 * @param tipoAlimento     nombre del alimento
 * @param cantidadBultos   bultos comprados
 * @param costoUnitario    costo de cada bulto
 * @param total            valor de la compra
 */
public record CompraAlimentoDTO(Long idCompraAlimento, LocalDate fecha, Long idProveedor, String proveedor,
		Long idTipoAlimento, String tipoAlimento, Integer cantidadBultos, BigDecimal costoUnitario, BigDecimal total) {
}
