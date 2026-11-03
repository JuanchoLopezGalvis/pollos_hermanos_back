package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.dto.GalponDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GalponOcupacionDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoGalpon;
import co.edu.unbosque.pollos_hermanos_back.entity.Galpon;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.GalponRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Administra los galpones de la finca. Un galpón aloja varios lotes de edades
 * distintas (producción escalonada) que comparten su capacidad.
 * <p>
 * Las reglas del estado y la capacidad las valida el trigger
 * {@code trg_galpon_validar}; si las incumple, PostgreSQL rechaza la operación y
 * el manejador global responde con su mensaje.
 */
@Service
public class GalponService implements CRUDOperation<GalponDTO> {

	/** Repositorio de galpones. */
	private final GalponRepository galponRepository;

	/** Acceso a los lotes, para la ocupación. */
	private final LoteRepository loteRepository;

	/** Conversor entre entidad y DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param galponRepository repositorio de galpones
	 * @param loteRepository   acceso a los lotes
	 * @param modelMapper      conversor entre entidad y DTO
	 */
	public GalponService(GalponRepository galponRepository, LoteRepository loteRepository, ModelMapper modelMapper) {
		this.galponRepository = galponRepository;
		this.loteRepository = loteRepository;
		this.modelMapper = modelMapper;
	}

	/**
	 * Obtiene la ocupación de los galpones: varios lotes comparten la capacidad
	 * de cada galpón.
	 *
	 * @return ocupación por galpón
	 */
	public List<GalponOcupacionDTO> obtenerOcupacion() {
		return loteRepository.obtenerOcupacionGalpones();
	}

	/**
	 * Crea un galpón. Si no se envía el estado, queda disponible.
	 *
	 * @param dto datos del galpón
	 * @return el galpón creado
	 */
	@Override
	@Transactional
	public GalponDTO crear(GalponDTO dto) {
		Galpon galpon = modelMapper.map(dto, Galpon.class);
		galpon.setId(null);
		if (galpon.getEstado() == null) {
			galpon.setEstado(EstadoGalpon.DISPONIBLE);
		}
		return aDTO(galponRepository.saveAndFlush(galpon));
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public List<GalponDTO> obtenerTodos() {
		return galponRepository.findAll().stream().map(this::aDTO).toList();
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public GalponDTO obtenerPorId(Long id) {
		return aDTO(buscar(id));
	}

	/**
	 * Actualiza el galpón. Si {@code estado} viene en {@code null}, lo deja como
	 * estaba.
	 *
	 * @param id  identificador del galpón
	 * @param dto nuevos datos del galpón
	 * @return el galpón actualizado
	 */
	@Override
	@Transactional
	public GalponDTO actualizar(Long id, GalponDTO dto) {
		Galpon galpon = buscar(id);
		modelMapper.map(dto, galpon);
		galpon.setId(id);
		return aDTO(galponRepository.saveAndFlush(galpon));
	}

	/**
	 * Elimina un galpón. Si tiene lotes registrados, PostgreSQL lo impide por la
	 * llave foránea.
	 *
	 * @param id identificador del galpón
	 */
	@Override
	@Transactional
	public void eliminar(Long id) {
		galponRepository.delete(buscar(id));
		galponRepository.flush();
	}

	/** {@inheritDoc} */
	@Override
	public long contar() {
		return galponRepository.count();
	}

	/** {@inheritDoc} */
	@Override
	public boolean existe(Long id) {
		return galponRepository.existsById(id);
	}

	/**
	 * Busca un galpón por su identificador.
	 *
	 * @param id identificador
	 * @return el galpón
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private Galpon buscar(Long id) {
		return galponRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el galpón con id " + id + "."));
	}

	/**
	 * Convierte el galpón en DTO.
	 *
	 * @param galpon entidad
	 * @return DTO del galpón
	 */
	private GalponDTO aDTO(Galpon galpon) {
		return modelMapper.map(galpon, GalponDTO.class);
	}
}
