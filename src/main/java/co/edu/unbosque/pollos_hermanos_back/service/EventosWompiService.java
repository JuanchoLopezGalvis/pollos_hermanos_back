package co.edu.unbosque.pollos_hermanos_back.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;

/**
 * Avisos (eventos) que Wompi envía al servidor cuando cambia una transacción.
 * <p>
 * Así un pago aprobado queda registrado aunque el cliente cierre la ventana
 * antes de volver a la página de resultado. El aviso no se cree tal cual: se
 * verifica su firma con el secreto de eventos y luego la transacción se
 * consulta directamente en Wompi, igual que cuando vuelve el cliente. Recibir
 * el mismo aviso varias veces no duplica el pago.
 * <p>
 * Firma de Wompi: SHA-256 de los valores de {@code signature.properties}
 * (tomados de {@code data}), seguidos del {@code timestamp} y del secreto.
 */
@Service
public class EventosWompiService {

	/** Evento que envía Wompi cuando una transacción cambia de estado. */
	static final String TRANSACCION_ACTUALIZADA = "transaction.updated";

	/** Pago en línea, para consultar y registrar la transacción. */
	private final PagoEnLineaService pagoEnLineaService;

	/** Secreto de eventos del comercio ({@code test_events_...}). */
	private final String secretoEventos;

	/**
	 * Crea el servicio.
	 *
	 * @param pagoEnLineaService pago en línea
	 * @param secretoEventos     valor de {@code WOMPI_SECRETO_EVENTOS}
	 */
	public EventosWompiService(PagoEnLineaService pagoEnLineaService,
			@Value("${pollos.wompi.secreto-eventos:}") String secretoEventos) {
		this.pagoEnLineaService = pagoEnLineaService;
		this.secretoEventos = secretoEventos;
	}

	/**
	 * Procesa un aviso de Wompi.
	 *
	 * @param evento cuerpo del aviso
	 * @return el resultado del pago, o {@code null} si el evento no es de una transacción
	 * @throws ReglaNegocioException  si falta el secreto de eventos en el servidor
	 * @throws AccessDeniedException si la firma no corresponde: el aviso no viene de Wompi
	 */
	public ResultadoPagoEnLineaDTO procesar(Map<String, Object> evento) {
		if (secretoEventos == null || secretoEventos.isBlank()) {
			throw new ReglaNegocioException("Falta WOMPI_SECRETO_EVENTOS en el servidor: no se pueden verificar los avisos de Wompi.");
		}
		if (!firmaValida(evento, secretoEventos)) {
			throw new AccessDeniedException("La firma del aviso no corresponde a Wompi.");
		}
		if (!TRANSACCION_ACTUALIZADA.equals(evento.get("event"))) {
			return null;
		}
		Object idTransaccion = valor(evento.get("data"), "transaction.id");
		if (idTransaccion == null) {
			return null;
		}
		return pagoEnLineaService.confirmar(idTransaccion.toString(), idVenta -> true);
	}

	/**
	 * Verifica la firma del aviso en tiempo constante.
	 *
	 * @param evento  cuerpo del aviso
	 * @param secreto secreto de eventos
	 * @return {@code true} si la firma corresponde
	 */
	static boolean firmaValida(Map<String, Object> evento, String secreto) {
		if (!(evento.get("signature") instanceof Map<?, ?> firma) || !(firma.get("properties") instanceof List<?> propiedades)
				|| !(firma.get("checksum") instanceof String checksum) || evento.get("timestamp") == null) {
			return false;
		}
		StringBuilder texto = new StringBuilder();
		for (Object propiedad : propiedades) {
			Object v = valor(evento.get("data"), String.valueOf(propiedad));
			if (v == null) {
				return false;
			}
			texto.append(v);
		}
		texto.append(evento.get("timestamp")).append(secreto);
		byte[] esperado = PagoEnLineaService.sha256(texto.toString()).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(esperado, checksum.toLowerCase().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Busca un valor por su ruta con puntos, por ejemplo {@code transaction.status}.
	 *
	 * @param raiz objeto donde buscar
	 * @param ruta ruta separada por puntos
	 * @return el valor, o {@code null} si no existe
	 */
	private static Object valor(Object raiz, String ruta) {
		Object actual = raiz;
		for (String parte : ruta.split("\\.")) {
			if (!(actual instanceof Map<?, ?> mapa)) {
				return null;
			}
			actual = mapa.get(parte);
		}
		return actual;
	}
}
