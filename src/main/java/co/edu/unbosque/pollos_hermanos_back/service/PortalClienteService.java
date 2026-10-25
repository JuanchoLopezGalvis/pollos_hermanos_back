package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.CheckoutPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.DisponibilidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.UsuarioBase;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.PedidoRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;

/**
 * Lo que un cliente hace en el portal: pedir aves, consultar sus pedidos y
 * ventas, cancelar un pedido pendiente y pagar con Wompi (D24).
 * <p>
 * Todo se filtra por el cliente del usuario autenticado. Un pedido o una venta
 * de otro cliente responde igual que uno inexistente, para no revelar que
 * existe. Los pedidos se devuelven sin los lotes: el cliente no los ve.
 * <p>
 * No es {@code @Transactional}: los procedimientos hacen su propio COMMIT.
 */
@Service
public class PortalClienteService {

	/** Repositorio de clientes. */
	private final ClienteRepository clienteRepository;

	/** Repositorio de pedidos. */
	private final PedidoRepository pedidoRepository;

	/** Repositorio de ventas y pagos. */
	private final VentaRepository ventaRepository;

	/** Pago en línea con Wompi. */
	private final PagoEnLineaService pagoEnLineaService;

	/** Usuario autenticado. */
	private final EvaluadorPermisos permisos;

	/**
	 * Crea el servicio.
	 *
	 * @param clienteRepository  repositorio de clientes
	 * @param pedidoRepository   repositorio de pedidos
	 * @param ventaRepository    repositorio de ventas
	 * @param pagoEnLineaService pago en línea
	 * @param permisos           usuario autenticado
	 */
	public PortalClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository,
			VentaRepository ventaRepository, PagoEnLineaService pagoEnLineaService, EvaluadorPermisos permisos) {
		this.clienteRepository = clienteRepository;
		this.pedidoRepository = pedidoRepository;
		this.ventaRepository = ventaRepository;
		this.pagoEnLineaService = pagoEnLineaService;
		this.permisos = permisos;
	}

	/**
	 * Datos del cliente autenticado.
	 *
	 * @return el cliente
	 */
	public ClienteDTO perfil() {
		return clienteActual();
	}

	/**
	 * Aves que se pueden pedir ahora, sin decir de qué lotes.
	 *
	 * @return aves disponibles
	 */
	public DisponibilidadDTO disponibilidad() {
		clienteActual();
		return new DisponibilidadDTO(pedidoRepository.avesLibresVenta());
	}

	/**
	 * Pedidos del cliente autenticado.
	 *
	 * @return pedidos, del más reciente al más antiguo
	 */
	public List<PedidoDTO> pedidos() {
		return pedidoRepository.obtenerDeCliente(clienteActual().idCliente()).stream().map(PortalClienteService::sinLotes)
				.toList();
	}

	/**
	 * Crea un pedido. PostgreSQL toma las aves de los lotes más antiguos o lo
	 * rechaza si no alcanzan (D22).
	 *
	 * @param dto cantidad, fecha y observaciones
	 * @return el pedido creado, pendiente de aprobación
	 */
	public PedidoDTO crearPedido(PedidoClienteDTO dto) {
		Long id = pedidoRepository.crearParaCliente(clienteActual().idCliente(), dto);
		return sinLotes(pedidoRepository.buscar(id).orElseThrow());
	}

	/**
	 * Cancela un pedido propio que la finca todavía no ha aprobado.
	 *
	 * @param idPedido pedido
	 * @return el pedido cancelado
	 * @throws ReglaNegocioException si ya no está pendiente
	 */
	public PedidoDTO cancelarPedido(Long idPedido) {
		PedidoDTO pedido = pedidoPropio(idPedido);
		if (!"PENDIENTE".equals(pedido.estado())) {
			throw new ReglaNegocioException("Solo puedes cancelar pedidos pendientes; el pedido " + idPedido + " está "
					+ pedido.estado().toLowerCase() + ". Comunícate con la finca.");
		}
		pedidoRepository.cancelar(idPedido);
		return sinLotes(pedidoRepository.buscar(idPedido).orElseThrow());
	}

	/**
	 * Ventas del cliente autenticado con su valor y saldo.
	 *
	 * @return ventas, de la más reciente a la más antigua
	 */
	public List<VentaDTO> ventas() {
		return ventaRepository.obtenerVentasDeCliente(clienteActual().idCliente());
	}

	/**
	 * Pagos de una venta propia.
	 *
	 * @param idVenta venta
	 * @return pagos de la venta
	 */
	public List<PagoDTO> pagos(Long idVenta) {
		ventaPropia(idVenta);
		return ventaRepository.obtenerPagos(idVenta);
	}

	/**
	 * Prepara el checkout de Wompi para pagar una venta propia.
	 *
	 * @param idVenta venta
	 * @param dto     monto y URL de retorno
	 * @return URL firmada del checkout
	 */
	public CheckoutPagoDTO iniciarPago(Long idVenta, IniciarPagoEnLineaDTO dto) {
		ventaPropia(idVenta);
		return pagoEnLineaService.iniciar(idVenta, dto);
	}

	/**
	 * Confirma un pago en línea, solo si es de una venta propia.
	 *
	 * @param idTransaccion transacción de Wompi
	 * @return resultado del pago
	 */
	public ResultadoPagoEnLineaDTO confirmarPago(String idTransaccion) {
		Long idCliente = clienteActual().idCliente();
		return pagoEnLineaService.confirmar(idTransaccion,
				idVenta -> ventaRepository.buscarVenta(idVenta).map(v -> idCliente.equals(v.idCliente())).orElse(false));
	}

	/**
	 * Cliente vinculado al usuario autenticado.
	 *
	 * @return el cliente
	 * @throws RecursoNoEncontradoException si el usuario no es cliente
	 */
	private ClienteDTO clienteActual() {
		UsuarioBase usuario = permisos.usuarioActual();
		if (usuario == null) {
			throw new AccessDeniedException("Debe iniciar sesión.");
		}
		return clienteRepository.buscarPorUsuario(usuario.getId())
				.orElseThrow(() -> new RecursoNoEncontradoException("Tu usuario no está vinculado a ningún cliente."));
	}

	/**
	 * Busca un pedido del cliente autenticado.
	 *
	 * @param idPedido pedido
	 * @return el pedido
	 * @throws RecursoNoEncontradoException si no existe o es de otro cliente
	 */
	private PedidoDTO pedidoPropio(Long idPedido) {
		Long idCliente = clienteActual().idCliente();
		return pedidoRepository.buscar(idPedido).filter(p -> idCliente.equals(p.idCliente()))
				.orElseThrow(() -> new RecursoNoEncontradoException("No tienes un pedido con el número " + idPedido + "."));
	}

	/**
	 * Busca una venta del cliente autenticado.
	 *
	 * @param idVenta venta
	 * @return la venta
	 * @throws RecursoNoEncontradoException si no existe o es de otro cliente
	 */
	private VentaDTO ventaPropia(Long idVenta) {
		Long idCliente = clienteActual().idCliente();
		return ventaRepository.buscarVenta(idVenta).filter(v -> idCliente.equals(v.idCliente()))
				.orElseThrow(() -> new RecursoNoEncontradoException("No tienes una venta con el número " + idVenta + "."));
	}

	/**
	 * Quita los lotes del pedido: el cliente no los ve.
	 *
	 * @param p pedido completo
	 * @return el pedido sin lotes
	 */
	private static PedidoDTO sinLotes(PedidoDTO p) {
		return new PedidoDTO(p.idPedido(), p.idCliente(), p.cliente(), p.fechaPedido(), p.fechaEstimadaEntrega(), p.estado(),
				p.observaciones(), p.totalAves(), null, p.idVenta());
	}
}
