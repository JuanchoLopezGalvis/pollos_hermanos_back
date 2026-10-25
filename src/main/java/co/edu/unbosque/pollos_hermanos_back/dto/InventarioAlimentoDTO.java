package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Tipo de alimento con su inventario, leído de {@code v_inventario_alimento}.
 *
 * @param idTipoAlimento    identificador del tipo de alimento
 * @param nombre            nombre del alimento
 * @param etapa             etapa a la que corresponde
 * @param diaInicio         primer día de la etapa
 * @param diaFin            último día de la etapa, o {@code null}
 * @param pesoBultoKg       peso del bulto
 * @param activo            {@code false} si ya no se usa
 * @param bultosComprados   bultos comprados en total
 * @param bultosConsumidos  bultos consumidos en total
 * @param bultosDisponibles bultos en bodega
 * @param costoPromedio     costo promedio ponderado del bulto, o {@code null} sin compras
 */
public record InventarioAlimentoDTO(Long idTipoAlimento, String nombre, String etapa, Integer diaInicio, Integer diaFin,
		BigDecimal pesoBultoKg, Boolean activo, BigDecimal bultosComprados, BigDecimal bultosConsumidos,
		BigDecimal bultosDisponibles, BigDecimal costoPromedio) {
}
