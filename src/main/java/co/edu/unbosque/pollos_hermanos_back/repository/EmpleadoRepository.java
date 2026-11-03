package co.edu.unbosque.pollos_hermanos_back.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.EmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEmpleadoDTO;

/**
 * Acceso a los empleados en PostgreSQL. El trigger {@code trg_empleado_validar}
 * impide reactivar a un retirado o cambiarle el cargo; el retiro pasa por
 * {@code sp_retirar_empleado}.
 */
@Repository
public class EmpleadoRepository {

	/** Convierte las filas de {@code v_empleado} en DTO. */
	private static final DataClassRowMapper<EmpleadoDTO> MAPEO = new DataClassRowMapper<>(EmpleadoDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public EmpleadoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los empleados, opcionalmente filtrados.
	 *
	 * @param soloActivos  {@code true} para excluir a los retirados
	 * @param codigoCargo  código del cargo, o {@code null} para todos
	 * @return empleados ordenados por apellidos
	 */
	public List<EmpleadoDTO> obtener(boolean soloActivos, String codigoCargo) {
		StringBuilder sql = new StringBuilder("SELECT * FROM v_empleado WHERE TRUE");
		List<Object> parametros = new ArrayList<>();
		if (soloActivos) {
			sql.append(" AND activo");
		}
		if (codigoCargo != null && !codigoCargo.isBlank()) {
			sql.append(" AND codigo_cargo = ?");
			parametros.add(codigoCargo);
		}
		sql.append(" ORDER BY apellidos, nombres");
		return jdbcTemplate.query(sql.toString(), MAPEO, parametros.toArray());
	}

	/**
	 * Busca un empleado.
	 *
	 * @param id identificador
	 * @return el empleado, si existe
	 */
	public Optional<EmpleadoDTO> buscar(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_empleado WHERE id_empleado = ?", MAPEO, id).stream().findFirst();
	}

	/**
	 * Registra un empleado activo.
	 *
	 * @param dto datos del empleado
	 * @return identificador asignado
	 */
	public Long crear(RegistroEmpleadoDTO dto) {
		return jdbcTemplate.queryForObject("INSERT INTO empleado (documento, nombres, apellidos, telefono, id_cargo, fecha_ingreso)"
				+ " VALUES (?, ?, ?, ?, ?, ?) RETURNING id_empleado", Long.class, dto.documento().trim(), dto.nombres().trim(),
				dto.apellidos().trim(), vacioANulo(dto.telefono()), dto.idCargo(), dto.fechaIngreso());
	}

	/**
	 * Modifica los datos de un empleado.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return {@code true} si el empleado existía
	 */
	public boolean actualizar(Long id, RegistroEmpleadoDTO dto) {
		return jdbcTemplate.update("UPDATE empleado SET documento = ?, nombres = ?, apellidos = ?, telefono = ?, id_cargo = ?,"
				+ " fecha_ingreso = ? WHERE id_empleado = ?", dto.documento().trim(), dto.nombres().trim(),
				dto.apellidos().trim(), vacioANulo(dto.telefono()), dto.idCargo(), dto.fechaIngreso(), id) > 0;
	}

	/**
	 * Retira un empleado con {@code sp_retirar_empleado}.
	 *
	 * @param id          identificador
	 * @param fechaRetiro fecha de retiro, o {@code null} para hoy
	 */
	public void retirar(Long id, LocalDate fechaRetiro) {
		jdbcTemplate.update("CALL sp_retirar_empleado(?, ?)", id, fechaRetiro);
	}

	/**
	 * Convierte un texto vacío en {@code null}.
	 *
	 * @param texto texto recibido
	 * @return el texto sin espacios, o {@code null} si queda vacío
	 */
	static String vacioANulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}
}
