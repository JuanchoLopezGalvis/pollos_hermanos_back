package co.edu.unbosque.pollos_hermanos_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marca la cartera vencida cada día (S38). Es una tarea programada y no un
 * trigger porque la mora depende del paso del tiempo, no de un cambio en los
 * datos.
 */
@Component
public class CarteraProgramada {

	/** Registro de la tarea. */
	private static final Logger log = LoggerFactory.getLogger(CarteraProgramada.class);

	/** Servicio de ventas. */
	private final VentaService ventaService;

	/**
	 * Crea la tarea.
	 *
	 * @param ventaService servicio de ventas
	 */
	public CarteraProgramada(VentaService ventaService) {
		this.ventaService = ventaService;
	}

	/** Ejecuta {@code sp_marcar_cartera_vencida}; por defecto a la 1:00 a. m. */
	@Scheduled(cron = "${pollos.cartera.cron:0 0 1 * * *}")
	public void marcarCarteraVencida() {
		try {
			int marcadas = ventaService.marcarCarteraVencida().ventasMarcadas();
			log.info("Cartera vencida: {} ventas marcadas", marcadas);
		} catch (DataAccessException e) {
			log.error("No se pudo marcar la cartera vencida", e);
		}
	}
}
