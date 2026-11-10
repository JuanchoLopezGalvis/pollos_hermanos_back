package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDate;

/**
 * Empleado con su cargo, leído de la vista {@code v_empleado}.
 *
 * @param idEmpleado   identificador
 * @param documento    documento de identidad
 * @param nombres      nombres
 * @param apellidos    apellidos
 * @param telefono     teléfono de contacto
 * @param idCargo      identificador del cargo
 * @param codigoCargo  código del cargo, por ejemplo {@code TRANSPORTISTA}
 * @param cargo        nombre del cargo
 * @param fechaIngreso fecha de ingreso a la finca
 * @param fechaRetiro  fecha de retiro, o {@code null} si está activo
 * @param activo       {@code false} si está retirado
 */
public record EmpleadoDTO(Long idEmpleado, String documento, String nombres, String apellidos, String telefono,
		Long idCargo, String codigoCargo, String cargo, LocalDate fechaIngreso, LocalDate fechaRetiro, Boolean activo) {
}
