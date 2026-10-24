package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.ConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.repository.AlimentacionRepository;

/**
 * Pruebas de {@link AlimentacionService}.
 */
class AlimentacionServiceTest {

	/** Repositorio simulado. */
	private final AlimentacionRepository repositorio = mock(AlimentacionRepository.class);

	/** Servicio bajo prueba. */
	private final AlimentacionService servicio = new AlimentacionService(repositorio);

	/** Registrar un consumo devuelve el costo que congeló PostgreSQL. */
	@Test
	void registraConsumoConSuCosto() {
		RegistroConsumoAlimentoDTO dto = new RegistroConsumoAlimentoDTO(3L, 1L, null, new BigDecimal("2"));
		when(repositorio.registrarConsumo(dto)).thenReturn(11L);
		when(repositorio.buscarConsumo(11L)).thenReturn(new ConsumoAlimentoDTO(11L, LocalDate.of(2026, 10, 3), 3L, "L-1", 3, 1L,
				"Preiniciador", new BigDecimal("2"), new BigDecimal("110000"), new BigDecimal("220000")));

		assertEquals(new BigDecimal("220000"), servicio.registrarConsumo(dto).costoTotal());
	}
}
