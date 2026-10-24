package co.edu.unbosque.pollos_hermanos_back.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.pollos_hermanos_back.dto.DetalleBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ErrorApiDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.JornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.OtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroGastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroJornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroOtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RentabilidadLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.service.GastoBeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Gastos, beneficio, otros ingresos y balance de los lotes. Registrar gastos
 * requiere {@code gasto.registrar}; registrar jornadas de beneficio requiere
 * {@code beneficio.registrar}; registrar otros ingresos requiere
 * {@code venta.gestionar}; consultar el balance requiere {@code costo.ver}.
 */
@RestController
@Tag(name = "Gastos, ingresos y balance", description = "Gastos por lote o generales, jornadas de beneficio, otros ingresos y ganancia por lote")
public class GastoBeneficioController {

	/** Permiso para consultar gastos. */
	private static final String VER_GASTOS = "@permisos.tieneAlguno('gasto.registrar', 'costo.ver')";

	/** Permiso para registrar gastos. */
	private static final String REGISTRAR_GASTO = "@permisos.tiene('gasto.registrar')";

	/** Permiso para consultar el beneficio. */
	private static final String VER_BENEFICIO = "@permisos.tieneAlguno('beneficio.registrar', 'costo.ver')";

	/** Permiso para registrar el beneficio. */
	private static final String REGISTRAR_BENEFICIO = "@permisos.tiene('beneficio.registrar')";

	/** Permiso para consultar otros ingresos. */
	private static final String VER_INGRESOS = "@permisos.tieneAlguno('venta.gestionar', 'costo.ver')";

	/** Permiso para registrar otros ingresos. */
	private static final String REGISTRAR_INGRESO = "@permisos.tiene('venta.gestionar')";

	/** Permiso para consultar costos. */
	private static final String VER_COSTOS = "@permisos.tiene('costo.ver')";

	/** Servicio de gastos y beneficio. */
	private final GastoBeneficioService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de gastos y beneficio
	 */
	public GastoBeneficioController(GastoBeneficioService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista los gastos.
	 *
	 * @param lote lote para filtrar, opcional
	 * @return gastos
	 */
	@Operation(summary = "Listar gastos", description = "Sin filtro incluye los gastos generales de la finca.")
	@ApiResponse(responseCode = "200", description = "Gastos")
	@PreAuthorize(VER_GASTOS)
	@GetMapping("/gastos")
	public ResponseEntity<List<GastoDTO>> obtenerGastos(
			@Parameter(description = "Identificador del lote") @RequestParam(required = false) Long lote) {
		return ResponseEntity.ok(servicio.obtenerGastos(lote));
	}

	/**
	 * Registra un gasto.
	 *
	 * @param dto datos del gasto
	 * @return el gasto registrado
	 */
	@Operation(summary = "Registrar gasto", description = "Invoca sp_registrar_gasto. Sin lote es un gasto general de la finca; los pagos a las beneficiadoras se registran en el beneficio.")
	@ApiResponse(responseCode = "201", description = "Gasto registrado")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el gasto", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR_GASTO)
	@PostMapping("/gastos")
	public ResponseEntity<GastoDTO> registrarGasto(@Valid @RequestBody RegistroGastoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarGasto(dto));
	}

	/**
	 * Lista las jornadas de beneficio.
	 *
	 * @param lote lote para filtrar, opcional
	 * @return jornadas
	 */
	@Operation(summary = "Jornadas de beneficio")
	@ApiResponse(responseCode = "200", description = "Jornadas con aves beneficiadas y costo")
	@PreAuthorize(VER_BENEFICIO)
	@GetMapping("/beneficio/jornadas")
	public ResponseEntity<List<JornadaBeneficioDTO>> obtenerJornadas(
			@Parameter(description = "Identificador del lote") @RequestParam(required = false) Long lote) {
		return ResponseEntity.ok(servicio.obtenerJornadas(lote));
	}

	/**
	 * Muestra quién participó en una jornada.
	 *
	 * @param id jornada
	 * @return detalle de la jornada
	 */
	@Operation(summary = "Detalle de una jornada", description = "Beneficiadoras, pollos beneficiados y tarifa por pollo.")
	@ApiResponse(responseCode = "200", description = "Detalle")
	@ApiResponse(responseCode = "404", description = "La jornada no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER_BENEFICIO)
	@GetMapping("/beneficio/jornadas/{id}/detalle")
	public ResponseEntity<List<DetalleBeneficioDTO>> obtenerDetalle(@Parameter(description = "Jornada") @PathVariable Long id) {
		return ResponseEntity.ok(servicio.obtenerDetalle(id));
	}

	/**
	 * Registra una jornada de beneficio.
	 *
	 * @param dto jornada y beneficiadoras
	 * @return la jornada registrada
	 */
	@Operation(summary = "Registrar jornada de beneficio", description = "Invoca sp_registrar_jornada_beneficio: guarda la jornada y su detalle en una sola transacción. Solo beneficiadoras activas; el total no supera las aves vivas del lote.")
	@ApiResponse(responseCode = "201", description = "Jornada registrada")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó la jornada", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR_BENEFICIO)
	@PostMapping("/beneficio/jornadas")
	public ResponseEntity<JornadaBeneficioDTO> registrarJornada(@Valid @RequestBody RegistroJornadaBeneficioDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarJornada(dto));
	}

	/**
	 * Lista los otros ingresos de los lotes.
	 *
	 * @param lote lote para filtrar, opcional
	 * @return ingresos
	 */
	@Operation(summary = "Otros ingresos de los lotes", description = "Ingresos distintos de la venta de aves, por ejemplo la gallinaza.")
	@ApiResponse(responseCode = "200", description = "Otros ingresos")
	@PreAuthorize(VER_INGRESOS)
	@GetMapping("/ingresos")
	public ResponseEntity<List<OtroIngresoDTO>> obtenerOtrosIngresos(
			@Parameter(description = "Identificador del lote") @RequestParam(required = false) Long lote) {
		return ResponseEntity.ok(servicio.obtenerOtrosIngresos(lote));
	}

	/**
	 * Registra otro ingreso de un lote.
	 *
	 * @param dto datos del ingreso
	 * @return el ingreso registrado
	 */
	@Operation(summary = "Registrar otro ingreso", description = "Invoca sp_registrar_otro_ingreso. Suma en la ganancia del lote.")
	@ApiResponse(responseCode = "201", description = "Ingreso registrado")
	@ApiResponse(responseCode = "409", description = "La base de datos rechazó el ingreso", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(REGISTRAR_INGRESO)
	@PostMapping("/ingresos")
	public ResponseEntity<OtroIngresoDTO> registrarOtroIngreso(@Valid @RequestBody RegistroOtroIngresoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarOtroIngreso(dto));
	}

	/**
	 * Muestra el costo y la rentabilidad de los lotes.
	 *
	 * @return rentabilidad por lote
	 */
	@Operation(summary = "Costo y rentabilidad de los lotes", description = "Lee v_rentabilidad_lote: costos (pollitos, alimento, sanidad, beneficio, otros gastos), ingresos (ventas en libras y otros ingresos) y ganancia neta.")
	@ApiResponse(responseCode = "200", description = "Rentabilidad por lote")
	@PreAuthorize(VER_COSTOS)
	@GetMapping("/costos/lotes")
	public ResponseEntity<List<RentabilidadLoteDTO>> obtenerRentabilidad() {
		return ResponseEntity.ok(servicio.obtenerRentabilidad());
	}

	/**
	 * Muestra el costo y la rentabilidad de un lote.
	 *
	 * @param id lote
	 * @return su rentabilidad
	 */
	@Operation(summary = "Costo y rentabilidad de un lote")
	@ApiResponse(responseCode = "200", description = "Rentabilidad del lote")
	@ApiResponse(responseCode = "404", description = "El lote no existe", content = @Content(schema = @Schema(implementation = ErrorApiDTO.class)))
	@PreAuthorize(VER_COSTOS)
	@GetMapping("/costos/lotes/{id}")
	public ResponseEntity<RentabilidadLoteDTO> obtenerRentabilidadLote(@Parameter(description = "Lote") @PathVariable Long id) {
		return ResponseEntity.ok(servicio.obtenerRentabilidad(id));
	}
}
