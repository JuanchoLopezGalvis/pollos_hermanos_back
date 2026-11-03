package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.CatalogoDTO;

/**
 * Lee los catálogos de PostgreSQL. Solo se consultan: sus valores los cargan los
 * scripts de cada módulo, porque los triggers validan por su código.
 */
@Repository
public class CatalogoRepository {

	/** Tablas de catálogo que se exponen, por el nombre que usa la API. */
	private static final Map<String, String> TABLAS = Map.of(
			"cargos", "SELECT id_cargo AS id, codigo, nombre FROM cargo WHERE activo ORDER BY nombre",
			"tipos-cliente", "SELECT id_tipo_cliente AS id, codigo, nombre FROM tipo_cliente WHERE activo ORDER BY nombre",
			"metodos-pago", "SELECT id_metodo_pago AS id, codigo, nombre FROM metodo_pago WHERE activo ORDER BY id_metodo_pago",
			"categorias-gasto", "SELECT id_categoria_gasto AS id, codigo, nombre FROM categoria_gasto WHERE activo ORDER BY nombre");

	/** Convierte las filas en DTO. */
	private static final DataClassRowMapper<CatalogoDTO> MAPEO = new DataClassRowMapper<>(CatalogoDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public CatalogoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los elementos activos de un catálogo.
	 *
	 * @param nombre nombre del catálogo en la API, por ejemplo {@code cargos}
	 * @return elementos del catálogo, o vacío si el catálogo no existe
	 */
	public Optional<List<CatalogoDTO>> obtener(String nombre) {
		String consulta = TABLAS.get(nombre);
		return consulta == null ? Optional.empty() : Optional.of(jdbcTemplate.query(consulta, MAPEO));
	}
}
