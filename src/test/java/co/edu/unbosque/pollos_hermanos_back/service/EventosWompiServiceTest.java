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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;

/**
 * Pruebas de {@link EventosWompiService}.
 */
class EventosWompiServiceTest {

	/** Secreto de eventos de prueba. */
	private static final String SECRETO = "test_events_abc";

	/** Pago en línea simulado. */
	private final PagoEnLineaService pagoEnLineaService = mock(PagoEnLineaService.class);

	/** Servicio bajo prueba. */
	private final EventosWompiService servicio = new EventosWompiService(pagoEnLineaService, SECRETO);

	/**
	 * Aviso como lo envía Wompi, firmado con el secreto dado.
	 *
	 * @param tipo    tipo de evento
	 * @param secreto secreto con el que se firma
	 * @return el aviso
	 */
	private static Map<String, Object> evento(String tipo, String secreto) {
		Map<String, Object> transaccion = Map.of("id", "1234-1610641025-49201", "status", "APPROVED",
				"amount_in_cents", 4490000, "reference", "PH-V7-1700000000000");
		long timestamp = 1530291411L;
		String checksum = PagoEnLineaService.sha256("1234-1610641025-49201" + "APPROVED" + 4490000 + timestamp + secreto);
		Map<String, Object> evento = new HashMap<>();
		evento.put("event", tipo);
		evento.put("data", Map.of("transaction", transaccion));
		evento.put("signature", Map.of("properties", List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"),
				"checksum", checksum.toUpperCase()));
		evento.put("timestamp", timestamp);
		return evento;
	}

	/** Un aviso con la firma correcta se confirma consultando la transacción en Wompi. */
	@Test
	void avisoFirmadoConfirmaElPago() {
		servicio.procesar(evento(EventosWompiService.TRANSACCION_ACTUALIZADA, SECRETO));

		verify(pagoEnLineaService).confirmar(eq("1234-1610641025-49201"), any());
	}

	/** Un aviso firmado con otro secreto no viene de Wompi: se rechaza sin registrar nada. */
	@Test
	void avisoConFirmaFalsaSeRechaza() {
		assertThrows(AccessDeniedException.class,
				() -> servicio.procesar(evento(EventosWompiService.TRANSACCION_ACTUALIZADA, "otro_secreto")));
		verify(pagoEnLineaService, never()).confirmar(any(), any());
	}

	/** Si alguien cambia el estado del aviso, la firma deja de coincidir. */
	@Test
	void avisoAlteradoSeRechaza() {
		Map<String, Object> evento = evento(EventosWompiService.TRANSACCION_ACTUALIZADA, SECRETO);
		evento.put("data", Map.of("transaction", Map.of("id", "1234-1610641025-49201", "status", "DECLINED",
				"amount_in_cents", 4490000)));

		assertFalse(EventosWompiService.firmaValida(evento, SECRETO));
	}

	/** Un aviso sin firma no es válido. */
	@Test
	void avisoSinFirmaNoEsValido() {
		assertFalse(EventosWompiService.firmaValida(Map.of("event", "transaction.updated"), SECRETO));
		assertTrue(EventosWompiService.firmaValida(evento("transaction.updated", SECRETO), SECRETO));
	}

	/** Otros eventos se aceptan pero no registran pagos. */
	@Test
	void otrosEventosSeIgnoran() {
		assertNull(servicio.procesar(evento("nequi_token.updated", SECRETO)));
		verify(pagoEnLineaService, never()).confirmar(any(), any());
	}

	/** Sin el secreto configurado no se procesa ningún aviso. */
	@Test
	void sinSecretoNoSeProcesa() {
		EventosWompiService sinSecreto = new EventosWompiService(pagoEnLineaService, "");

		assertThrows(ReglaNegocioException.class,
				() -> sinSecreto.procesar(evento(EventosWompiService.TRANSACCION_ACTUALIZADA, SECRETO)));
	}
}
