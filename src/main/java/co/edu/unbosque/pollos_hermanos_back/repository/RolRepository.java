package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.pollos_hermanos_back.entity.Rol;

/**
 * Repositorio JPA de los roles del sistema.
 */
public interface RolRepository extends JpaRepository<Rol, Long> {

	/**
	 * Busca los roles cuyos nombres están en la colección.
	 *
	 * @param nombres nombres de los roles
	 * @return roles encontrados
	 */
	List<Rol> findByNombreIn(Collection<String> nombres);

	/**
	 * Busca un rol por su nombre.
	 *
	 * @param nombre nombre del rol
	 * @return el rol, si existe
	 */
	Optional<Rol> findByNombre(String nombre);
}
