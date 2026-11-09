package co.edu.unbosque.pollos_hermanos_back.repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.AuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;

/**
 * Consulta de la tabla {@code auditoria}, que llenan los triggers del módulo 10.
 * La aplicación solo la lee: no tiene permiso para modificarla.
 */
@Repository
public class AuditoriaRepository {

	/** Columnas que se leen. */
	private static final String SELECT = "SELECT id_auditoria, tabla, operacion, id_registro, columnas_cambiadas,"
			+ " datos_antes::text AS datos_antes, datos_despues::text AS datos_despues, usuario_app, usuario_bd,"
			+ " id_transaccion, fecha_creacion FROM auditoria";

	/** Convierte las filas en DTO. */
	private static final RowMapper<AuditoriaDTO> MAPEO = (rs, i) -> new AuditoriaDTO(rs.getLong("id_auditoria"),
			rs.getString("tabla"), rs.getString("operacion"), rs.getString("id_registro"), columnas(rs),
			rs.getString("datos_antes"), rs.getString("datos_despues"), rs.getString("usuario_app"),
			rs.getString("usuario_bd"), rs.getLong("id_transaccion"),
			rs.getTimestamp("fecha_creacion").toLocalDateTime());

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public AuditoriaRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Busca los cambios que cumplen los filtros, del más reciente al más antiguo.
	 *
	 * @param filtro  filtros; los vacíos no se aplican
	 * @param limite  máximo de cambios
	 * @param desplazamiento cambios que se saltan
	 * @return cambios encontrados
	 */
	public List<AuditoriaDTO> buscar(FiltroAuditoriaDTO filtro, int limite, long desplazamiento) {
		List<Object> parametros = new ArrayList<>();
		String where = where(filtro, parametros);
		parametros.add(limite);
		parametros.add(desplazamiento);
		return jdbcTemplate.query(SELECT + where + " ORDER BY id_auditoria DESC LIMIT ? OFFSET ?", MAPEO,
				parametros.toArray());
	}

	/**
	 * Cuenta los cambios que cumplen los filtros.
	 *
	 * @param filtro filtros; los vacíos no se aplican
	 * @return cantidad de cambios
	 */
	public long contar(FiltroAuditoriaDTO filtro) {
		List<Object> parametros = new ArrayList<>();
		String where = where(filtro, parametros);
		Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM auditoria" + where, Long.class,
				parametros.toArray());
		return total == null ? 0 : total;
	}

	/**
	 * Obtiene las tablas que tienen cambios registrados.
	 *
	 * @return nombres de las tablas, en orden alfabético
	 */
	public List<String> tablas() {
		return jdbcTemplate.queryForList("SELECT DISTINCT tabla FROM auditoria ORDER BY tabla", String.class);
	}

	/**
	 * Arma la cláusula WHERE con los filtros que traen valor y agrega sus
	 * parámetros en el mismo orden.
	 *
	 * @param filtro     filtros
	 * @param parametros lista donde se agregan los parámetros
	 * @return cláusula WHERE, o cadena vacía si no hay filtros
	 */
	static String where(FiltroAuditoriaDTO filtro, List<Object> parametros) {
		List<String> condiciones = new ArrayList<>();
		String tabla = EmpleadoRepository.vacioANulo(filtro.tabla());
		if (tabla != null) {
			condiciones.add("tabla = ?");
			parametros.add(tabla);
		}
		String operacion = EmpleadoRepository.vacioANulo(filtro.operacion());
		if (operacion != null) {
			condiciones.add("operacion = ?");
			parametros.add(operacion.toUpperCase());
		}
		String usuario = EmpleadoRepository.vacioANulo(filtro.usuario());
		if (filtro.soloSistema()) {
			condiciones.add("usuario_app IS NULL");
		} else if (usuario != null) {
			condiciones.add("usuario_app ILIKE ?");
			parametros.add("%" + usuario.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
		}
		String idRegistro = EmpleadoRepository.vacioANulo(filtro.idRegistro());
		if (idRegistro != null) {
			condiciones.add("id_registro = ?");
			parametros.add(idRegistro);
		}
		if (filtro.desde() != null) {
			condiciones.add("fecha_creacion >= ?");
			parametros.add(filtro.desde().atStartOfDay());
		}
		if (filtro.hasta() != null) {
			condiciones.add("fecha_creacion < ?");
			parametros.add(filtro.hasta().plusDays(1).atStartOfDay());
		}
		return condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
	}

	/**
	 * Lee el arreglo {@code columnas_cambiadas}.
	 *
	 * @param rs fila actual
	 * @return columnas cambiadas, o {@code null} si no es un UPDATE
	 * @throws SQLException si falla la lectura
	 */
	private static List<String> columnas(ResultSet rs) throws SQLException {
		Array arreglo = rs.getArray("columnas_cambiadas");
		return arreglo == null ? null : Arrays.asList((String[]) arreglo.getArray());
	}
}
