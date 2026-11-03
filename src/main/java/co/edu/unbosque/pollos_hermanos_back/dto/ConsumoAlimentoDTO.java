package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consumo de alimento de un lote, leído de {@code v_consumo_alimento}.
 *
 * @param idConsumoAlimento identificador
 * @param fecha             día del consumo
 * @param idLote            lote
 * @param codigoLote        código del lote
 * @param edadDias          edad del lote ese día
 * @param idTipoAlimento    tipo de alimento
 * @param tipoAlimento      nombre del alimento
 * @param cantidadBultos    bultos consumidos
 * @param costoUnitario     costo promedio del bulto congelado al registrar
 * @param costoTotal        costo cargado al lote
 */
public record ConsumoAlimentoDTO(Long idConsumoAlimento, LocalDate fecha, Long idLote, String codigoLote, Integer edadDias,
		Long idTipoAlimento, String tipoAlimento, BigDecimal cantidadBultos, BigDecimal costoUnitario, BigDecimal costoTotal) {
}
