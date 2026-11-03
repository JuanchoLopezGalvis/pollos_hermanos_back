package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Datos corregibles de un lote. El código, el galpón y la fecha de ingreso no
 * cambian, porque identifican el lote también en MongoDB.
 *
 * @param cantidadInicial    aves con las que ingresó el lote
 * @param costoPollito       costo unitario del pollito de un día
 * @param fechaEstimadaVenta fecha estimada de venta
 */
public record ActualizacionLoteDTO(
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") @Max(value = 100000, message = "admite máximo 100.000 aves") Integer cantidadInicial,
		@NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo") @Digits(integer = 12, fraction = 2, message = "admite máximo 12 cifras enteras y 2 decimales") BigDecimal costoPollito,
		@NotNull(message = "es obligatoria") LocalDate fechaEstimadaVenta) {
}
