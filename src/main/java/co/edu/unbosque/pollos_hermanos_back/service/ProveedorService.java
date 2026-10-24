package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ProveedorDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ProveedorRepository;

/**
 * Administra los proveedores de alimento e insumos sanitarios. No se eliminan:
 * se desactivan para conservar las compras registradas.
 */
@Service
public class ProveedorService {

	/** Acceso a los proveedores. */
	private final ProveedorRepository proveedorRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param proveedorRepository acceso a los proveedores
	 */
	public ProveedorService(ProveedorRepository proveedorRepository) {
		this.proveedorRepository = proveedorRepository;
	}

	/**
	 * Obtiene todos los proveedores.
	 *
	 * @return proveedores
	 */
	public List<ProveedorDTO> obtenerTodos() {
		return proveedorRepository.obtenerTodos();
	}

	/**
	 * Obtiene un proveedor.
	 *
	 * @param id identificador
	 * @return el proveedor
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public ProveedorDTO obtenerPorId(Long id) {
		return proveedorRepository.buscar(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el proveedor con id " + id + "."));
	}

	/**
	 * Registra un proveedor.
	 *
	 * @param dto datos del proveedor
	 * @return el proveedor creado
	 */
	public ProveedorDTO crear(ProveedorDTO dto) {
		return obtenerPorId(proveedorRepository.crear(dto));
	}

	/**
	 * Modifica un proveedor.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el proveedor actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public ProveedorDTO actualizar(Long id, ProveedorDTO dto) {
		if (!proveedorRepository.actualizar(id, dto)) {
			throw new RecursoNoEncontradoException("No existe el proveedor con id " + id + ".");
		}
		return obtenerPorId(id);
	}
}
