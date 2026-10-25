package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Resultado de {@code sp_registrar_mortalidad}.
 *
 * @param idMortalidad         identificador del registro creado
 * @param porcentajeMortalidad mortalidad acumulada del lote después del registro
 * @param alerta               {@code true} si la acumulada supera el 5% de
 *                             referencia
 */
public record MortalidadRegistradaDTO(Long idMortalidad, BigDecimal porcentajeMortalidad, boolean alerta) {
}
