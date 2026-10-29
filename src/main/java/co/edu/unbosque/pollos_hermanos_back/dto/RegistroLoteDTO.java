package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un lote nuevo con {@code sp_registrar_lote}. Aquí solo se
 * valida el formato; la capacidad compartida del galpón y el ciclo de 35 a 45
 * días los valida PostgreSQL.
 *
 * @param codigo             código único del lote; se guarda en mayúsculas
 * @param idGalpon           galpón donde ingresan las aves
 * @param fechaIngreso       fecha de ingreso; no puede ser futura
 * @param cantidadInicial    aves que ingresan
 * @param costoPollito       costo unitario del pollito de un día
 * @param fechaEstimadaVenta fecha estimada de venta; si es {@code null}, la base
 *                           de datos usa el ingreso más 40 días
 */
public record RegistroLoteDTO(
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres")
		@Pattern(regexp = "^[A-Za-z0-9-]+$", message = "solo admite letras, números y guiones") String codigo,
		@NotNull(message = "es obligatorio") Long idGalpon,
		@NotNull(message = "es obligatoria") @PastOrPresent(message = "no puede ser futura") LocalDate fechaIngreso,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Max(value = 100000, message = "admite máximo 100.000 aves") Integer cantidadInicial,
		@NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo") @Digits(integer = 12, fraction = 2, message = "admite máximo 12 cifras enteras y 2 decimales") BigDecimal costoPollito,
		LocalDate fechaEstimadaVenta) {
}
