package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Balance económico de un lote, leído de {@code v_rentabilidad_lote}. Ganancia
 * neta = ingresos totales - costos totales.
 *
 * @param idLote              lote
 * @param codigo              código del lote
 * @param estado              estado del lote
 * @param costoPollitos       pollitos de un día
 * @param costoAlimento       alimento consumido al costo promedio ponderado
 * @param costoSanidad        vitaminas y medicamentos aplicados
 * @param costoBeneficio      pagos a las beneficiadoras
 * @param otrosGastos         mano de obra, servicios y transporte del lote
 * @param costoTotal          suma de los costos
 * @param avesVendidas        aves vendidas
 * @param librasVendidas      libras atribuidas al lote
 * @param pesoPromedioLibras  peso promedio de salida por ave
 * @param ingresoVentas       ingreso por venta de aves atribuido al lote
 * @param otrosIngresos       otros ingresos del lote
 * @param ingresoTotal        ventas más otros ingresos
 * @param ganancia            ingreso total menos costo total (negativa si hubo pérdida)
 * @param costoPorAveVendida  costo total sobre aves vendidas
 * @param costoPorLibra       costo total sobre libras vendidas
 * @param precioPromedioLibra ingreso por ventas sobre libras vendidas
 * @param margenPorcentaje    ganancia sobre ingreso total
 */
public record RentabilidadLoteDTO(Long idLote, String codigo, String estado, BigDecimal costoPollitos,
		BigDecimal costoAlimento, BigDecimal costoSanidad, BigDecimal costoBeneficio, BigDecimal otrosGastos,
		BigDecimal costoTotal, Integer avesVendidas, BigDecimal librasVendidas, BigDecimal pesoPromedioLibras,
		BigDecimal ingresoVentas, BigDecimal otrosIngresos, BigDecimal ingresoTotal, BigDecimal ganancia,
		BigDecimal costoPorAveVendida, BigDecimal costoPorLibra, BigDecimal precioPromedioLibra, BigDecimal margenPorcentaje) {
}
