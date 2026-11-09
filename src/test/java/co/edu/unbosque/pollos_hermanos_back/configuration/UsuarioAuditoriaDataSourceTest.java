package co.edu.unbosque.pollos_hermanos_back.configuration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pruebas de {@link UsuarioAuditoriaDataSource}.
 */
class UsuarioAuditoriaDataSourceTest {

	/** Pool simulado. */
	private final DataSource destino = mock(DataSource.class);

	/** Conexión simulada. */
	private final Connection conexion = mock(Connection.class);

	/** Sentencia simulada que fija el usuario. */
	private final PreparedStatement sentencia = mock(PreparedStatement.class);

	/** DataSource bajo prueba. */
	private final UsuarioAuditoriaDataSource dataSource = new UsuarioAuditoriaDataSource(destino);

	/**
	 * Prepara el pool para devolver la conexión simulada.
	 *
	 * @throws SQLException no ocurre con los simulados
	 */
	@BeforeEach
	void preparar() throws SQLException {
		when(destino.getConnection()).thenReturn(conexion);
		when(conexion.prepareStatement(UsuarioAuditoriaDataSource.FIJAR_USUARIO)).thenReturn(sentencia);
	}

	/** Limpia el usuario autenticado. */
	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	/**
	 * Con un usuario autenticado, la conexión sale con su nombre fijado.
	 *
	 * @throws SQLException no ocurre con los simulados
	 */
	@Test
	void fijaElUsuarioAutenticado() throws SQLException {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"gerente", null, List.of(new SimpleGrantedAuthority("lote.crear"))));

		assertSame(conexion, dataSource.getConnection());
		verify(sentencia).setString(1, "gerente");
		verify(sentencia).execute();
	}

	/**
	 * Sin usuario autenticado (aviso de Wompi o tarea programada), el usuario
	 * queda vacío para no heredar el de la solicitud anterior.
	 *
	 * @throws SQLException no ocurre con los simulados
	 */
	@Test
	void sinUsuarioLoDejaVacio() throws SQLException {
		dataSource.getConnection();

		verify(sentencia).setString(1, "");
	}

	/**
	 * El usuario anónimo de Spring Security no cuenta como autenticado.
	 *
	 * @throws SQLException no ocurre con los simulados
	 */
	@Test
	void usuarioAnonimoQuedaVacio() throws SQLException {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		dataSource.getConnection();

		verify(sentencia).setString(1, "");
	}

	/**
	 * Si no se puede fijar el usuario, la conexión vuelve al pool y el error se
	 * propaga.
	 *
	 * @throws SQLException no ocurre con los simulados
	 */
	@Test
	void siFallaDevuelveLaConexion() throws SQLException {
		when(sentencia.execute()).thenThrow(new SQLException("sin conexión"));

		assertThrows(SQLException.class, dataSource::getConnection);
		verify(conexion).close();
	}
}
