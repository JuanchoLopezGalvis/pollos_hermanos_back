package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.CatalogoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.CatalogoRepository;

/**
 * Consulta los catálogos del sistema: cargos, tipos de cliente, métodos de pago
 * y categorías de gasto.
 */
@Service
public class CatalogoService {

	/** Acceso a los catálogos. */
	private final CatalogoRepository catalogoRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param catalogoRepository acceso a los catálogos
	 */
	public CatalogoService(CatalogoRepository catalogoRepository) {
		this.catalogoRepository = catalogoRepository;
	}

	/**
	 * Obtiene los elementos activos de un catálogo.
	 *
	 * @param nombre nombre del catálogo, por ejemplo {@code cargos}
	 * @return elementos del catálogo
	 * @throws RecursoNoEncontradoException si el catálogo no existe
	 */
	public List<CatalogoDTO> obtener(String nombre) {
		return catalogoRepository.obtener(nombre)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el catálogo " + nombre + "."));
	}
}
