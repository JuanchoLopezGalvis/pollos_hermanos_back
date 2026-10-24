package co.edu.unbosque.pollos_hermanos_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.pollos_hermanos_back.entity.Galpon;

/**
 * Repositorio JPA de galpones.
 */
public interface GalponRepository extends JpaRepository<Galpon, Long> {
}
