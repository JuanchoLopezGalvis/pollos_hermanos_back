package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Punto de la curva de crecimiento semanal de un lote.
 *
 * @param semana             semana de vida del lote (1 = días 1 a 7)
 * @param pesoPromedioGramos peso promedio de la semana en gramos
 * @param registros          cantidad de seguimientos usados en el promedio
 */
public record CurvaCrecimientoSemanalDTO(int semana, double pesoPromedioGramos, int registros) {
}
