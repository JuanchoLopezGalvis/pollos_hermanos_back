package co.edu.unbosque.pollos_hermanos_back.dto;

import jakarta.validation.groups.Default;

/**
 * Grupo de Bean Validation para las reglas de formato que solo aplican al crear
 * un registro, como la contraseña obligatoria de un usuario nuevo.
 */
public interface AlCrear extends Default {
}
