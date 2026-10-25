package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Aves que un cliente puede pedir en este momento. No dice de qué lotes son.
 *
 * @param avesDisponibles aves libres de los lotes listos para venta
 */
public record DisponibilidadDTO(int avesDisponibles) {
}
