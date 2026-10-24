package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Datos para abrir el checkout de la pasarela. El cliente paga en la página de
 * la pasarela: los datos de tarjeta nunca pasan por el backend.
 *
 * @param urlCheckout     URL completa del checkout, ya firmada
 * @param referencia      referencia única del intento de pago
 * @param montoEnCentavos monto en centavos de peso
 * @param moneda          moneda (COP)
 */
public record CheckoutPagoDTO(String urlCheckout, String referencia, long montoEnCentavos, String moneda) {
}
