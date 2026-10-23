package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.dto.UsuarioDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Administra los usuarios del sistema, sus roles y sus permisos directos.
 * <p>
 * Los usuarios no se borran: {@link #eliminar(Long)} los desactiva para
 * conservar su historial, como se recomienda con {@code is_active} en Django.
 */
@Service
public class UsuarioService implements CRUDOperation<UsuarioDTO> {

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarioRepository;

	/** Servicio de roles, para resolver los nombres. */
	private final RolService rolService;

	/** Servicio de permisos, para resolver los códigos. */
	private final PermisoService permisoService;

	/** Codificador de contraseñas con BCrypt. */
	private final PasswordEncoder passwordEncoder;

	/** Conversor entre entidad y DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarioRepository repositorio de usuarios
	 * @param rolService        servicio de roles
	 * @param permisoService    servicio de permisos
	 * @param passwordEncoder   codificador de contraseñas
	 * @param modelMapper       conversor entre entidad y DTO
	 */
	public UsuarioService(UsuarioRepository usuarioRepository, RolService rolService, PermisoService permisoService,
			PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
		this.usuarioRepository = usuarioRepository;
		this.rolService = rolService;
		this.permisoService = permisoService;
		this.passwordEncoder = passwordEncoder;
		this.modelMapper = modelMapper;
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public UsuarioDTO crear(UsuarioDTO dto) {
		Usuario usuario = modelMapper.map(dto, Usuario.class);
		usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
		asignarRelaciones(usuario, dto);
		return aDTO(usuarioRepository.save(usuario));
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public List<UsuarioDTO> obtenerTodos() {
		return usuarioRepository.findAll().stream().map(this::aDTO).toList();
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly = true)
	public UsuarioDTO obtenerPorId(Long id) {
		return aDTO(buscar(id));
	}

	/**
	 * Actualiza el usuario. Solo cambian los campos enviados; si llega una
	 * contraseña, se cifra y reemplaza la anterior.
	 *
	 * @param id  identificador del usuario
	 * @param dto nuevos datos del usuario
	 * @return el usuario actualizado
	 */
	@Override
	@Transactional
	public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {
		Usuario usuario = buscar(id);
		modelMapper.map(dto, usuario);
		if (dto.getContrasena() != null && !dto.getContrasena().isBlank()) {
			usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
		}
		asignarRelaciones(usuario, dto);
		return aDTO(usuarioRepository.save(usuario));
	}

	/**
	 * Desactiva el usuario en lugar de borrarlo, para conservar su historial.
	 *
	 * @param id identificador del usuario
	 */
	@Override
	@Transactional
	public void eliminar(Long id) {
		Usuario usuario = buscar(id);
		usuario.setActivo(false);
		usuarioRepository.save(usuario);
	}

	/** {@inheritDoc} */
	@Override
	public long contar() {
		return usuarioRepository.count();
	}

	/** {@inheritDoc} */
	@Override
	public boolean existe(Long id) {
		return usuarioRepository.existsById(id);
	}

	/**
	 * Reemplaza los roles y los permisos directos que vengan en el DTO; los que
	 * vengan en {@code null} se dejan como estaban.
	 *
	 * @param usuario entidad a modificar
	 * @param dto     datos recibidos
	 */
	private void asignarRelaciones(Usuario usuario, UsuarioDTO dto) {
		if (dto.getRoles() != null) {
			usuario.setRoles(rolService.buscarPorNombres(dto.getRoles()));
		}
		if (dto.getPermisos() != null) {
			usuario.setPermisos(permisoService.buscarPorCodigos(dto.getPermisos()));
		}
	}

	/**
	 * Busca un usuario por su identificador.
	 *
	 * @param id identificador
	 * @return el usuario
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private Usuario buscar(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id + "."));
	}

	/**
	 * Convierte el usuario en DTO, sin la contraseña y con sus roles y permisos
	 * por nombre o código.
	 *
	 * @param usuario entidad
	 * @return DTO del usuario
	 */
	private UsuarioDTO aDTO(Usuario usuario) {
		UsuarioDTO dto = modelMapper.map(usuario, UsuarioDTO.class);
		Set<String> roles = new TreeSet<>();
		usuario.getRoles().stream().map(Rol::getNombre).forEach(roles::add);
		Set<String> permisos = new TreeSet<>();
		usuario.getPermisos().stream().map(Permiso::getCodigo).forEach(permisos::add);
		dto.setRoles(List.copyOf(roles));
		dto.setPermisos(List.copyOf(permisos));
		dto.setPermisosEfectivos(usuario.obtenerTodosLosPermisos());
		return dto;
	}
}
