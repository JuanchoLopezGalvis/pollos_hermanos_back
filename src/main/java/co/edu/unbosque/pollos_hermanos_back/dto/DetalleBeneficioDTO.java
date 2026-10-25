package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;

/**
 * Participación de una beneficiadora en una jornada, leída de
 * {@code v_detalle_beneficio}.
 *
 * @param idJornada        jornada
 * @param idEmpleado       beneficiadora
 * @param beneficiadora    nombre de la beneficiadora
 * @param avesBeneficiadas pollos que benefició
 * @param tarifaPorAve     tarifa pagada por pollo
 * @param pago             pago de la jornada
 */
public record DetalleBeneficioDTO(Long idJornada, Long idEmpleado, String beneficiadora, Integer avesBeneficiadas,
		BigDecimal tarifaPorAve, BigDecimal pago) {
}
