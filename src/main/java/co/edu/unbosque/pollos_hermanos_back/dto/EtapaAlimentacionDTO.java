package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Etapa de alimentación con su rango de días de edad del lote.
 *
 * @param idEtapa   identificador
 * @param nombre    nombre de la etapa
 * @param orden     orden dentro del ciclo
 * @param diaInicio primer día de la etapa
 * @param diaFin    último día, o {@code null} si llega hasta la venta
 */
public record EtapaAlimentacionDTO(Long idEtapa, String nombre, Integer orden, Integer diaInicio, Integer diaFin) {
}
