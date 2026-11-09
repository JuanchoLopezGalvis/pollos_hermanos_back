package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PaginaAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.AuditoriaRepository;

/**
 * Consulta de la auditoría. Los cambios los registran los triggers de
 * PostgreSQL; aquí solo se leen por páginas.
 */
@Service
public class AuditoriaService {

	/** Cambios por página cuando no se indica. */
	public static final int TAMANO_POR_DEFECTO = 50;

	/** Máximo de cambios por página. */
	public static final int TAMANO_MAXIMO = 200;

	/** Operaciones que registra la auditoría. */
	private static final Set<String> OPERACIONES = Set.of("INSERT", "UPDATE", "DELETE", "TRUNCATE");

	/** Acceso a la auditoría. */
	private final AuditoriaRepository auditoriaRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param auditoriaRepository acceso a la auditoría
	 */
	public AuditoriaService(AuditoriaRepository auditoriaRepository) {
		this.auditoriaRepository = auditoriaRepository;
	}

	/**
	 * Busca una página de cambios.
	 *
	 * @param filtro filtros; los vacíos no se aplican
	 * @param pagina número de página desde 0; si es negativo se usa 0
	 * @param tamano cambios por página; se ajusta entre 1 y {@link #TAMANO_MAXIMO}
	 * @return la página con el total de cambios que cumplen los filtros
	 * @throws ReglaNegocioException si la operación no existe o las fechas están invertidas
	 */
	public PaginaAuditoriaDTO buscar(FiltroAuditoriaDTO filtro, int pagina, int tamano) {
		if (filtro.operacion() != null && !filtro.operacion().isBlank()
				&& !OPERACIONES.contains(filtro.operacion().trim().toUpperCase())) {
			throw new ReglaNegocioException("La operación debe ser INSERT, UPDATE, DELETE o TRUNCATE.");
		}
		if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
			throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final.");
		}
		int paginaReal = Math.max(pagina, 0);
		int tamanoReal = Math.clamp(tamano, 1, TAMANO_MAXIMO);
		return new PaginaAuditoriaDTO(
				auditoriaRepository.buscar(filtro, tamanoReal, (long) paginaReal * tamanoReal),
				auditoriaRepository.contar(filtro), paginaReal, tamanoReal);
	}

	/**
	 * Obtiene las tablas que tienen cambios registrados, para el filtro.
	 *
	 * @return nombres de las tablas
	 */
	public List<String> tablas() {
		return auditoriaRepository.tablas();
	}
}
