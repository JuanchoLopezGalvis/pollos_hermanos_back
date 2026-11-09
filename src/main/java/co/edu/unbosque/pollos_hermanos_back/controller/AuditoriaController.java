package co.edu.unbosque.pollos_hermanos_back.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.FiltroAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PaginaAuditoriaDTO;
import co.edu.unbosque.pollos_hermanos_back.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta de la auditoría: quién creó, modificó o borró cada registro. Es de
 * solo lectura y requiere {@code auditoria.ver}.
 */
@RestController
@RequestMapping("/auditoria")
@Tag(name = "Auditoría", description = "Cambios registrados por los triggers de auditoría de PostgreSQL")
public class AuditoriaController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tiene('auditoria.ver')";

	/** Servicio de auditoría. */
	private final AuditoriaService auditoriaService;

	/**
	 * Crea el controlador.
	 *
	 * @param auditoriaService servicio de auditoría
	 */
	public AuditoriaController(AuditoriaService auditoriaService) {
		this.auditoriaService = auditoriaService;
	}

	/**
	 * Busca una página de cambios, del más reciente al más antiguo.
	 *
	 * @param tabla       tabla exacta
	 * @param operacion   INSERT, UPDATE, DELETE o TRUNCATE
	 * @param usuario     parte del nombre del usuario
	 * @param soloSistema solo los cambios hechos por el sistema
	 * @param registro    llave primaria exacta
	 * @param desde       fecha inicial
	 * @param hasta       fecha final
	 * @param pagina      número de página, desde 0
	 * @param tamano      cambios por página (máximo 200)
	 * @return la página de cambios
	 */
	@Operation(summary = "Consultar la auditoría", description = "Filtros opcionales; los cambios sin usuario los hizo el sistema.")
	@ApiResponse(responseCode = "200", description = "Página de cambios")
	@ApiResponse(responseCode = "409", description = "Operación inexistente o fechas invertidas", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<PaginaAuditoriaDTO> buscar(
			@Parameter(description = "Tabla, por ejemplo lote") @RequestParam(required = false) String tabla,
			@Parameter(description = "INSERT, UPDATE, DELETE o TRUNCATE") @RequestParam(required = false) String operacion,
			@Parameter(description = "Parte del nombre del usuario") @RequestParam(required = false) String usuario,
			@Parameter(description = "Solo los cambios del sistema (sin usuario)") @RequestParam(defaultValue = "false") boolean soloSistema,
			@Parameter(description = "Llave primaria del registro, por ejemplo 52") @RequestParam(required = false) String registro,
			@Parameter(description = "Fecha inicial (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@Parameter(description = "Fecha final (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@Parameter(description = "Número de página, desde 0") @RequestParam(defaultValue = "0") int pagina,
			@Parameter(description = "Cambios por página") @RequestParam(defaultValue = "" + AuditoriaService.TAMANO_POR_DEFECTO) int tamano) {
		FiltroAuditoriaDTO filtro = new FiltroAuditoriaDTO(tabla, operacion, usuario, soloSistema, registro, desde, hasta);
		return ResponseEntity.ok(auditoriaService.buscar(filtro, pagina, tamano));
	}

	/**
	 * Lista las tablas que tienen cambios registrados.
	 *
	 * @return nombres de las tablas
	 */
	@Operation(summary = "Tablas con cambios auditados")
	@ApiResponse(responseCode = "200", description = "Nombres de las tablas")
	@PreAuthorize(VER)
	@GetMapping("/tablas")
	public ResponseEntity<List<String>> tablas() {
		return ResponseEntity.ok(auditoriaService.tablas());
	}
}
