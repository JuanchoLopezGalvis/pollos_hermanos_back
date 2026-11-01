package co.edu.unbosque.pollos_hermanos_back.exception;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Busca el mensaje en español de una restricción violada en la tabla
 * {@code mensaje_restriccion} de PostgreSQL, la misma que usan los
 * procedimientos. Así el mensaje es el mismo si la restricción salta dentro de
 * un procedimiento o en un INSERT directo.
 */
@Component
public class TraductorRestricciones {

	/** Nombre de la restricción dentro del mensaje de PostgreSQL. */
	private static final Pattern RESTRICCION = Pattern.compile("constraint \"([^\"]+)\"");

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el traductor.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public TraductorRestricciones(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Extrae el nombre de la restricción del mensaje de PostgreSQL.
	 *
	 * @param mensaje mensaje del error
	 * @return nombre de la restricción, o {@code null} si no aparece
	 */
	static String nombreRestriccion(String mensaje) {
		if (mensaje == null) {
			return null;
		}
		Matcher m = RESTRICCION.matcher(mensaje);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Traduce la restricción mencionada en un mensaje de PostgreSQL.
	 *
	 * @param mensajePostgres mensaje original del error
	 * @return mensaje en español, o {@code null} si no hay uno registrado
	 */
	public String traducir(String mensajePostgres) {
		String restriccion = nombreRestriccion(mensajePostgres);
		if (restriccion == null) {
			return null;
		}
		try {
			List<String> mensajes = jdbcTemplate.queryForList(
					"SELECT mensaje FROM mensaje_restriccion WHERE restriccion = ?", String.class, restriccion);
			return mensajes.isEmpty() ? null : mensajes.get(0);
		} catch (DataAccessException e) {
			return null;
		}
	}
}
