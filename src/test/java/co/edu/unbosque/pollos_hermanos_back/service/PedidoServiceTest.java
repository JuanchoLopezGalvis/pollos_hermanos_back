package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.LineaPedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.PedidoRepository;

/**
 * Pruebas de {@link PedidoService}.
 */
class PedidoServiceTest {

	/** Repositorio simulado. */
	private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);

	/** Servicio bajo prueba. */
	private final PedidoService pedidoService = new PedidoService(pedidoRepository);

	/**
	 * Pedido de prueba.
	 *
	 * @param estado estado
	 * @return el pedido
	 */
	private static PedidoDTO pedido(String estado) {
		return new PedidoDTO(5L, 1L, "Asadero", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), estado, null, 150,
				"L-1 (100), L-2 (50)", null);
	}

	/** Crear un pedido con varios lotes devuelve el pedido pendiente. */
	@Test
	void creaPedidoDeVariosLotes() {
		RegistroPedidoDTO dto = new RegistroPedidoDTO(1L, LocalDate.of(2026, 10, 22), null,
				List.of(new LineaPedidoDTO(1L, 100), new LineaPedidoDTO(2L, 50)));
		when(pedidoRepository.crear(dto)).thenReturn(5L);
		when(pedidoRepository.buscar(5L)).thenReturn(Optional.of(pedido("PENDIENTE")));

		assertEquals(150, pedidoService.crear(dto).totalAves());
	}

	/** Confirmar llama al procedimiento y devuelve el pedido actualizado. */
	@Test
	void confirmaPedido() {
		when(pedidoRepository.buscar(5L)).thenReturn(Optional.of(pedido("CONFIRMADO")));

		assertEquals("CONFIRMADO", pedidoService.confirmar(5L).estado());
		verify(pedidoRepository).confirmar(5L);
	}

	/** El detalle de un pedido inexistente responde 404. */
	@Test
	void detalleDePedidoInexistente() {
		when(pedidoRepository.buscar(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.obtenerDetalle(9L));
	}
}
