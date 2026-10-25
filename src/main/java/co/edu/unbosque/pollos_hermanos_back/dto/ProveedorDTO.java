package co.edu.unbosque.pollos_hermanos_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Proveedor de alimento, de insumos sanitarios o de ambos (S21).
 *
 * @param idProveedor        identificador
 * @param nit                NIT, único
 * @param nombre             razón social
 * @param telefono           teléfono, opcional
 * @param correo             correo, opcional
 * @param direccion          dirección, opcional
 * @param suministraAlimento suministra alimento
 * @param suministraSanidad  suministra vitaminas y medicamentos
 * @param activo             {@code false} si ya no se le compra; si es
 *                           {@code null} al crear, queda activo
 */
public record ProveedorDTO(
		@JsonProperty(access = JsonProperty.Access.READ_ONLY) Long idProveedor,
		@NotBlank(message = "es obligatorio") @Size(max = 20, message = "admite máximo 20 caracteres") String nit,
		@NotBlank(message = "es obligatorio") @Size(max = 120, message = "admite máximo 120 caracteres") String nombre,
		@Size(max = 20, message = "admite máximo 20 caracteres") String telefono,
		@Email(message = "no es un correo válido") @Size(max = 120, message = "admite máximo 120 caracteres") String correo,
		@Size(max = 200, message = "admite máximo 200 caracteres") String direccion,
		boolean suministraAlimento,
		boolean suministraSanidad,
		Boolean activo) {
}
