package co.edu.unbosque.pollos_hermanos_back.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;

/**
 * Pruebas de los filtros de {@link AuditoriaRepository}.
 */
class AuditoriaRepositoryTest {

	/** Sin filtros no hay WHERE. */
	@Test
	void sinFiltros() {
		List<Object> parametros = new ArrayList<>();

		assertEquals("", AuditoriaRepository.where(new FiltroAuditoriaDTO(" ", "", null, false, null, null, null), parametros));
		assertEquals(0, parametros.size());
	}

	/** Cada filtro agrega su condición y su parámetro en el mismo orden. */
	@Test
	void todosLosFiltros() {
		List<Object> parametros = new ArrayList<>();

		String where = AuditoriaRepository.where(new FiltroAuditoriaDTO("lote", "delete", "50%_x", false, "52",
				LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)), parametros);

		assertEquals(" WHERE tabla = ? AND operacion = ? AND usuario_app ILIKE ? AND id_registro = ?"
				+ " AND fecha_creacion >= ? AND fecha_creacion < ?", where);
		assertEquals(List.of("lote", "DELETE", "%50\\%\\_x%", "52", LocalDate.of(2026, 11, 1).atStartOfDay(),
				LocalDate.of(2026, 11, 2).atStartOfDay()), parametros);
	}

	/** "Solo del sistema" busca los cambios sin usuario e ignora el texto del usuario. */
	@Test
	void soloSistema() {
		List<Object> parametros = new ArrayList<>();

		assertEquals(" WHERE usuario_app IS NULL",
				AuditoriaRepository.where(new FiltroAuditoriaDTO(null, null, "gerente", true, null, null, null), parametros));
		assertEquals(0, parametros.size());
	}
}
