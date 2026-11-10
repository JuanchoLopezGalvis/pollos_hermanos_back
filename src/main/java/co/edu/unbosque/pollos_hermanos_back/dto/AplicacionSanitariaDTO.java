package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aplicación sanitaria a un lote, leída de {@code v_aplicacion_sanitaria}.
 *
 * @param idAplicacion  identificador
 * @param fecha         fecha de la aplicación
 * @param idLote        lote
 * @param codigoLote    código del lote
 * @param idProducto    producto aplicado
 * @param producto      nombre del producto
 * @param unidadMedida  unidad de la dosis
 * @param dosis         cantidad aplicada
 * @param costoUnitario costo por unidad congelado al registrar
 * @param costoTotal    costo cargado al lote
 * @param idEmpleado    encargado de sanidad responsable
 * @param responsable   nombre del responsable
 * @param observaciones observaciones
 */
public record AplicacionSanitariaDTO(Long idAplicacion, LocalDate fecha, Long idLote, String codigoLote, Long idProducto,
		String producto, String unidadMedida, BigDecimal dosis, BigDecimal costoUnitario, BigDecimal costoTotal,
		Long idEmpleado, String responsable, String observaciones) {
}
