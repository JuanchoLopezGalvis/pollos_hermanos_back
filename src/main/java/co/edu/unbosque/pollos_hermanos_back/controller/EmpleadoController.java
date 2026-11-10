package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.EmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RetiroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.service.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Empleados de la finca. Consultar requiere {@code personal.ver} o cualquier
 * permiso de un módulo que asigne responsables (sanidad, beneficio, ventas);
 * registrar, modificar o retirar requiere {@code personal.gestionar}.
 */
@RestController
@RequestMapping("/empleados")
@Tag(name = "Personal", description = "Empleados y cargos; los retirados conservan su historial")
public class EmpleadoController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('personal.ver', 'personal.gestionar', 'sanidad.registrar',"
			+ " 'beneficio.registrar', 'venta.gestionar')";

	/** Permiso para registrar, modificar o retirar. */
	private static final String GESTIONAR = "@permisos.tiene('personal.gestionar')";

	/** Servicio de empleados. */
	private final EmpleadoService empleadoService;

	/**
	 * Crea el controlador.
	 *
	 * @param empleadoService servicio de empleados
	 */
	public EmpleadoController(EmpleadoService empleadoService) {
		this.empleadoService = empleadoService;
	}

	/**
	 * Lista los empleados.
	 *
	 * @param activos {@code true} para excluir a los retirados
	 * @param cargo   código del cargo, por ejemplo {@code BENEFICIADORA}
	 * @return empleados
	 */
	@Operation(summary = "Listar empleados")
	@ApiResponse(responseCode = "200", description = "Empleados")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<EmpleadoDTO>> obtener(
			@Parameter(description = "Solo activos") @RequestParam(defaultValue = "false") boolean activos,
			@Parameter(description = "Código del cargo") @RequestParam(required = false) String cargo) {
		return ResponseEntity.ok(empleadoService.obtener(activos, cargo));
	}

	/**
	 * Registra un empleado.
	 *
	 * @param dto datos del empleado
	 * @return el empleado creado
	 */
	@Operation(summary = "Registrar empleado")
	@ApiResponse(responseCode = "201", description = "Empleado registrado")
	@ApiResponse(responseCode = "409", description = "Documento repetido o cargo inválido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<EmpleadoDTO> crear(@Valid @RequestBody RegistroEmpleadoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crear(dto));
	}

	/**
	 * Modifica un empleado.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el empleado actualizado
	 */
	@Operation(summary = "Modificar empleado", description = "A un empleado retirado no se le cambia el cargo.")
	@ApiResponse(responseCode = "200", description = "Empleado actualizado")
	@ApiResponse(responseCode = "404", description = "El empleado no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<EmpleadoDTO> actualizar(@Parameter(description = "Identificador") @PathVariable Long id,
			@Valid @RequestBody RegistroEmpleadoDTO dto) {
		return ResponseEntity.ok(empleadoService.actualizar(id, dto));
	}

	/**
	 * Retira un empleado.
	 *
	 * @param id  identificador
	 * @param dto fecha de retiro, opcional
	 * @return el empleado retirado
	 */
	@Operation(summary = "Retirar empleado", description = "Invoca sp_retirar_empleado. El empleado conserva su historial y ya no se asigna a nuevos registros.")
	@ApiResponse(responseCode = "200", description = "Empleado retirado")
	@ApiResponse(responseCode = "409", description = "Ya estaba retirado", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PatchMapping("/{id}/retiro")
	public ResponseEntity<EmpleadoDTO> retirar(@Parameter(description = "Identificador") @PathVariable Long id,
			@Valid @RequestBody(required = false) RetiroEmpleadoDTO dto) {
		return ResponseEntity.ok(empleadoService.retirar(id, dto));
	}
}
