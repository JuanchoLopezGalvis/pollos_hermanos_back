package co.edu.unbosque.pollos_hermanos_back.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Carga los usuarios desde PostgreSQL para Spring Security. Acepta el nombre de
 * usuario o el correo.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarioRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarioRepository repositorio de usuarios
	 */
	public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	/**
	 * Busca el usuario por nombre de usuario o correo.
	 *
	 * @param usuario nombre de usuario o correo
	 * @return el usuario con sus roles y permisos
	 * @throws UsernameNotFoundException si no existe
	 */
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String usuario) {
		return usuarioRepository.findByNombreUsuarioOrCorreo(usuario, usuario)
				.orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + usuario + "."));
	}
}
