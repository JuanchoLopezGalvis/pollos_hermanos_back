package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Datos para registrar una compra de alimento con {@code sp_registrar_compra_alimento}.
 *
 * @param idProveedor    proveedor de alimento
 * @param idTipoAlimento tipo de alimento
 * @param fecha          fecha de la compra; si es {@code null}, hoy
 * @param cantidadBultos bultos comprados
 * @param costoUnitario  costo de cada bulto
 */
public record RegistroCompraAlimentoDTO(
		@NotNull(message = "es obligatorio") Long idProveedor,
		@NotNull(message = "es obligatorio") Long idTipoAlimento,
		@PastOrPresent(message = "no puede ser futura") LocalDate fecha,
		@NotNull(message = "es obligatoria") @Positive(message = "debe ser mayor que cero") Integer cantidadBultos,
		@NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") BigDecimal costoUnitario) {
}
