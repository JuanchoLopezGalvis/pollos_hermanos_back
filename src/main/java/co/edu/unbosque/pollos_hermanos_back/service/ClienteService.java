package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;

/**
 * Administra los clientes: tiendas, restaurantes y personas naturales. No se
 * eliminan: se desactivan para conservar sus pedidos y ventas.
 */
@Service
public class ClienteService {

	/** Acceso a los clientes. */
	private final ClienteRepository clienteRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param clienteRepository acceso a los clientes
	 */
	public ClienteService(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	/**
	 * Obtiene los clientes.
	 *
	 * @return clientes
	 */
	public List<ClienteDTO> obtenerTodos() {
		return clienteRepository.obtenerTodos();
	}

	/**
	 * Obtiene un cliente.
	 *
	 * @param id identificador
	 * @return el cliente
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public ClienteDTO obtenerPorId(Long id) {
		return clienteRepository.buscar(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con id " + id + "."));
	}

	/**
	 * Registra un cliente.
	 *
	 * @param dto datos del cliente
	 * @return el cliente creado
	 */
	public ClienteDTO crear(RegistroClienteDTO dto) {
		return obtenerPorId(clienteRepository.crear(dto));
	}

	/**
	 * Modifica un cliente.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el cliente actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public ClienteDTO actualizar(Long id, RegistroClienteDTO dto) {
		if (!clienteRepository.actualizar(id, dto)) {
			throw new RecursoNoEncontradoException("No existe el cliente con id " + id + ".");
		}
		return obtenerPorId(id);
	}
}
