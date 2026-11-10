package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.CorreoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Pruebas de {@link ComprobantePagoService}.
 */
class ComprobantePagoServiceTest {

	/** Ventas simuladas. */
	private final VentaRepository ventaRepository = mock(VentaRepository.class);

	/** Clientes simulados. */
	private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

	/** Usuarios simulados. */
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

	/** Correo simulado. */
	private final CorreoService correoService = mock(CorreoService.class);

	/** Servicio bajo prueba. */
	private final ComprobantePagoService servicio = new ComprobantePagoService(ventaRepository, clienteRepository,
			usuarioRepository, correoService);

	/** Pago en línea de 150.000 en la venta 7. */
	private static final PagoDTO PAGO = new PagoDTO(3L, 7L, LocalDate.of(2026, 10, 27), new BigDecimal("150000"), "EN_LINEA",
			"En línea", "tx-1");

	/** Venta de 100 pollos y 600 libras a 9.000 la libra, con 150.000 pagados. */
	private static final VentaDTO VENTA = new VentaDTO(7L, 3L, 1L, "Asadero", LocalDate.of(2026, 10, 20), 100,
			new BigDecimal("600"), new BigDecimal("9000"), new BigDecimal("5400000"), new BigDecimal("150000"),
			new BigDecimal("5250000"), LocalDate.of(2026, 11, 20), "PENDIENTE_PAGO", false, null, null, "Calle 1");

	/**
	 * Cliente de prueba.
	 *
	 * @param correo correo del cliente
	 * @return el cliente, con cuenta en el usuario 9
	 */
	private static ClienteDTO cliente(String correo) {
		return new ClienteDTO(1L, 1L, "Restaurante", "900123", "Asadero", "Calle 1", null, correo, true, 9L, "asadero");
	}

	/** Prepara el pago, la venta y el cliente con el correo dado. */
	private void conCliente(String correo) {
		when(ventaRepository.buscarPagoPorReferencia("tx-1")).thenReturn(Optional.of(PAGO));
		when(ventaRepository.buscarVenta(7L)).thenReturn(Optional.of(VENTA));
		when(clienteRepository.buscar(1L)).thenReturn(Optional.of(cliente(correo)));
	}

	/** El comprobante va al correo del cliente con el peso y el saldo. */
	@Test
	@SuppressWarnings("unchecked")
	void enviaElComprobanteAlCliente() {
		conCliente("asadero@correo.co");

		assertTrue(servicio.enviar("tx-1"));

		ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
		verify(correoService).enviarPlantilla(eq("asadero@correo.co"), eq("Comprobante de pago - Compra #7"),
				eq(ComprobantePagoService.PLANTILLA), variables.capture());
		assertEquals("600 lb", variables.getValue().get("pesoTotal"));
		assertEquals("6 lb", variables.getValue().get("pesoPromedio"));
		assertEquals("$ 150.000", variables.getValue().get("montoPagado"));
		assertEquals("$ 5.250.000", variables.getValue().get("saldo"));
		assertEquals(false, variables.getValue().get("pagada"));
	}

	/** La plantilla real muestra el peso de los pollos, el monto y el saldo. */
	@Test
	void plantillaMuestraPesoYSaldo() {
		ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
		resolvedor.setPrefix("templates/");
		resolvedor.setSuffix(".html");
		resolvedor.setTemplateMode(TemplateMode.HTML);
		resolvedor.setCharacterEncoding("UTF-8");
		SpringTemplateEngine motor = new SpringTemplateEngine();
		motor.setTemplateResolver(resolvedor);
		Context contexto = new Context();
		contexto.setVariables(ComprobantePagoService.variables(PAGO, VENTA, cliente("asadero@correo.co")));

		String html = motor.process(ComprobantePagoService.PLANTILLA, contexto);

		assertTrue(html.contains("600 lb"));
		assertTrue(html.contains("6 lb"));
		assertTrue(html.contains("$ 9.000"));
		assertTrue(html.contains("$ 150.000"));
		assertTrue(html.contains("$ 5.250.000"));
		assertTrue(html.contains("20/11/2026"));
		assertTrue(html.contains("tx-1"));
	}

	/** Si el cliente no tiene correo, se usa el de su cuenta del portal. */
	@Test
	void sinCorreoDeClienteUsaElDeLaCuenta() {
		conCliente(null);
		Usuario usuario = new Usuario();
		usuario.setCorreo("cuenta@correo.co");
		when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));

		assertTrue(servicio.enviar("tx-1"));

		verify(correoService).enviarPlantilla(eq("cuenta@correo.co"), anyString(), anyString(), anyMap());
	}

	/** Si el correo falla, no se lanza la excepción: el pago ya quedó registrado. */
	@Test
	void falloDelCorreoNoRompeElPago() {
		conCliente("asadero@correo.co");
		doThrow(new CorreoException("sin conexión", null)).when(correoService).enviarPlantilla(anyString(), anyString(),
				anyString(), any());

		assertFalse(servicio.enviar("tx-1"));
	}

	/** Sin pago registrado no se envía nada. */
	@Test
	void sinPagoNoEnvia() {
		when(ventaRepository.buscarPagoPorReferencia("tx-9")).thenReturn(Optional.empty());

		assertFalse(servicio.enviar("tx-9"));
		verify(correoService, never()).enviarPlantilla(anyString(), anyString(), anyString(), any());
	}
}
