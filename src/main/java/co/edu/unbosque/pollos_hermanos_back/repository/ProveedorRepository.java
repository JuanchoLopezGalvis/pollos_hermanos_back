package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.ProveedorDTO;

/**
 * Acceso a los proveedores. La restricción {@code ck_proveedor_suministra} exige
 * que suministren alimento, insumos sanitarios o ambos.
 */
@Repository
public class ProveedorRepository {

	/** Columnas que se leen. */
	private static final String SELECT = "SELECT id_proveedor, nit, nombre, telefono, correo, direccion, suministra_alimento,"
			+ " suministra_sanidad, activo FROM proveedor";

	/** Convierte las filas en DTO. */
	private static final DataClassRowMapper<ProveedorDTO> MAPEO = new DataClassRowMapper<>(ProveedorDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public ProveedorRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los proveedores ordenados por nombre.
	 *
	 * @return proveedores
	 */
	public List<ProveedorDTO> obtenerTodos() {
		return jdbcTemplate.query(SELECT + " ORDER BY nombre", MAPEO);
	}

	/**
	 * Busca un proveedor.
	 *
	 * @param id identificador
	 * @return el proveedor, si existe
	 */
	public Optional<ProveedorDTO> buscar(Long id) {
		return jdbcTemplate.query(SELECT + " WHERE id_proveedor = ?", MAPEO, id).stream().findFirst();
	}

	/**
	 * Registra un proveedor activo.
	 *
	 * @param dto datos del proveedor
	 * @return identificador asignado
	 */
	public Long crear(ProveedorDTO dto) {
		return jdbcTemplate.queryForObject("INSERT INTO proveedor (nit, nombre, telefono, correo, direccion, suministra_alimento,"
				+ " suministra_sanidad) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_proveedor", Long.class, dto.nit().trim(),
				dto.nombre().trim(), EmpleadoRepository.vacioANulo(dto.telefono()), EmpleadoRepository.vacioANulo(dto.correo()),
				EmpleadoRepository.vacioANulo(dto.direccion()), dto.suministraAlimento(), dto.suministraSanidad());
	}

	/**
	 * Modifica un proveedor. Si {@code activo} es {@code null}, no cambia.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return {@code true} si existía
	 */
	public boolean actualizar(Long id, ProveedorDTO dto) {
		return jdbcTemplate.update("UPDATE proveedor SET nit = ?, nombre = ?, telefono = ?, correo = ?, direccion = ?,"
				+ " suministra_alimento = ?, suministra_sanidad = ?, activo = COALESCE(?, activo) WHERE id_proveedor = ?",
				dto.nit().trim(), dto.nombre().trim(), EmpleadoRepository.vacioANulo(dto.telefono()),
				EmpleadoRepository.vacioANulo(dto.correo()), EmpleadoRepository.vacioANulo(dto.direccion()),
				dto.suministraAlimento(), dto.suministraSanidad(), dto.activo(), id) > 0;
	}
}
