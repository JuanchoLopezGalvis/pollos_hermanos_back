package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.DetallePedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.PedidoRepository;

/**
 * Pedidos de aves de uno o varios lotes (S32). PostgreSQL valida que los lotes
 * estén listos para venta y que haya aves libres, también al confirmar. Sus
 * métodos no son {@code @Transactional} porque los procedimientos hacen su
 * propio {@code COMMIT}.
 */
@Service
public class PedidoService {

	/** Acceso a los pedidos. */
	private final PedidoRepository pedidoRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param pedidoRepository acceso a los pedidos
	 */
	public PedidoService(PedidoRepository pedidoRepository) {
		this.pedidoRepository = pedidoRepository;
	}

	/**
	 * Obtiene los pedidos.
	 *
	 * @param estado estado para filtrar, o {@code null}
	 * @return pedidos
	 */
	public List<PedidoDTO> obtener(String estado) {
		return pedidoRepository.obtener(estado);
	}

	/**
	 * Obtiene un pedido.
	 *
	 * @param id identificador
	 * @return el pedido
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public PedidoDTO obtenerPorId(Long id) {
		return pedidoRepository.buscar(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id + "."));
	}

	/**
	 * Obtiene las aves por lote de un pedido.
	 *
	 * @param id pedido
	 * @return detalle
	 * @throws RecursoNoEncontradoException si el pedido no existe
	 */
	public List<DetallePedidoDTO> obtenerDetalle(Long id) {
		obtenerPorId(id);
		return pedidoRepository.obtenerDetalle(id);
	}

	/**
	 * Crea un pedido pendiente.
	 *
	 * @param dto datos del pedido
	 * @return el pedido creado
	 */
	public PedidoDTO crear(RegistroPedidoDTO dto) {
		return obtenerPorId(pedidoRepository.crear(dto));
	}

	/**
	 * Confirma un pedido y aparta sus aves.
	 *
	 * @param id pedido
	 * @return el pedido confirmado
	 */
	public PedidoDTO confirmar(Long id) {
		pedidoRepository.confirmar(id);
		return obtenerPorId(id);
	}

	/**
	 * Cancela un pedido sin venta.
	 *
	 * @param id pedido
	 * @return el pedido cancelado
	 */
	public PedidoDTO cancelar(Long id) {
		pedidoRepository.cancelar(id);
		return obtenerPorId(id);
	}
}
