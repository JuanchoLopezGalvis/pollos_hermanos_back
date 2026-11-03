package co.edu.unbosque.pollos_hermanos_back;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;

/**
 * Utilidades para construir lotes en las pruebas.
 */
public final class LotesDePrueba {

	/** Clase de utilidades: no se instancia. */
	private LotesDePrueba() {
	}

	/**
	 * Crea el lote L-2026-01, ingresado el 1 de octubre de 2026, como lo
	 * devolvería la vista {@code v_lote_resumen}.
	 *
	 * @param id     identificador
	 * @param estado estado del lote
	 * @return el lote
	 */
	public static LoteDTO lote(Long id, EstadoLote estado) {
		return new LoteDTO(id, "L-2026-01", 1L, "Galpón 1", LocalDate.of(2026, 10, 1), 4000, new BigDecimal("2600"),
				new BigDecimal("10400000"), LocalDate.of(2026, 11, 10), estado, 23, 120, new BigDecimal("3.00"), 3880,
				false, 0, new BigDecimal("80"), new BigDecimal("3200"));
	}
}
