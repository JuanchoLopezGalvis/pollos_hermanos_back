package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Vitamina o medicamento del catálogo, leído de {@code v_producto_sanitario}.
 *
 * @param idProducto    identificador
 * @param nombre        nombre del producto
 * @param tipo          {@code VITAMINA} o {@code MEDICAMENTO}
 * @param presentacion  presentación, por ejemplo "Frasco 1 L"
 * @param unidadMedida  unidad en que se dosifica (ml, g, dosis)
 * @param idProveedor   proveedor
 * @param proveedor     nombre del proveedor
 * @param activo        {@code false} si ya no se usa
 * @param costoPromedio costo promedio ponderado por unidad, o {@code null} sin compras
 */
public record ProductoSanitarioDTO(Long idProducto, String nombre, String tipo, String presentacion, String unidadMedida,
		Long idProveedor, String proveedor, Boolean activo, BigDecimal costoPromedio) {
}
