package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;

/**
 * Retiro de un empleado.
 *
 * @param fechaRetiro fecha de retiro; si es {@code null}, se usa la de hoy
 */
public record RetiroEmpleadoDTO(@PastOrPresent(message = "no puede ser futura") LocalDate fechaRetiro) {
}
