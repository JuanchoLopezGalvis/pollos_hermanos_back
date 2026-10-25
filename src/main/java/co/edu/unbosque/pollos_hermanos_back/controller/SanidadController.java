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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.AplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroAplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.service.SanidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Sanidad de los lotes. Consultar requiere {@code sanidad.ver}; registrar
 * productos, compras o aplicaciones requiere {@code sanidad.registrar}.
 */
@RestController
@RequestMapping("/sanidad")
@Tag(name = "Sanidad", description = "Vitaminas y medicamentos: catálogo, compras y aplicaciones por lote")
public class SanidadController {

	/** Permiso para consultar. */
	private static final String VER = "@permisos.tieneAlguno('sanidad.ver', 'sanidad.registrar')";

	/** Permiso para registrar. */
	private static final String REGISTRAR = "@permisos.tiene('sanidad.registrar')";

	/** Servicio de sanidad. */
	private final SanidadService sanidadService;

	/**
	 * Crea el controlador.
	 *
	 * @param sanidadService servicio de sanidad
	 */
	public SanidadController(SanidadService sanidadService) {
		this.sanidadService = sanidadService;
	}

	/**
	 * Lista los productos sanitarios.
	 *
	 * @return productos
	 */
	@Operation(summary = "Productos sanitarios")
	@ApiResponse(responseCode = "200", description = "Productos con su costo promedio")
	@PreAuthorize(VER)
	@GetMapping("/productos")
	public ResponseEntity<List<ProductoSanitarioDTO>> obtenerProductos() {
		return ResponseEntity.ok(sanidadService.obtenerProductos());
	}

	/**
	 * Registra un producto sanitario.
	 *
	 * @param dto datos del producto
	 * @return el producto creado
	 */
	@Operation(summary = "Registrar producto", description = "El proveedor debe suministrar insumos sanitarios.")
	@ApiResponse(responseCode = "201", description = "Producto registrado")
	@ApiResponse(responseCode = "409", description = "Producto repetido o proveedor inválido", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/productos")
	public ResponseEntity<ProductoSanitarioDTO> crearProducto(@Valid @RequestBody RegistroProductoSanitarioDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sanidadService.crearProducto(dto));
	}

	/**
	 * Modifica un producto sanitario.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el producto actualizado
	 */
	@Operation(summary = "Modificar producto")
	@ApiResponse(responseCode = "200", description = "Producto actualizado")
	@ApiResponse(responseCode = "404", description = "El producto no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PutMapping("/productos/{id}")
	public ResponseEntity<ProductoSanitarioDTO> actualizarProducto(@Parameter(description = "Identificador") @PathVariable Long id,
			@Valid @RequestBody RegistroProductoSanitarioDTO dto) {
		return ResponseEntity.ok(sanidadService.actualizarProducto(id, dto));
	}

	/**
	 * Lista las compras sanitarias.
	 *
	 * @return compras
	 */
	@Operation(summary = "Compras sanitarias")
	@ApiResponse(responseCode = "200", description = "Compras, de la más reciente a la más antigua")
	@PreAuthorize(VER)
	@GetMapping("/compras")
	public ResponseEntity<List<CompraSanitariaDTO>> obtenerCompras() {
		return ResponseEntity.ok(sanidadService.obtenerCompras());
	}

	/**
	 * Registra una compra sanitaria.
	 *
	 * @param dto datos de la compra
	 * @return la compra registrada
	 */
	@Operation(summary = "Registrar compra sanitaria", description = "Invoca sp_registrar_compra_sanitaria.")
	@ApiResponse(responseCode = "201", description = "Compra registrada")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó la compra", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/compras")
	public ResponseEntity<CompraSanitariaDTO> registrarCompra(@Valid @RequestBody RegistroCompraSanitariaDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sanidadService.registrarCompra(dto));
	}

	/**
	 * Lista las aplicaciones sanitarias.
	 *
	 * @param lote lote para filtrar, opcional
	 * @return aplicaciones
	 */
	@Operation(summary = "Aplicaciones sanitarias")
	@ApiResponse(responseCode = "200", description = "Aplicaciones, de la más reciente a la más antigua")
	@PreAuthorize(VER)
	@GetMapping("/aplicaciones")
	public ResponseEntity<List<AplicacionSanitariaDTO>> obtenerAplicaciones(
			@Parameter(description = "Identificador del lote") @RequestParam(required = false) Long lote) {
		return ResponseEntity.ok(sanidadService.obtenerAplicaciones(lote));
	}

	/**
	 * Registra una aplicación sanitaria.
	 *
	 * @param dto datos de la aplicación
	 * @return la aplicación registrada
	 */
	@Operation(summary = "Registrar aplicación", description = "Invoca sp_registrar_aplicacion_sanitaria. Solo un encargado de sanidad activo puede ser responsable; el costo se carga al lote.")
	@ApiResponse(responseCode = "201", description = "Aplicación registrada")
	@ApiResponse(responseCode = "409", description = "Responsable sin el cargo, lote inactivo o producto sin compras", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR)
	@PostMapping("/aplicaciones")
	public ResponseEntity<AplicacionSanitariaDTO> registrarAplicacion(@Valid @RequestBody RegistroAplicacionSanitariaDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sanidadService.registrarAplicacion(dto));
	}
}
