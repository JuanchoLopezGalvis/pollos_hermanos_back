package co.edu.unbosque.pollos_hermanos_back.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Un cambio registrado por los triggers de auditoría
 * ({@code sql/modulo_10_auditoria.sql}).
 *
 * @param idAuditoria        identificador
 * @param tabla              tabla donde ocurrió el cambio
 * @param operacion          INSERT, UPDATE, DELETE o TRUNCATE
 * @param idRegistro         llave primaria de la fila (las compuestas separadas por coma)
 * @param columnasCambiadas  columnas que cambiaron, solo en UPDATE
 * @param datosAntes         fila antes del cambio, como objeto JSON (UPDATE y DELETE)
 * @param datosDespues       fila después del cambio, como objeto JSON (INSERT y UPDATE)
 * @param usuarioApp         usuario de la aplicación; {@code null} si lo hizo el sistema
 * @param usuarioBd          rol de la base de datos de la sesión
 * @param idTransaccion      transacción que agrupa los cambios de una misma operación
 * @param fecha              fecha y hora del cambio
 */
public record AuditoriaDTO(
		Long idAuditoria,
		String tabla,
		String operacion,
		String idRegistro,
		List<String> columnasCambiadas,
		@JsonRawValue String datosAntes,
		@JsonRawValue String datosDespues,
		String usuarioApp,
		String usuarioBd,
		Long idTransaccion,
		LocalDateTime fecha) {
}
