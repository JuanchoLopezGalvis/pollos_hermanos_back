package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Elemento de un catálogo (cargo, tipo de cliente, método de pago, categoría de
 * gasto).
 *
 * @param id     identificador
 * @param codigo código estable que usan las reglas de la base de datos
 * @param nombre nombre visible
 */
public record CatalogoDTO(Long id, String codigo, String nombre) {
}
