package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.dto.RolDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.RolRepository;

/**
 * Administra los roles del sistema y los permisos que otorga cada uno.
 */
@Service
public class RolService implements CRUDOperation<RolDTO> {

	/** Repositorio de roles. */
	private final RolRepository rolRepository;

	/** Servicio de permisos, para resolver los códigos. */
	private final PermisoService permisoService;

	/** Conversor entre entidad y DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param rolRepository  repositorio de roles
	 * @param permisoService servicio de permisos
	 * @param modelMapper    conversor entre entidad y DTO
	 */
	public RolService(RolRepository rolRepository, PermisoService permisoService, ModelMapper modelMapper) {
		this.rolRepository = rolRepository;
		this.permisoService = permisoService;
		this.modelMapper = modelMapper;
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public RolDTO crear(RolDTO dto) {
		Rol rol = modelMapper.map(dto, Rol.class);
		if (dto.getPermisos() != null) {
			rol.setPermisos(permisoService.buscarPorCodigos(dto.getPermisos()));
		}
		return aDTO(rolRepository.save(rol));
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public List<RolDTO> obtenerTodos() {
		return rolRepository.findAll().stream().map(this::aDTO).toList();
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public RolDTO obtenerPorId(Long id) {
		return aDTO(buscar(id));
	}

	/**
	 * Actualiza el rol. Si {@code permisos} viene en el DTO, reemplaza los permisos
	 * del rol; si viene en {@code null}, los deja como estaban.
	 *
	 * @param id  identificador del rol
	 * @param dto nuevos datos del rol
	 * @return el rol actualizado
	 */
	@Override
	@Transactional
	public RolDTO actualizar(Long id, RolDTO dto) {
		Rol rol = buscar(id);
		modelMapper.map(dto, rol);
		if (dto.getPermisos() != null) {
			rol.setPermisos(permisoService.buscarPorCodigos(dto.getPermisos()));
		}
		return aDTO(rolRepository.save(rol));
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void eliminar(Long id) {
		rolRepository.delete(buscar(id));
	}

	/** {@inheritDoc} */
	@Override
	public long contar() {
		return rolRepository.count();
	}

	/** {@inheritDoc} */
	@Override
	public boolean existe(Long id) {
		return rolRepository.existsById(id);
	}

	/**
	 * Busca los roles por su nombre y verifica que existan todos.
	 *
	 * @param nombres nombres de los roles
	 * @return roles encontrados
	 * @throws RecursoNoEncontradoException si algún nombre no existe
	 */
	@Transactional(readOnly = true)
	public Set<Rol> buscarPorNombres(Collection<String> nombres) {
		Set<String> buscados = new TreeSet<>(nombres);
		Set<Rol> encontrados = new HashSet<>(rolRepository.findByNombreIn(buscados));
		encontrados.forEach(rol -> buscados.remove(rol.getNombre()));
		if (!buscados.isEmpty()) {
			throw new RecursoNoEncontradoException("No existen los roles: " + String.join(", ", buscados) + ".");
		}
		return encontrados;
	}

	/**
	 * Busca un rol por su identificador.
	 *
	 * @param id identificador
	 * @return el rol
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private Rol buscar(Long id) {
		return rolRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol con id " + id + "."));
	}

	/**
	 * Convierte el rol en DTO, con los códigos de sus permisos ordenados.
	 *
	 * @param rol entidad
	 * @return DTO del rol
	 */
	private RolDTO aDTO(Rol rol) {
		RolDTO dto = modelMapper.map(rol, RolDTO.class);
		Set<String> codigos = new TreeSet<>();
		rol.getPermisos().stream().map(Permiso::getCodigo).forEach(codigos::add);
		dto.setPermisos(List.copyOf(codigos));
		return dto;
	}
}
