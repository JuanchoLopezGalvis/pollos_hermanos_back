package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;

/**
 * Repositorio JPA de los usuarios del sistema.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/**
	 * Busca un usuario por su nombre de usuario o por su correo.
	 *
	 * @param nombreUsuario nombre de usuario
	 * @param correo        correo
	 * @return el usuario, si existe
	 */
	Optional<Usuario> findByNombreUsuarioOrCorreo(String nombreUsuario, String correo);

	/**
	 * Indica si ya existe un usuario con ese nombre de usuario.
	 *
	 * @param nombreUsuario nombre de usuario
	 * @return {@code true} si existe
	 */
	boolean existsByNombreUsuario(String nombreUsuario);
}
