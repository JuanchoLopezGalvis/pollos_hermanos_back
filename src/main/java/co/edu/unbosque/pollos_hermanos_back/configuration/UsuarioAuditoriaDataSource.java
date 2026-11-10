package co.edu.unbosque.pollos_hermanos_back.configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Envuelve el {@link DataSource} para que los triggers de auditoría de
 * PostgreSQL sepan qué usuario de la aplicación hizo cada cambio.
 * <p>
 * La aplicación se conecta siempre con el mismo rol ({@code pollos_app}), así
 * que cada vez que entrega una conexión del pool fija en la sesión la variable
 * {@code pollos.usuario} con el usuario autenticado, o la deja vacía si no hay
 * ninguno (aviso de Wompi, tarea programada, registro público). Se fija a nivel
 * de sesión y no de transacción porque los procedimientos hacen COMMIT por
 * dentro; como se vuelve a fijar en cada préstamo, una conexión reutilizada
 * nunca conserva el usuario de la solicitud anterior.
 */
public class UsuarioAuditoriaDataSource extends DelegatingDataSource {

	/** Variable de sesión que lee {@code fn_trg_auditoria}. */
	static final String FIJAR_USUARIO = "SELECT set_config('pollos.usuario', ?, false)";

	/**
	 * Crea el envoltorio.
	 *
	 * @param destino pool de conexiones real
	 */
	public UsuarioAuditoriaDataSource(DataSource destino) {
		super(destino);
	}

	/**
	 * Entrega una conexión del pool con el usuario autenticado ya fijado.
	 *
	 * @return conexión lista para usar
	 * @throws SQLException si no se obtiene la conexión o no se fija el usuario
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return conUsuario(super.getConnection());
	}

	/**
	 * Entrega una conexión con credenciales explícitas y el usuario autenticado
	 * ya fijado.
	 *
	 * @param username usuario de la base de datos
	 * @param password contraseña de la base de datos
	 * @return conexión lista para usar
	 * @throws SQLException si no se obtiene la conexión o no se fija el usuario
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return conUsuario(super.getConnection(username, password));
	}

	/**
	 * Fija el usuario autenticado en la sesión de la conexión. Si falla, la
	 * devuelve al pool y propaga el error.
	 *
	 * @param conexion conexión recién obtenida
	 * @return la misma conexión
	 * @throws SQLException si no se puede fijar el usuario
	 */
	private Connection conUsuario(Connection conexion) throws SQLException {
		try (PreparedStatement sentencia = conexion.prepareStatement(FIJAR_USUARIO)) {
			sentencia.setString(1, usuarioAutenticado());
			sentencia.execute();
			return conexion;
		} catch (SQLException e) {
			conexion.close();
			throw e;
		}
	}

	/**
	 * Devuelve el nombre del usuario autenticado en la solicitud actual.
	 *
	 * @return nombre de usuario, o cadena vacía si no hay usuario autenticado
	 */
	static String usuarioAutenticado() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !autenticacion.isAuthenticated()
				|| autenticacion instanceof AnonymousAuthenticationToken) {
			return "";
		}
		return autenticacion.getName();
	}
}
