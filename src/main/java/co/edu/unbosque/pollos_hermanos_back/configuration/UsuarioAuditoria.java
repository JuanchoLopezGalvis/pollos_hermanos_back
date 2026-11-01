package co.edu.unbosque.pollos_hermanos_back.configuration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Fija el usuario de la auditoría dentro de la transacción en curso, para las
 * operaciones donde todavía no hay nadie autenticado pero ya se sabe quién es:
 * el inicio de sesión (después de validar la contraseña) y el registro público
 * de un cliente.
 * <p>
 * {@link UsuarioAuditoriaDataSource} fija el usuario al tomar la conexión, y en
 * esos dos casos la conexión se toma antes de saberlo. El valor se fija solo
 * para la transacción ({@code set_config(..., true)}): al terminar, la sesión
 * vuelve a quedar sin usuario.
 */
@Component
public class UsuarioAuditoria {

	/** Fija el usuario hasta el final de la transacción. */
	static final String FIJAR_EN_TRANSACCION = "SELECT set_config('pollos.usuario', ?, true)";

	/** Cliente JDBC; participa en la transacción de Spring en curso. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el componente.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public UsuarioAuditoria(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Registra a {@code nombreUsuario} como autor de los cambios que haga la
	 * transacción en curso. Debe llamarse dentro de un método
	 * {@code @Transactional}.
	 *
	 * @param nombreUsuario usuario que hace los cambios
	 */
	public void fijarEnTransaccion(String nombreUsuario) {
		jdbcTemplate.queryForObject(FIJAR_EN_TRANSACCION, String.class, nombreUsuario);
	}
}
