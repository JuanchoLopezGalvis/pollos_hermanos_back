package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.edu.unbosque.pollos_hermanos_back.dto.TransaccionPasarelaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;

/**
 * Cliente de la API de Wompi en su ambiente de pruebas (sandbox). Solo consulta
 * transacciones; los cobros los hace el checkout de Wompi en el navegador.
 */
@Component
public class ClienteWompi implements ClientePasarela {

	/** Cliente HTTP de la API de Wompi. */
	private final RestClient restClient;

	/**
	 * Crea el cliente.
	 *
	 * @param urlApi URL base de la API, por defecto la del sandbox
	 */
	public ClienteWompi(@Value("${pollos.wompi.url-api:https://sandbox.wompi.co/v1}") String urlApi) {
		this.restClient = RestClient.builder().baseUrl(urlApi).build();
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public TransaccionPasarelaDTO consultarTransaccion(String idTransaccion) {
		Map<String, Object> respuesta;
		try {
			respuesta = restClient.get().uri("/transactions/{id}", idTransaccion).retrieve().body(Map.class);
		} catch (RestClientException e) {
			throw new ReglaNegocioException("No fue posible consultar la transacción " + idTransaccion + " en la pasarela de pagos.");
		}
		Object datos = respuesta == null ? null : respuesta.get("data");
		if (!(datos instanceof Map<?, ?> d)) {
			throw new ReglaNegocioException("La pasarela de pagos no devolvió la transacción " + idTransaccion + ".");
		}
		Map<String, Object> t = (Map<String, Object>) d;
		Object monto = t.get("amount_in_cents");
		return new TransaccionPasarelaDTO(String.valueOf(t.get("id")), String.valueOf(t.get("status")),
				String.valueOf(t.get("reference")), monto instanceof Number n ? n.longValue() : 0L, String.valueOf(t.get("currency")));
	}
}
