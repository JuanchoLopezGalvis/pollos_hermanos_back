package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.pollos_hermanos_back.entity.Permiso;

/**
 * Repositorio JPA de los permisos del sistema.
 */
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

	/**
	 * Busca los permisos cuyos códigos están en la colección.
	 *
	 * @param codigos códigos de los permisos
	 * @return permisos encontrados
	 */
	List<Permiso> findByCodigoIn(Collection<String> codigos);
}
