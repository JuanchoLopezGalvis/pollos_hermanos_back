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

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Clientes. Consultar requiere un permiso comercial; registrar o modificar
 * requiere {@code cliente.gestionar}.
 */
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Tiendas, restaurantes y personas naturales")
public class ClienteController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('cliente.gestionar', 'pedido.gestionar', 'venta.gestionar', 'cartera.ver')";

	/** Permiso para registrar o modificar. */
	private static final String GESTIONAR = "@permisos.tiene('cliente.gestionar')";

	/** Servicio de clientes. */
	private final ClienteService clienteService;

	/**
	 * Crea el controlador.
	 *
	 * @param clienteService servicio de clientes
	 */
	public ClienteController(ClienteService clienteService) {
		this.clienteService = clienteService;
	}

	/**
	 * Lista los clientes.
	 *
	 * @return clientes
	 */
	@Operation(summary = "Listar clientes")
	@ApiResponse(responseCode = "200", description = "Clientes")
	@PreAuthorize(VER)
	@GetMapping
	public ResponseEntity<List<ClienteDTO>> obtenerTodos() {
		return ResponseEntity.ok(clienteService.obtenerTodos());
	}

	/**
	 * Registra un cliente.
	 *
	 * @param dto datos del cliente
	 * @return el cliente creado
	 */
	@Operation(summary = "Registrar cliente", description = "Debe tener teléfono o correo; documento, teléfono y correo son únicos.")
	@ApiResponse(responseCode = "201", description = "Cliente registrado")
	@ApiResponse(responseCode = "400", description = "Sin medio de contacto o datos inválidos", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@ApiResponse(responseCode = "409", description = "Documento, teléfono o correo repetido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PostMapping
	public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody RegistroClienteDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(dto));
	}

	/**
	 * Modifica un cliente.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el cliente actualizado
	 */
	@Operation(summary = "Modificar cliente")
	@ApiResponse(responseCode = "200", description = "Cliente actualizado")
	@ApiResponse(responseCode = "404", description = "El cliente no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(GESTIONAR)
	@PutMapping("/{id}")
	public ResponseEntity<ClienteDTO> actualizar(@Parameter(description = "Identificador") @PathVariable Long id,
			@Valid @RequestBody RegistroClienteDTO dto) {
		return ResponseEntity.ok(clienteService.actualizar(id, dto));
	}
}
