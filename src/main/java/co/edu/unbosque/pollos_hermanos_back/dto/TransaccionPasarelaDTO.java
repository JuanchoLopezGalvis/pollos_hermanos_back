package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Transacción consultada directamente a la pasarela.
 *
 * @param id              identificador de la transacción
 * @param estado          APPROVED, DECLINED, VOIDED, ERROR o PENDING
 * @param referencia      referencia enviada al iniciar el pago
 * @param montoEnCentavos monto cobrado en centavos
 * @param moneda          moneda
 */
public record TransaccionPasarelaDTO(String id, String estado, String referencia, long montoEnCentavos, String moneda) {
}
