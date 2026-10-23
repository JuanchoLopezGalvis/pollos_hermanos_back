package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.dto.PermisoDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.PermisoRepository;

/**
 * Administra el catálogo de permisos del sistema.
 */
@Service
public class PermisoService implements CRUDOperation<PermisoDTO> {

	/** Repositorio de permisos. */
	private final PermisoRepository permisoRepository;

	/** Conversor entre entidad y DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param permisoRepository repositorio de permisos
	 * @param modelMapper       conversor entre entidad y DTO
	 */
	public PermisoService(PermisoRepository permisoRepository, ModelMapper modelMapper) {
		this.permisoRepository = permisoRepository;
		this.modelMapper = modelMapper;
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public PermisoDTO crear(PermisoDTO dto) {
		Permiso permiso = modelMapper.map(dto, Permiso.class);
		permiso.setId(null);
		return modelMapper.map(permisoRepository.save(permiso), PermisoDTO.class);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public List<PermisoDTO> obtenerTodos() {
		return permisoRepository.findAll().stream().map(permiso -> modelMapper.map(permiso, PermisoDTO.class))
				.toList();
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public PermisoDTO obtenerPorId(Long id) {
		return modelMapper.map(buscar(id), PermisoDTO.class);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public PermisoDTO actualizar(Long id, PermisoDTO dto) {
		Permiso permiso = buscar(id);
		modelMapper.map(dto, permiso);
		permiso.setId(id);
		return modelMapper.map(permisoRepository.save(permiso), PermisoDTO.class);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void eliminar(Long id) {
		permisoRepository.delete(buscar(id));
	}

	/** {@inheritDoc} */
	@Override
	public long contar() {
		return permisoRepository.count();
	}

	/** {@inheritDoc} */
	@Override
	public boolean existe(Long id) {
		return permisoRepository.existsById(id);
	}

	/**
	 * Busca los permisos por su código y verifica que existan todos.
	 *
	 * @param codigos códigos de permiso
	 * @return permisos encontrados
	 * @throws RecursoNoEncontradoException si algún código no existe
	 */
	@Transactional(readOnly = true)
	public Set<Permiso> buscarPorCodigos(Collection<String> codigos) {
		Set<String> buscados = new TreeSet<>(codigos);
		Set<Permiso> encontrados = new HashSet<>(permisoRepository.findByCodigoIn(buscados));
		encontrados.forEach(permiso -> buscados.remove(permiso.getCodigo()));
		if (!buscados.isEmpty()) {
			throw new RecursoNoEncontradoException("No existen los permisos: " + String.join(", ", buscados) + ".");
		}
		return encontrados;
	}

	/**
	 * Busca un permiso por su identificador.
	 *
	 * @param id identificador
	 * @return el permiso
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private Permiso buscar(Long id) {
		return permisoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el permiso con id " + id + "."));
	}
}
