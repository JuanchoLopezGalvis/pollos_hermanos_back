package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.CatalogoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catálogos del sistema. Cualquier usuario autenticado puede consultarlos,
 * porque los formularios de varios módulos los usan.
 */
@RestController
@RequestMapping("/catalogos")
@Tag(name = "Catálogos", description = "Cargos, tipos de cliente, métodos de pago y categorías de gasto")
public class CatalogoController {

	/** Servicio de catálogos. */
	private final CatalogoService catalogoService;

	/**
	 * Crea el controlador.
	 *
	 * @param catalogoService servicio de catálogos
	 */
	public CatalogoController(CatalogoService catalogoService) {
		this.catalogoService = catalogoService;
	}

	/**
	 * Obtiene un catálogo.
	 *
	 * @param nombre {@code cargos}, {@code tipos-cliente}, {@code metodos-pago} o
	 *               {@code categorias-gasto}
	 * @return elementos activos del catálogo
	 */
	@Operation(summary = "Obtener catálogo")
	@ApiResponse(responseCode = "200", description = "Elementos activos del catálogo")
	@ApiResponse(responseCode = "404", description = "El catálogo no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@GetMapping("/{nombre}")
	public ResponseEntity<List<CatalogoDTO>> obtener(
			@Parameter(description = "cargos, tipos-cliente, metodos-pago o categorias-gasto") @PathVariable String nombre) {
		return ResponseEntity.ok(catalogoService.obtener(nombre));
	}
}
