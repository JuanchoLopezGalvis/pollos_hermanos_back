package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

/**
 * Operaciones CRUD básicas que implementan los servicios de las entidades de
 * PostgreSQL.
 * <p>
 * Los errores no se informan con códigos de retorno: cada método lanza una
 * excepción del paquete {@code exception} (o deja pasar la de PostgreSQL) y el
 * manejador global la convierte en una respuesta HTTP con mensaje en español.
 *
 * @param <D> tipo de DTO con el que trabaja el servicio
 */
public interface CRUDOperation<D> {

	/**
	 * Crea un registro a partir de los datos del DTO.
	 *
	 * @param dto datos del registro a crear
	 * @return el registro creado, con su identificador
	 */
	D crear(D dto);

	/**
	 * Obtiene todos los registros.
	 *
	 * @return lista de registros; vacía si no hay ninguno
	 */
	List<D> obtenerTodos();

	/**
	 * Obtiene un registro por su identificador.
	 *
	 * @param id identificador del registro
	 * @return el registro encontrado
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException si no existe
	 */
	D obtenerPorId(Long id);

	/**
	 * Actualiza un registro existente.
	 *
	 * @param id  identificador del registro
	 * @param dto nuevos datos del registro
	 * @return el registro actualizado
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException si no existe
	 */
	D actualizar(Long id, D dto);

	/**
	 * Elimina un registro por su identificador.
	 *
	 * @param id identificador del registro
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException si no existe
	 */
	void eliminar(Long id);

	/**
	 * Cuenta los registros existentes.
	 *
	 * @return número de registros
	 */
	long contar();

	/**
	 * Indica si existe un registro con el identificador dado.
	 *
	 * @param id identificador del registro
	 * @return {@code true} si existe
	 */
	boolean existe(Long id);
}
