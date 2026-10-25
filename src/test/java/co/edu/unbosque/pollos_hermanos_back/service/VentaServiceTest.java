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
