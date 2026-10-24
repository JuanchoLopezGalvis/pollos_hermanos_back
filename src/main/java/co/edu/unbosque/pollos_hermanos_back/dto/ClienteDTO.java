package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Cliente, leído de {@code v_cliente}.
 *
 * @param idCliente     identificador
 * @param idTipoCliente tipo de cliente
 * @param tipoCliente   nombre del tipo (tienda, restaurante, persona natural)
 * @param documento     documento o NIT
 * @param nombre        nombre o razón social
 * @param direccion     dirección
 * @param telefono      teléfono
 * @param correo        correo
 * @param activo        {@code false} si ya no compra
 */
public record ClienteDTO(Long idCliente, Long idTipoCliente, String tipoCliente, String documento, String nombre,
		String direccion, String telefono, String correo, Boolean activo) {
}
