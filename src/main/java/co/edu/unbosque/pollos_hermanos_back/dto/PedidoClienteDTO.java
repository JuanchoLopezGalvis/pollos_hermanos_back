package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Pedido que hace un cliente desde el portal: solo la cantidad de aves. Los
 * lotes los elige PostgreSQL por antigüedad (D22).
 *
 * @param cantidadAves         aves que pide
 * @param fechaEstimadaEntrega fecha en que quiere recibirlas
 * @param observaciones        indicaciones para la finca
 */
public record PedidoClienteDTO(
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero")
		@Max(value = 100000, message = "admite máximo 100.000 aves") Integer cantidadAves,
		@NotNull(message = "es obligatoria") @FutureOrPresent(message = "no puede ser anterior a hoy") LocalDate fechaEstimadaEntrega,
		@Size(max = 300, message = "admite máximo 300 caracteres") String observaciones) {
}
