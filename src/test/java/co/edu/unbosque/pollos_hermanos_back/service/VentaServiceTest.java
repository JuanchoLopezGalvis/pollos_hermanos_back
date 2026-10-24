package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Pruebas de {@link VentaService}.
 */
class VentaServiceTest {

	/** Repositorio simulado. */
	private final VentaRepository ventaRepository = mock(VentaRepository.class);

	/** Servicio bajo prueba. */
	private final VentaService ventaService = new VentaService(ventaRepository);

	/** Un pago manual se registra sin referencia de pasarela y devuelve el saldo nuevo. */
	@Test
	void pagoManualSinReferencia() {
		VentaDTO venta = new VentaDTO(7L, 3L, 1L, "Asadero", LocalDate.of(2026, 10, 20), 96, new BigDecimal("576"),
				new BigDecimal("9000"), new BigDecimal("5184000"), new BigDecimal("1000000"), new BigDecimal("4184000"),
				LocalDate.of(2026, 11, 20), "PENDIENTE_PAGO", false, null, null);
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta));

		VentaDTO resultado = ventaService.registrarPago(7L, new RegistroPagoDTO("EFECTIVO", null, new BigDecimal("1000000")));

		verify(ventaRepository).registrarPago(7L, "EFECTIVO", null, new BigDecimal("1000000"), null);
		assertEquals(new BigDecimal("4184000"), resultado.saldo());
	}

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
}
