package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar una jornada de beneficio con
 * {@code sp_registrar_jornada_beneficio}: la jornada y su detalle se guardan en
 * una sola transacción.
 *
 * @param idLote        lote listo para venta o vendido
 * @param fecha         fecha; si es {@code null}, hoy
 * @param observaciones observaciones, opcional
 * @param detalle       beneficiadoras que participaron
 */
public record RegistroJornadaBeneficioDTO(
		@NotNull(message = "es obligatorio") Long idLote,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones,
		@NotEmpty(message = "debe incluir al menos una beneficiadora") List<@Valid LineaBeneficioDTO> detalle) {
}
