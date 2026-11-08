package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.CheckoutPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TransaccionPasarelaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Pruebas de {@link PagoEnLineaService}.
 */
class PagoEnLineaServiceTest {

	/** Ventas simuladas. */
	private final VentaRepository ventaRepository = mock(VentaRepository.class);

	/** Pasarela simulada. */
	private final ClientePasarela pasarela = mock(ClientePasarela.class);

	/** Reloj fijo para que la referencia sea predecible. */
	private final Clock reloj = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC);

	/** Comprobante por correo simulado. */
	private final ComprobantePagoService comprobante = mock(ComprobantePagoService.class);

	/** Servicio con llaves de prueba. */
	private final PagoEnLineaService servicio = new PagoEnLineaService(ventaRepository, pasarela, comprobante, "pub_test_abc",
			"test_integrity_xyz", "https://checkout.wompi.co/p/", reloj);

	/**
	 * Venta de prueba con el saldo dado.
	 *
	 * @param saldo saldo pendiente
	 * @return la venta
	 */
	private static VentaDTO venta(String saldo) {
		return new VentaDTO(7L, 3L, 1L, "Asadero", LocalDate.of(2026, 10, 20), 100, new BigDecimal("600"),
				new BigDecimal("9000"), new BigDecimal("5400000"), BigDecimal.ZERO, new BigDecimal(saldo),
				LocalDate.of(2026, 11, 20), "PENDIENTE_PAGO", false, null, null, "Calle 1");
	}

	/** El checkout lleva la firma de integridad SHA-256 de referencia, monto, moneda y secreto. */
	@Test
	void iniciarFirmaElCheckout() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("5400000")));

		CheckoutPagoDTO checkout = servicio.iniciar(7L, new IniciarPagoEnLineaDTO(new BigDecimal("150000"), "http://localhost/pago-resultado"));

		String referencia = "PH-V7-1700000000000";
		assertEquals(referencia, checkout.referencia());
		assertEquals(15_000_000L, checkout.montoEnCentavos());
		String firma = PagoEnLineaService.sha256(referencia + "15000000COP" + "test_integrity_xyz");
		assertTrue(checkout.urlCheckout().contains("signature%3Aintegrity=" + firma));
		assertTrue(checkout.urlCheckout().contains("public-key=pub_test_abc"));
	}

	/** No se paga más que el saldo. */
	@Test
	void montoMayorQueElSaldoSeRechaza() {
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("1000")));

		assertThrows(ReglaNegocioException.class,
				() -> servicio.iniciar(7L, new IniciarPagoEnLineaDTO(new BigDecimal("1000.01"), "http://localhost/x")));
	}

	/** Con llaves de producción no se inicia ningún pago: el sistema no cobra dinero real. */
	@Test
	void llavesDeProduccionSeRechazan() {
		PagoEnLineaService produccion = new PagoEnLineaService(ventaRepository, pasarela, comprobante, "pub_prod_abc", "prod_integrity",
				"https://checkout.wompi.co/p/", reloj);

		assertThrows(ReglaNegocioException.class,
				() -> produccion.iniciar(7L, new IniciarPagoEnLineaDTO(BigDecimal.TEN, "http://localhost/x")));
	}

	/** Una transacción aprobada se registra como pago en línea con su referencia. */
	@Test
	void confirmarAprobadaRegistraElPago() {
		when(pasarela.consultarTransaccion("tx-1"))
				.thenReturn(new TransaccionPasarelaDTO("tx-1", "APPROVED", "PH-V7-1700000000000", 15_000_000L, "COP"));
		when(ventaRepository.buscarPagoPorReferencia("tx-1")).thenReturn(Optional.empty());
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("5250000")));

		ResultadoPagoEnLineaDTO resultado = servicio.confirmar("tx-1", idVenta -> true);

		assertTrue(resultado.registrado());
		verify(ventaRepository).registrarPago(7L, "EN_LINEA", null, new BigDecimal("150000.00"), "tx-1");
		verify(comprobante).enviar("tx-1");
	}

	/** Confirmar dos veces la misma transacción no duplica el pago. */
	@Test
	void confirmarDosVecesNoDuplica() {
		when(pasarela.consultarTransaccion("tx-1"))
				.thenReturn(new TransaccionPasarelaDTO("tx-1", "APPROVED", "PH-V7-1700000000000", 15_000_000L, "COP"));
		when(ventaRepository.buscarPagoPorReferencia("tx-1"))
				.thenReturn(Optional.of(new PagoDTO(1L, 7L, LocalDate.now(), new BigDecimal("150000"), "EN_LINEA", "En línea", "tx-1")));
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("5250000")));

		assertTrue(servicio.confirmar("tx-1", idVenta -> true).registrado());
		verify(ventaRepository, never()).registrarPago(anyLong(), anyString(), any(), any(), anyString());
		verify(comprobante, never()).enviar(anyString());
	}

	/** Una transacción rechazada no registra nada. */
	@Test
	void confirmarRechazadaNoRegistra() {
		when(pasarela.consultarTransaccion("tx-2"))
				.thenReturn(new TransaccionPasarelaDTO("tx-2", "DECLINED", "PH-V7-1700000000000", 100L, "COP"));

		ResultadoPagoEnLineaDTO resultado = servicio.confirmar("tx-2", idVenta -> true);

		assertFalse(resultado.registrado());
		verify(ventaRepository, never()).registrarPago(anyLong(), anyString(), any(), any(), anyString());
	}

	/** Solo se aceptan referencias generadas por el sistema. */
	@Test
	void referenciaAjenaNoIdentificaVenta() {
		assertEquals(7L, PagoEnLineaService.idVentaDeReferencia("PH-V7-123"));
		assertNull(PagoEnLineaService.idVentaDeReferencia("OTRA-7"));
	}

	/** Un pago de una venta que el filtro no permite (de otro cliente) no se registra. */
	@Test
	void confirmarConFiltroNoRegistraVentasAjenas() {
		when(pasarela.consultarTransaccion("tx-1"))
				.thenReturn(new TransaccionPasarelaDTO("tx-1", "APPROVED", "PH-V7-1700000000000", 15_000_000L, "COP"));

		ResultadoPagoEnLineaDTO r = servicio.confirmar("tx-1", idVenta -> false);

		assertFalse(r.registrado());
		assertNull(r.idVenta());
		verify(ventaRepository, never()).registrarPago(anyLong(), anyString(), any(), any(), anyString());
	}

	/** Si el aviso de Wompi y el cliente confirman a la vez, el segundo encuentra el pago y no falla. */
	@Test
	void confirmacionSimultaneaNoFalla() {
		when(pasarela.consultarTransaccion("tx-1"))
				.thenReturn(new TransaccionPasarelaDTO("tx-1", "APPROVED", "PH-V7-1700000000000", 15_000_000L, "COP"));
		when(ventaRepository.buscarPagoPorReferencia("tx-1")).thenReturn(Optional.empty(),
				Optional.of(new PagoDTO(1L, 7L, LocalDate.now(), new BigDecimal("150000"), "EN_LINEA", "Pago en línea", "tx-1")));
		when(ventaRepository.registrarPago(anyLong(), anyString(), any(), any(), anyString()))
				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_pago_referencia"));
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(venta("0")));

		ResultadoPagoEnLineaDTO r = servicio.confirmar("tx-1", idVenta -> true);

		assertTrue(r.registrado());
		assertEquals("Este pago ya estaba registrado en la venta 7.", r.mensaje());
	}
}
