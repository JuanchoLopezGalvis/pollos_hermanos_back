package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o modificar un producto sanitario (S18).
 *
 * @param nombre       nombre del producto
 * @param tipo         {@code VITAMINA} o {@code MEDICAMENTO}
 * @param presentacion presentación
 * @param unidadMedida unidad en que se dosifica
 * @param idProveedor  proveedor de insumos sanitarios
 * @param activo       si es {@code null}, no cambia (al crear queda activo)
 */
public record RegistroProductoSanitarioDTO(
		@NotBlank(message = "es obligatorio") @Size(max = 100, message = "admite máximo 100 caracteres") String nombre,
		@NotNull(message = "es obligatorio") @Pattern(regexp = "VITAMINA|MEDICAMENTO", message = "debe ser VITAMINA o MEDICAMENTO") String tipo,
		@NotBlank(message = "es obligatoria") @Size(max = 80, message = "admite máximo 80 caracteres") String presentacion,
		@NotBlank(message = "es obligatoria") @Size(max = 20, message = "admite máximo 20 caracteres") String unidadMedida,
		@NotNull(message = "es obligatorio") Long idProveedor,
		Boolean activo) {
}
