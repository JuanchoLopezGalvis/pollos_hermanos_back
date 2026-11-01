package co.edu.unbosque.pollos_hermanos_back.exception;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;

/**
 * Convierte las excepciones de la aplicación en respuestas HTTP con mensajes en
 * español.
 * <p>
 * Cuando PostgreSQL rechaza una operación ({@code RAISE EXCEPTION} o violación
 * de una restricción) se responde 400 o 409 según el SQLSTATE, nunca 500.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

	/** Registro de errores inesperados. */
	private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

	/** SQLSTATE de {@code RAISE EXCEPTION} sin código propio en PL/pgSQL. */
	static final String RAISE_EXCEPTION = "P0001";
	/** SQLSTATE de violación de llave única. */
	static final String UNIQUE_VIOLATION = "23505";
	/** SQLSTATE de violación de llave foránea. */
	static final String FOREIGN_KEY_VIOLATION = "23503";
	/** SQLSTATE de violación de una restricción CHECK. */
	static final String CHECK_VIOLATION = "23514";
	/** SQLSTATE de valor nulo en una columna obligatoria. */
	static final String NOT_NULL_VIOLATION = "23502";

	/**
	 * Traduce las restricciones con los mensajes de PostgreSQL. Puede faltar (por
	 * ejemplo en las pruebas de controladores); entonces se usan mensajes
	 * genéricos.
	 */
	private final ObjectProvider<TraductorRestricciones> traductor;

	/** Crea el manejador sin traductor de restricciones. */
	public ManejadorGlobalExcepciones() {
		this(null);
	}

	/**
	 * Crea el manejador.
	 *
	 * @param traductor traductor de restricciones, si está disponible
	 */
	@Autowired
	public ManejadorGlobalExcepciones(ObjectProvider<TraductorRestricciones> traductor) {
		this.traductor = traductor;
	}

	/**
	 * Responde 404 cuando el recurso no existe.
	 *
	 * @param e excepción lanzada por el servicio
	 * @return respuesta 404
	 */
	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ErrorApiDTO> manejarNoEncontrado(RecursoNoEncontradoException e) {
		return responder(HttpStatus.NOT_FOUND, e.getMessage());
	}

	/**
	 * Responde 409 cuando se incumple una regla del negocio comprobada en el
	 * backend.
	 *
	 * @param e excepción lanzada por el servicio
	 * @return respuesta 409
	 */
	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<ErrorApiDTO> manejarReglaNegocio(ReglaNegocioException e) {
		return responder(HttpStatus.CONFLICT, e.getMessage());
	}

	/**
	 * Responde 401 cuando el usuario o la contraseña no coinciden.
	 *
	 * @param e excepción de autenticación
	 * @return respuesta 401
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorApiDTO> manejarCredenciales(BadCredentialsException e) {
		return responder(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
	}

	/**
	 * Responde 401 cuando un usuario inactivo intenta iniciar sesión.
	 *
	 * @param e excepción de autenticación
	 * @return respuesta 401
	 */
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErrorApiDTO> manejarInactivo(DisabledException e) {
		return responder(HttpStatus.UNAUTHORIZED, "El usuario está inactivo.");
	}

	/**
	 * Responde 401 ante cualquier otro error de autenticación.
	 *
	 * @param e excepción de autenticación
	 * @return respuesta 401
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorApiDTO> manejarAutenticacion(AuthenticationException e) {
		return responder(HttpStatus.UNAUTHORIZED, "No fue posible iniciar sesión.");
	}

	/**
	 * Responde 403 cuando el usuario no tiene el permiso que exige la operación.
	 *
	 * @param e excepción de autorización
	 * @return respuesta 403
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorApiDTO> manejarAccesoDenegado(AccessDeniedException e) {
		return responder(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.");
	}

	/**
	 * Responde 400 con la lista de campos que no pasaron Bean Validation.
	 *
	 * @param e excepción de validación
	 * @return respuesta 400 con los campos inválidos en {@code detalles}
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorApiDTO> manejarValidacion(MethodArgumentNotValidException e) {
		List<String> detalles = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).toList();
		ErrorApiDTO cuerpo = new ErrorApiDTO(HttpStatus.BAD_REQUEST.value(), "Los datos enviados no son válidos.",
				detalles, LocalDateTime.now());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	/**
	 * Responde 400 cuando el cuerpo de la solicitud no es un JSON válido o tiene
	 * valores con un tipo incorrecto.
	 *
	 * @param e excepción de lectura del cuerpo
	 * @return respuesta 400
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorApiDTO> manejarCuerpoIlegible(HttpMessageNotReadableException e) {
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato válido.");
	}

	/**
	 * Responde 400 cuando un parámetro de la ruta o de la consulta tiene un tipo
	 * incorrecto.
	 *
	 * @param e excepción de conversión del parámetro
	 * @return respuesta 400
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorApiDTO> manejarTipoParametro(MethodArgumentTypeMismatchException e) {
		return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' no tiene un formato válido.");
	}

	/**
	 * Responde 405 cuando la ruta existe pero no admite ese método HTTP, por
	 * ejemplo un POST de pago en una ruta que solo permite consultar.
	 *
	 * @param e excepción de método no soportado
	 * @return respuesta 405
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorApiDTO> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException e) {
		return responder(HttpStatus.METHOD_NOT_ALLOWED, "La operación " + e.getMethod() + " no está permitida en esta ruta.");
	}

	/**
	 * Responde 404 cuando la ruta no existe.
	 *
	 * @param e excepción de recurso inexistente
	 * @return respuesta 404
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorApiDTO> manejarRutaInexistente(NoResourceFoundException e) {
		return responder(HttpStatus.NOT_FOUND, "La ruta /" + e.getResourcePath() + " no existe.");
	}

	/**
	 * Responde 409 cuando PostgreSQL o MongoDB rechazan un registro por un índice
	 * único.
	 *
	 * @param e excepción de llave duplicada
	 * @return respuesta 409
	 */
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<ErrorApiDTO> manejarLlaveDuplicada(DuplicateKeyException e) {
		SQLException sql = buscarSQLException(e);
		String porDefecto = "Ya existe un registro con esos datos.";
		return responder(HttpStatus.CONFLICT, sql == null ? porDefecto : traducir(sql, porDefecto));
	}

	/**
	 * Traduce los errores de acceso a datos. Si PostgreSQL rechazó la operación
	 * responde 400 o 409 según el SQLSTATE; si la base de datos no responde,
	 * responde 503.
	 *
	 * @param e excepción de acceso a datos
	 * @return respuesta con el mensaje traducido
	 */
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorApiDTO> manejarAccesoDatos(DataAccessException e) {
		SQLException sql = buscarSQLException(e);
		if (sql != null && sql.getSQLState() != null) {
			ResponseEntity<ErrorApiDTO> respuesta = traducirSQLException(sql);
			if (respuesta != null) {
				return respuesta;
			}
		}
		if (e instanceof DataAccessResourceFailureException) {
			log.error("La base de datos no está disponible", e);
			return responder(HttpStatus.SERVICE_UNAVAILABLE, "La base de datos no está disponible en este momento.");
		}
		log.error("Error de acceso a datos no controlado", e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error al acceder a los datos.");
	}

	/**
	 * Responde 503 cuando no se pudo enviar un correo.
	 *
	 * @param e excepción del servicio de correo
	 * @return respuesta 503
	 */
	@ExceptionHandler(CorreoException.class)
	public ResponseEntity<ErrorApiDTO> manejarCorreo(CorreoException e) {
		log.error("Error al enviar un correo", e);
		return responder(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

	/**
	 * Responde 500 ante cualquier error no previsto, sin exponer detalles internos.
	 *
	 * @param e excepción no controlada
	 * @return respuesta 500
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorApiDTO> manejarInesperado(Exception e) {
		log.error("Error inesperado", e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.");
	}

	/**
	 * Traduce un error de PostgreSQL según su SQLSTATE.
	 *
	 * @param sql excepción original del controlador JDBC
	 * @return la respuesta traducida, o {@code null} si el SQLSTATE no es un
	 *         rechazo conocido
	 */
	private ResponseEntity<ErrorApiDTO> traducirSQLException(SQLException sql) {
		String estado = sql.getSQLState();
		if (estado == null) {
			return null;
		}
		return switch (estado) {
		case RAISE_EXCEPTION -> responder(HttpStatus.CONFLICT, mensajePostgres(sql));
		case UNIQUE_VIOLATION -> responder(HttpStatus.CONFLICT, traducir(sql, "Ya existe un registro con esos datos."));
		case FOREIGN_KEY_VIOLATION -> responder(HttpStatus.CONFLICT, traducir(sql,
				"El registro hace referencia a un dato que no existe o tiene datos relacionados que impiden la operación."));
		case CHECK_VIOLATION -> responder(HttpStatus.BAD_REQUEST,
				traducir(sql, "Los datos no cumplen una restricción de la base de datos."));
		case NOT_NULL_VIOLATION -> responder(HttpStatus.BAD_REQUEST, "Falta un dato obligatorio.");
		default -> null;
		};
	}

	/**
	 * Busca el mensaje en español de la restricción violada.
	 *
	 * @param sql       excepción original del controlador JDBC
	 * @param porDefecto mensaje si la restricción no tiene uno registrado
	 * @return mensaje para el usuario
	 */
	private String traducir(SQLException sql, String porDefecto) {
		TraductorRestricciones t = traductor == null ? null : traductor.getIfAvailable();
		String mensaje = t == null ? null : t.traducir(sql.getMessage());
		return mensaje == null ? porDefecto : mensaje;
	}

	/**
	 * Extrae el mensaje escrito en el {@code RAISE EXCEPTION}. El controlador de
	 * PostgreSQL lo entrega como {@code "ERROR: <mensaje>"} seguido del contexto en
	 * líneas aparte; se conserva solo el mensaje.
	 *
	 * @param sql excepción original del controlador JDBC
	 * @return mensaje para el usuario
	 */
	static String mensajePostgres(SQLException sql) {
		String mensaje = sql.getMessage();
		if (mensaje == null || mensaje.isBlank()) {
			return "La base de datos rechazó la operación.";
		}
		String primeraLinea = mensaje.lines().findFirst().orElse(mensaje).trim();
		if (primeraLinea.startsWith("ERROR:")) {
			primeraLinea = primeraLinea.substring("ERROR:".length()).trim();
		}
		return primeraLinea;
	}

	/**
	 * Busca la {@link SQLException} original dentro de la cadena de causas.
	 *
	 * @param e excepción lanzada por Spring
	 * @return la excepción de JDBC, o {@code null} si no hay
	 */
	private static SQLException buscarSQLException(Throwable e) {
		Throwable actual = e;
		while (actual != null) {
			if (actual instanceof SQLException sql) {
				return sql;
			}
			actual = actual.getCause();
		}
		return null;
	}

	/**
	 * Construye una respuesta de error sin detalles.
	 *
	 * @param estado  código de estado HTTP
	 * @param mensaje mensaje en español
	 * @return la respuesta
	 */
	private static ResponseEntity<ErrorApiDTO> responder(HttpStatus estado, String mensaje) {
		return ResponseEntity.status(estado).body(ErrorApiDTO.de(estado.value(), mensaje));
	}
}
