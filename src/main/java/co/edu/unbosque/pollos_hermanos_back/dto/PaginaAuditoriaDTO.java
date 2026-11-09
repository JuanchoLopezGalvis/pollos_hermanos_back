package co.edu.unbosque.pollos_hermanos_back.dto;

import java.util.List;

/**
 * Una página de la auditoría, del cambio más reciente al más antiguo.
 *
 * @param registros cambios de la página
 * @param total     cambios que cumplen los filtros, en todas las páginas
 * @param pagina    número de la página, desde 0
 * @param tamano    cambios por página
 */
public record PaginaAuditoriaDTO(List<AuditoriaDTO> registros, long total, int pagina, int tamano) {
}
