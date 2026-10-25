package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.EmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RetiroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.EmpleadoRepository;

/**
 * Administra los empleados de la finca. Los empleados no se eliminan: se
 * retiran y conservan su historial (S27).
 */
@Service
public class EmpleadoService {

	/** Acceso a los empleados. */
	private final EmpleadoRepository empleadoRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param empleadoRepository acceso a los empleados
	 */
	public EmpleadoService(EmpleadoRepository empleadoRepository) {
		this.empleadoRepository = empleadoRepository;
	}

	/**
	 * Obtiene los empleados.
	 *
	 * @param soloActivos {@code true} para excluir a los retirados
	 * @param codigoCargo código del cargo para filtrar, o {@code null}
	 * @return empleados
	 */
	public List<EmpleadoDTO> obtener(boolean soloActivos, String codigoCargo) {
		return empleadoRepository.obtener(soloActivos, codigoCargo);
	}

	/**
	 * Obtiene un empleado.
	 *
	 * @param id identificador
	 * @return el empleado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public EmpleadoDTO obtenerPorId(Long id) {
		return empleadoRepository.buscar(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado con id " + id + "."));
	}

	/**
	 * Registra un empleado.
	 *
	 * @param dto datos del empleado
	 * @return el empleado creado
	 */
	public EmpleadoDTO crear(RegistroEmpleadoDTO dto) {
		return obtenerPorId(empleadoRepository.crear(dto));
	}

	/**
	 * Modifica un empleado.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el empleado actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public EmpleadoDTO actualizar(Long id, RegistroEmpleadoDTO dto) {
		if (!empleadoRepository.actualizar(id, dto)) {
			throw new RecursoNoEncontradoException("No existe el empleado con id " + id + ".");
		}
		return obtenerPorId(id);
	}

	/**
	 * Retira un empleado.
	 *
	 * @param id  identificador
	 * @param dto fecha de retiro
	 * @return el empleado retirado
	 */
	public EmpleadoDTO retirar(Long id, RetiroEmpleadoDTO dto) {
		empleadoRepository.retirar(id, dto == null ? null : dto.fechaRetiro());
		return obtenerPorId(id);
	}
}
