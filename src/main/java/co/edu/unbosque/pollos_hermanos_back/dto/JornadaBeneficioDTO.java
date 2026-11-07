package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Jornada de beneficio de un lote con sus totales, leída de
 * {@code v_jornada_beneficio}.
 *
 * @param idJornada        identificador
 * @param fecha            fecha de la jornada
 * @param idLote           lote
 * @param codigoLote       código del lote
 * @param observaciones    observaciones
 * @param avesBeneficiadas aves beneficiadas en la jornada
 * @param beneficiadoras   personas que participaron
 * @param costoTotal       pago total a las beneficiadoras
 */
public record JornadaBeneficioDTO(Long idJornada, LocalDate fecha, Long idLote, String codigoLote, String observaciones,
		Integer avesBeneficiadas, Integer beneficiadoras, BigDecimal costoTotal) {
}
