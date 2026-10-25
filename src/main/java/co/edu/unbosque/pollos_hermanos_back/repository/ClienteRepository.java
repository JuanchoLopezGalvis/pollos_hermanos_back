package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCuentaClienteDTO;

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

	/**
	 * Busca el cliente vinculado a un usuario (D21).
	 *
	 * @param idUsuario identificador del usuario
	 * @return el cliente, o vacío si el usuario no es cliente
	 */
	public Optional<ClienteDTO> buscarPorUsuario(Long idUsuario) {
		return jdbcTemplate.query("SELECT * FROM v_cliente WHERE id_usuario = ?", MAPEO, idUsuario).stream().findFirst();
	}

	/**
	 * Indica si ya hay un cliente con ese documento.
	 *
	 * @param documento documento o NIT
	 * @return {@code true} si existe
	 */
	public boolean existeDocumento(String documento) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM cliente WHERE documento = ?)",
				Boolean.class, documento.trim()));
	}

	/**
	 * Registra el cliente que crea su cuenta en el portal, ya vinculado a su
	 * usuario. Corre en la transacción que creó el usuario.
	 *
	 * @param dto       datos del registro
	 * @param idUsuario usuario recién creado
	 * @return identificador del cliente
	 */
	public Long crearConUsuario(RegistroCuentaClienteDTO dto, Long idUsuario) {
		return jdbcTemplate.queryForObject("INSERT INTO cliente (id_tipo_cliente, documento, nombre, direccion, telefono, correo,"
				+ " id_usuario) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_cliente", Long.class, dto.idTipoCliente(),
				dto.documento().trim(), dto.nombre().trim(), dto.direccion().trim(), EmpleadoRepository.vacioANulo(dto.telefono()),
				dto.correo().trim(), idUsuario);
	}

	/**
	 * Vincula un usuario a un cliente que todavía no tiene cuenta.
	 *
	 * @param idCliente identificador del cliente
	 * @param idUsuario identificador del usuario
	 * @return {@code true} si el cliente existía y no tenía cuenta
	 */
	public boolean vincularUsuario(Long idCliente, Long idUsuario) {
		return jdbcTemplate.update("UPDATE cliente SET id_usuario = ? WHERE id_cliente = ? AND id_usuario IS NULL", idUsuario,
				idCliente) > 0;
	}
}
