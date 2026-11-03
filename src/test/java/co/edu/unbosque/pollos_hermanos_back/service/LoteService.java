package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CambioEstadoLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadRegistradaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Registra los lotes, su estado y su mortalidad diaria.
 * <p>
 * Las reglas del negocio viven en PostgreSQL: este servicio invoca los
 * procedimientos y no las duplica. Sus métodos no son {@code @Transactional}
 * porque los procedimientos hacen su propio {@code COMMIT} o {@code ROLLBACK}.
 * No implementa {@link CRUDOperation} porque los lotes no se eliminan: se
 * cierran.
 */
@Service
public class LoteService {

	/** Acceso a lotes y mortalidad. */
	private final LoteRepository loteRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param loteRepository acceso a lotes y mortalidad
	 */
	public LoteService(LoteRepository loteRepository) {
		this.loteRepository = loteRepository;
	}

	/**
	 * Registra un lote nuevo. El código se guarda en mayúsculas.
	 *
	 * @param dto datos del lote
	 * @return el lote creado
	 */
	public LoteDTO crear(RegistroLoteDTO dto) {
		Long id = loteRepository.registrar(dto.codigo(), dto.idGalpon(), dto.fechaIngreso(), dto.cantidadInicial(),
				dto.costoPollito(), dto.fechaEstimadaVenta());
		return obtenerPorId(id);
	}

	/**
	 * Obtiene todos los lotes.
	 *
	 * @return lotes, del más reciente al más antiguo
	 */
	public List<LoteDTO> obtenerTodos() {
		return loteRepository.obtenerTodos();
	}

	/**
	 * Obtiene un lote por su identificador.
	 *
	 * @param id identificador del lote
	 * @return el lote
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public LoteDTO obtenerPorId(Long id) {
		return loteRepository.buscarPorId(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote con id " + id + "."));
	}

	/**
	 * Corrige la cantidad inicial, el costo del pollito o la fecha estimada de
	 * venta.
	 *
	 * @param id  identificador del lote
	 * @param dto nuevos datos
	 * @return el lote actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public LoteDTO actualizar(Long id, ActualizacionLoteDTO dto) {
		if (!loteRepository.actualizar(id, dto.cantidadInicial(), dto.costoPollito(), dto.fechaEstimadaVenta())) {
			throw new RecursoNoEncontradoException("No existe el lote con id " + id + ".");
		}
		return obtenerPorId(id);
	}

	/**
	 * Cambia el estado del lote.
	 *
	 * @param id  identificador del lote
	 * @param dto nuevo estado
	 * @return el lote actualizado
	 */
	public LoteDTO cambiarEstado(Long id, CambioEstadoLoteDTO dto) {
		loteRepository.cambiarEstado(id, dto.estado());
		return obtenerPorId(id);
	}

	/**
	 * Obtiene la mortalidad registrada de un lote.
	 *
	 * @param idLote identificador del lote
	 * @return registros, del día más reciente al más antiguo
	 * @throws RecursoNoEncontradoException si el lote no existe
	 */
	public List<MortalidadDTO> obtenerMortalidad(Long idLote) {
		obtenerPorId(idLote);
		return loteRepository.obtenerMortalidad(idLote);
	}

	/**
	 * Registra la mortalidad de un día.
	 *
	 * @param idLote identificador del lote
	 * @param dto    datos de la mortalidad
	 * @return registro creado y alerta del 5%
	 */
	public MortalidadRegistradaDTO registrarMortalidad(Long idLote, MortalidadDTO dto) {
		return loteRepository.registrarMortalidad(idLote, dto.fecha(), dto.cantidad(), dto.causa());
	}

	/**
	 * Corrige la cantidad o la causa de un registro de mortalidad. La fecha no
	 * cambia.
	 *
	 * @param idLote       identificador del lote
	 * @param idMortalidad identificador del registro
	 * @param dto          nuevos datos
	 * @return el lote con su mortalidad recalculada
	 * @throws RecursoNoEncontradoException si el registro no existe en ese lote
	 */
	public LoteDTO actualizarMortalidad(Long idLote, Long idMortalidad, MortalidadDTO dto) {
		if (!loteRepository.actualizarMortalidad(idLote, idMortalidad, dto.cantidad(), dto.causa())) {
			throw new RecursoNoEncontradoException(
					"No existe el registro de mortalidad " + idMortalidad + " en el lote " + idLote + ".");
		}
		return obtenerPorId(idLote);
	}
}
