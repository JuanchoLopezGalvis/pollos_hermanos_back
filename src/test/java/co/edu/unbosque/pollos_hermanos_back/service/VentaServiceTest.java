package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Pruebas de {@link VentaService}.
 */
class VentaServiceTest {

	/** Repositorio simulado. */
	private final VentaRepository ventaRepository = mock(VentaRepository.class);

	/** Servicio bajo prueba. */
	private final VentaService ventaService = new VentaService(ventaRepository);

	/** El marcado de cartera devuelve cuántas ventas pasaron a vencidas. */
	@Test
	void marcaCarteraVencida() {
		when(ventaRepository.marcarCarteraVencida()).thenReturn(3);

		assertEquals(3, ventaService.marcarCarteraVencida().ventasMarcadas());
	}

	/** Los pagos de una venta inexistente responden 404. */
	@Test
	void pagosDeVentaInexistente() {
		when(ventaRepository.buscarVenta(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> ventaService.obtenerPagos(9L));
	}
	/**
	 * Venta de prueba con el saldo indicado.
	 *
	 * @param saldo saldo pendiente
	 * @return venta
	 */
	private static VentaDTO venta(String saldo) {
		BigDecimal s = new BigDecimal(saldo);
		return new VentaDTO(7L, 3L, 1L, "Tienda La 80", LocalDate.now(), 60, new BigDecimal("576"), new BigDecimal("9000"),
				new BigDecimal("5184000"), new BigDecimal("5184000").subtract(s), s, LocalDate.now().plusDays(30),
				s.signum() > 0 ? "PENDIENTE_PAGO" : "PAGADA", false, null, null, "Calle 80");
	}

	/** El pago en efectivo se registra por todo el saldo y devuelve la venta pagada. */
	@Test
	void pagaEnEfectivoTodoElSaldo() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("1184000")), Optional.of(venta("0")));

		assertEquals("PAGADA", ventaService.registrarPagoEfectivo(7L).estado());
		verify(ventaRepository).registrarPago(7L, "EFECTIVO", null, new BigDecimal("1184000"), null);
	}

	/** Una venta sin saldo no recibe un pago en efectivo. */
	@Test
	void ventaPagadaNoRecibePagoEnEfectivo() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("0")));

		assertThrows(ReglaNegocioException.class, () -> ventaService.registrarPagoEfectivo(7L));
		verify(ventaRepository, never()).registrarPago(any(), any(), any(), any(), any());
	}
}
