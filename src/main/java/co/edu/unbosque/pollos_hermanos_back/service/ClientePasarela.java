package co.edu.unbosque.pollos_hermanos_back.service;

import co.edu.unbosque.pollos_hermanos_back.dto.TransaccionPasarelaDTO;

/**
 * Consulta transacciones en la pasarela de pagos. Se separa del servicio para
 * poder probarlo sin llamar a la pasarela real.
 */
public interface ClientePasarela {

	/**
	 * Consulta una transacción directamente en la pasarela, para no confiar en lo
	 * que reporte el navegador.
	 *
	 * @param idTransaccion identificador de la transacción
	 * @return la transacción
	 * @throws co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException si no se pudo consultar
	 */
	TransaccionPasarelaDTO consultarTransaccion(String idTransaccion);
}
