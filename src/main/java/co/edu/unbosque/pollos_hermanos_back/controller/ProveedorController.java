package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ProveedorDTO;
import co.edu.unbosque.pollos_hermanos_back.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Proveedores. Consultar requiere un permiso de alimentación, sanidad o
 * proveedores; registrar o modificar requiere {@code proveedor.gestionar}.
 */
@RestController
@RequestMapping("/proveedores")
@Tag(name = "Proveedores", description = "Proveedores de alimento e insumos sanitarios")
public class ProveedorController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('proveedor.gestionar', 'alimentacion.ver', 'alimentacion.registrar',"
			+ " 'sanidad.ver', 'sanidad.registrar')";

	/** Permiso para registrar o modificar. */
	private static final String GESTIONAR = "@permisos.tiene('proveedor.gestionar')";

	/** Servicio de proveedores. */
	private final ProveedorService proveedorService;

	/**
	 * Crea el controlador.
	 *
	 * @param proveedorService servicio de proveedores
	 */
	public ProveedorController(ProveedorService proveedorService) {
		this.proveedorService = proveedorService;
	}

	/**
	 * Lista los proveedores.
	 *
	 * @return proveedores
	 */
	@Operation(summary = "Listar proveedores")
	@ApiResponse(responseCode = "200", description = "Proveedores")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<ProveedorDTO>> obtenerTodos() {
		return ResponseEntity.ok(proveedorService.obtenerTodos());
	}

	/**
	 * Registra un proveedor.
	 *
	 * @param dto datos del proveedor
	 * @return el proveedor creado
	 */
	@Operation(summary = "Registrar proveedor", description = "Debe suministrar alimento, insumos sanitarios o ambos.")
	@ApiResponse(responseCode = "201", description = "Proveedor registrado")
	@ApiResponse(responseCode = "400", description = "No suministra nada o datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "NIT repetido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<ProveedorDTO> crear(@Valid @RequestBody ProveedorDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(dto));
	}

	/**
	 * Modifica un proveedor.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el proveedor actualizado
	 */
	@Operation(summary = "Modificar proveedor")
	@ApiResponse(responseCode = "200", description = "Proveedor actualizado")
	@ApiResponse(responseCode = "404", description = "El proveedor no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<ProveedorDTO> actualizar(@Parameter(description = "Identificador") @PathVariable Long id,
			@Valid @RequestBody ProveedorDTO dto) {
		return ResponseEntity.ok(proveedorService.actualizar(id, dto));
	}
}
