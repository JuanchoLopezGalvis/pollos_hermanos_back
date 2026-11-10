package co.edu.unbosque.pollos_hermanos_back.dto;

import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado de un lote. Si la transición no es válida, PostgreSQL la rechaza.
 *
 * @param estado estado al que pasa el lote
 */
public record CambioEstadoLoteDTO(@NotNull(message = "es obligatorio") EstadoLote estado) {
}
