package co.edu.unbosque.pollos_hermanos_back.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Verifica en PostgreSQL el lote de un seguimiento de MongoDB, para que ambas
 * bases manejen el mismo lote, el mismo día y la misma edad.
 */
@Service
public class VerificadorLotePostgres implements VerificadorLote {

	/** Acceso a los lotes. */
	private final LoteRepository loteRepository;

	/**
	 * Crea el verificador.
	 *
	 * @param loteRepository acceso a los lotes
	 */
	public VerificadorLotePostgres(LoteRepository loteRepository) {
		this.loteRepository = loteRepository;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws RecursoNoEncontradoException si el lote no existe
	 * @throws ReglaNegocioException        si el lote no está activo
	 */
	@Override
	public LocalDate obtenerFechaIngresoLoteActivo(String codigoLote) {
		LoteDTO lote = loteRepository.buscarPorCodigo(codigoLote)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote " + codigoLote + "."));
		if (!lote.estado().esActivo()) {
			throw new ReglaNegocioException(
					"El lote " + codigoLote + " está " + lote.estado() + ": solo se hace seguimiento a lotes activos.");
		}
		return lote.fechaIngreso();
	}
}
