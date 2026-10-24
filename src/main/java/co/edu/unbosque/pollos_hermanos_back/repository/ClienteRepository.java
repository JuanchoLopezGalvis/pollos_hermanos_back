package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroClienteDTO;

/**
 * Acceso a los clientes. Las restricciones de PostgreSQL exigen documento,
 * teléfono y correo únicos, y al menos un medio de contacto.
 */
@Repository
public class ClienteRepository {

	/** Convierte las filas de {@code v_cliente} en DTO. */
	private static final DataClassRowMapper<ClienteDTO> MAPEO = new DataClassRowMapper<>(ClienteDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public ClienteRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los clientes ordenados por nombre.
	 *
	 * @return clientes
	 */
	public List<ClienteDTO> obtenerTodos() {
		return jdbcTemplate.query("SELECT * FROM v_cliente ORDER BY nombre", MAPEO);
	}

	/**
	 * Busca un cliente.
	 *
	 * @param id identificador
	 * @return el cliente, si existe
	 */
	public Optional<ClienteDTO> buscar(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_cliente WHERE id_cliente = ?", MAPEO, id).stream().findFirst();
	}

	/**
	 * Registra un cliente activo.
	 *
	 * @param dto datos del cliente
	 * @return identificador asignado
	 */
	public Long crear(RegistroClienteDTO dto) {
		return jdbcTemplate.queryForObject("INSERT INTO cliente (id_tipo_cliente, documento, nombre, direccion, telefono, correo)"
				+ " VALUES (?, ?, ?, ?, ?, ?) RETURNING id_cliente", Long.class, dto.idTipoCliente(), dto.documento().trim(),
				dto.nombre().trim(), EmpleadoRepository.vacioANulo(dto.direccion()), EmpleadoRepository.vacioANulo(dto.telefono()),
				EmpleadoRepository.vacioANulo(dto.correo()));
	}

	/**
	 * Modifica un cliente. Si {@code activo} es {@code null}, no cambia.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return {@code true} si existía
	 */
	public boolean actualizar(Long id, RegistroClienteDTO dto) {
		return jdbcTemplate.update("UPDATE cliente SET id_tipo_cliente = ?, documento = ?, nombre = ?, direccion = ?, telefono = ?,"
				+ " correo = ?, activo = COALESCE(?, activo) WHERE id_cliente = ?", dto.idTipoCliente(), dto.documento().trim(),
				dto.nombre().trim(), EmpleadoRepository.vacioANulo(dto.direccion()), EmpleadoRepository.vacioANulo(dto.telefono()),
				EmpleadoRepository.vacioANulo(dto.correo()), dto.activo(), id) > 0;
	}
}
