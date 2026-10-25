package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

/**
 * Ocupación de un galpón, leída de {@code v_galpon_ocupacion}.
 *
 * @param idGalpon        galpón
 * @param nombre          nombre del galpón
 * @param capacidadMaxima aves que caben
 * @param estado          estado del galpón
 * @param lotesActivos    lotes en engorde o listos para venta
 * @param avesAlojadas    aves vivas sin vender de sus lotes activos
 * @param capacidadLibre  aves que aún caben
 * @param proximaSalida   fecha estimada de venta más cercana, o {@code null}
 */
public record GalponOcupacionDTO(Long idGalpon, String nombre, Integer capacidadMaxima, String estado, Integer lotesActivos,
		Integer avesAlojadas, Integer capacidadLibre, LocalDate proximaSalida) {
}
