package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar una aplicación con {@code sp_registrar_aplicacion_sanitaria}.
 * PostgreSQL exige que el responsable sea un encargado de sanidad activo (S25).
 *
 * @param idLote        lote
 * @param idProducto    producto aplicado
 * @param idEmpleado    encargado de sanidad responsable
 * @param fecha         fecha; si es {@code null}, hoy
 * @param dosis         cantidad aplicada
 * @param observaciones observaciones, opcional
 */
public record RegistroAplicacionSanitariaDTO(
		@NotNull(message = "es obligatorio") Long idLote,
		@NotNull(message = "es obligatorio") Long idProducto,
		@NotNull(message = "es obligatorio") Long idEmpleado,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Digits(integer = 8, fraction = 2, message = "admite máximo 8 cifras enteras y 2 decimales") BigDecimal dosis,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones) {
}
