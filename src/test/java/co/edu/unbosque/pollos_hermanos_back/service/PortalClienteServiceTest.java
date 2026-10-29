package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.LongPredicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import co.edu.unbosque.pollos_hermanos_back.UsuariosDePrueba;
import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.PedidoRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;
import co.edu.unbosque.pollos_hermanos_back.security.EvaluadorPermisos;

/**
 * Pruebas de {@link PortalClienteService}: cada cliente ve y opera solo lo suyo.
 */
class PortalClienteServiceTest {

	/** Clientes simulados. */
	private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

	/** Pedidos simulados. */
	private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);

	/** Ventas simuladas. */
	private final VentaRepository ventaRepository = mock(VentaRepository.class);

	/** Pago en línea simulado. */
	private final PagoEnLineaService pagoEnLineaService = mock(PagoEnLineaService.class);

	/** Usuario autenticado simulado. */
	private final EvaluadorPermisos permisos = mock(EvaluadorPermisos.class);

	/** Servicio bajo prueba. */
	private final PortalClienteService servicio = new PortalClienteService(clienteRepository, pedidoRepository,
			ventaRepository, pagoEnLineaService, permisos);

	/**
	 * Pedido de prueba.
	 *
	 * @param idCliente dueño
	 * @param estado    estado del pedido
	 * @return el pedido, con sus lotes como lo devuelve la vista
	 */
	private static PedidoDTO pedido(Long idCliente, String estado) {
		return new PedidoDTO(5L, idCliente, "Cliente", LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 28), estado, null, 150,
				"L-2026-1 (100), L-2026-2 (50)", null);
	}

	/**
	 * Venta de prueba.
	 *
	 * @param idCliente dueño
	 * @return la venta
	 */
	private static VentaDTO venta(Long idCliente) {
		return new VentaDTO(7L, 5L, idCliente, "Cliente", LocalDate.of(2026, 10, 25), 150, new BigDecimal("900"),
				new BigDecimal("9000"), new BigDecimal("8100000"), BigDecimal.ZERO, new BigDecimal("8100000"),
				LocalDate.of(2026, 11, 24), "PENDIENTE_PAGO", false, null, null, "Calle 1");
	}

	/** El usuario autenticado es el cliente 4. */
	@BeforeEach
	void iniciarSesionComoCliente4() {
		var usuario = UsuariosDePrueba.conPermisos("portal.cliente");
		usuario.setId(9L);
		when(permisos.usuarioActual()).thenReturn(usuario);
		when(clienteRepository.buscarPorUsuario(9L)).thenReturn(Optional.of(
				new ClienteDTO(4L, 2L, "Tienda", "900123", "Asadero", "Calle 1", null, "a@b.co", true, 9L, "asadero")));
	}

	/** El cliente no ve de qué lotes salen sus aves. */
	@Test
	void losPedidosSeDevuelvenSinLotes() {
		when(pedidoRepository.obtenerDeCliente(4L)).thenReturn(List.of(pedido(4L, "PENDIENTE")));

		assertNull(servicio.pedidos().get(0).lotes());
	}

	/** Un pedido de otro cliente responde como si no existiera. */
	@Test
	void noCancelaPedidosAjenos() {
		when(pedidoRepository.buscar(5L)).thenReturn(Optional.of(pedido(8L, "PENDIENTE")));

		assertThrows(RecursoNoEncontradoException.class, () -> servicio.cancelarPedido(5L));
		verify(pedidoRepository, never()).cancelar(any());
	}

	/** Un pedido aprobado ya no lo cancela el cliente. */
	@Test
	void noCancelaPedidosAprobados() {
		when(pedidoRepository.buscar(5L)).thenReturn(Optional.of(pedido(4L, "CONFIRMADO")));

		assertThrows(ReglaNegocioException.class, () -> servicio.cancelarPedido(5L));
		verify(pedidoRepository, never()).cancelar(any());
	}

	/** El cliente cancela su pedido pendiente. */
	@Test
	void cancelaSuPedidoPendiente() {
		when(pedidoRepository.buscar(5L)).thenReturn(Optional.of(pedido(4L, "PENDIENTE")));

		servicio.cancelarPedido(5L);

		verify(pedidoRepository).cancelar(5L);
	}

	/** No se paga ni se consulta una venta de otro cliente. */
	@Test
	void noPagaVentasAjenas() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta(8L)));

		assertThrows(RecursoNoEncontradoException.class,
				() -> servicio.iniciarPago(7L, new IniciarPagoEnLineaDTO(BigDecimal.TEN, "http://localhost/pago-resultado")));
		assertThrows(RecursoNoEncontradoException.class, () -> servicio.pagos(7L));
		verify(pagoEnLineaService, never()).iniciar(any(), any());
	}

	/** Al confirmar un pago, el filtro solo acepta las ventas del cliente. */
	@Test
	void confirmarFiltraPorCliente() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta(4L)));
		when(ventaRepository.buscarVenta(8L)).thenReturn(Optional.of(venta(8L)));

		servicio.confirmarPago("tx-1");

		ArgumentCaptor<LongPredicate> filtro = ArgumentCaptor.forClass(LongPredicate.class);
		verify(pagoEnLineaService).confirmar(eq("tx-1"), filtro.capture());
		assertTrue(filtro.getValue().test(7L));
		assertFalse(filtro.getValue().test(8L));
		assertFalse(filtro.getValue().test(99L));
	}

	/** El historial trae los pagos de todas las ventas del cliente autenticado. */
	@Test
	void historialPagosEsDelClienteAutenticado() {
		servicio.historialPagos();

		verify(ventaRepository).obtenerPagosDeCliente(4L);
	}

	/** Un usuario que no es cliente (por ejemplo, del personal) no tiene portal. */
	@Test
	void usuarioSinClienteResponde404() {
		when(clienteRepository.buscarPorUsuario(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> servicio.pedidos());
	}
}
