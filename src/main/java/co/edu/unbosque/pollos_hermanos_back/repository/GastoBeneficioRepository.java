package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.DetalleBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.JornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LineaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.OtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroGastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroJornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroOtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RentabilidadLoteDTO;

/**
 * Acceso a gastos, jornadas de beneficio, otros ingresos y balance de los lotes. Las escrituras
 * pasan por procedimientos que hacen {@code COMMIT}.
 */
@Repository
public class GastoBeneficioRepository {

	/** Mapeo de gastos. */
	private static final DataClassRowMapper<GastoDTO> MAPEO_GASTO = new DataClassRowMapper<>(GastoDTO.class);

	/** Mapeo de jornadas. */
	private static final DataClassRowMapper<JornadaBeneficioDTO> MAPEO_JORNADA = new DataClassRowMapper<>(JornadaBeneficioDTO.class);

	/** Mapeo del detalle de beneficio. */
	private static final DataClassRowMapper<DetalleBeneficioDTO> MAPEO_DETALLE = new DataClassRowMapper<>(DetalleBeneficioDTO.class);

	/** Mapeo de la rentabilidad. */
	private static final DataClassRowMapper<RentabilidadLoteDTO> MAPEO_RENTABILIDAD = new DataClassRowMapper<>(RentabilidadLoteDTO.class);

	/** Mapeo de otros ingresos. */
	private static final DataClassRowMapper<OtroIngresoDTO> MAPEO_INGRESO = new DataClassRowMapper<>(OtroIngresoDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public GastoBeneficioRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los gastos.
	 *
	 * @param idLote lote, o {@code null} para todos (incluidos los generales)
	 * @return gastos, del más reciente al más antiguo
	 */
	public List<GastoDTO> obtenerGastos(Long idLote) {
		String sql = "SELECT * FROM v_gasto" + (idLote == null ? "" : " WHERE id_lote = ?") + " ORDER BY fecha DESC, id_gasto DESC";
		return idLote == null ? jdbcTemplate.query(sql, MAPEO_GASTO) : jdbcTemplate.query(sql, MAPEO_GASTO, idLote);
	}

	/**
	 * Registra un gasto con {@code sp_registrar_gasto}.
	 *
	 * @param dto datos del gasto
	 * @return el gasto registrado
	 */
	public GastoDTO registrarGasto(RegistroGastoDTO dto) {
		Long id = jdbcTemplate.queryForObject("CALL sp_registrar_gasto(?, ?, ?, ?, ?, NULL)", Long.class, dto.idCategoriaGasto(),
				dto.idLote(), dto.fecha(), dto.monto(), dto.descripcion());
		return jdbcTemplate.queryForObject("SELECT * FROM v_gasto WHERE id_gasto = ?", MAPEO_GASTO, id);
	}

	/**
	 * Obtiene las jornadas de beneficio.
	 *
	 * @param idLote lote, o {@code null} para todas
	 * @return jornadas, de la más reciente a la más antigua
	 */
	public List<JornadaBeneficioDTO> obtenerJornadas(Long idLote) {
		String sql = "SELECT * FROM v_jornada_beneficio" + (idLote == null ? "" : " WHERE id_lote = ?")
				+ " ORDER BY fecha DESC, id_jornada DESC";
		return idLote == null ? jdbcTemplate.query(sql, MAPEO_JORNADA) : jdbcTemplate.query(sql, MAPEO_JORNADA, idLote);
	}

	/**
	 * Busca una jornada.
	 *
	 * @param id identificador
	 * @return la jornada, si existe
	 */
	public Optional<JornadaBeneficioDTO> buscarJornada(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_jornada_beneficio WHERE id_jornada = ?", MAPEO_JORNADA, id).stream().findFirst();
	}

	/**
	 * Obtiene quién participó en una jornada.
	 *
	 * @param idJornada jornada
	 * @return detalle de la jornada
	 */
	public List<DetalleBeneficioDTO> obtenerDetalle(Long idJornada) {
		return jdbcTemplate.query("SELECT * FROM v_detalle_beneficio WHERE id_jornada = ? ORDER BY beneficiadora", MAPEO_DETALLE,
				idJornada);
	}

	/**
	 * Registra una jornada y su detalle con {@code sp_registrar_jornada_beneficio}.
	 *
	 * @param dto datos de la jornada
	 * @return identificador de la jornada
	 */
	public Long registrarJornada(RegistroJornadaBeneficioDTO dto) {
		return ((Number) jdbcTemplate.queryForMap("CALL sp_registrar_jornada_beneficio(?, ?, ?, ?::jsonb, NULL, NULL)",
				dto.idLote(), dto.fecha(), dto.observaciones(), detalleJson(dto.detalle())).get("p_id_jornada")).longValue();
	}

	/**
	 * Obtiene los otros ingresos de los lotes.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return ingresos, del más reciente al más antiguo
	 */
	public List<OtroIngresoDTO> obtenerOtrosIngresos(Long idLote) {
		String sql = "SELECT * FROM v_otro_ingreso" + (idLote == null ? "" : " WHERE id_lote = ?")
				+ " ORDER BY fecha DESC, id_otro_ingreso DESC";
		return idLote == null ? jdbcTemplate.query(sql, MAPEO_INGRESO) : jdbcTemplate.query(sql, MAPEO_INGRESO, idLote);
	}

	/**
	 * Registra otro ingreso con {@code sp_registrar_otro_ingreso}.
	 *
	 * @param dto datos del ingreso
	 * @return el ingreso registrado
	 */
	public OtroIngresoDTO registrarOtroIngreso(RegistroOtroIngresoDTO dto) {
		Long id = jdbcTemplate.queryForObject("CALL sp_registrar_otro_ingreso(?, ?, ?, ?, NULL)", Long.class, dto.idLote(),
				dto.fecha(), dto.monto(), dto.descripcion());
		return jdbcTemplate.queryForObject("SELECT * FROM v_otro_ingreso WHERE id_otro_ingreso = ?", MAPEO_INGRESO, id);
	}

	/**
	 * Obtiene el costo y la rentabilidad de los lotes.
	 *
	 * @return rentabilidad por lote, del más reciente al más antiguo
	 */
	public List<RentabilidadLoteDTO> obtenerRentabilidad() {
		return jdbcTemplate.query("SELECT r.* FROM v_rentabilidad_lote r JOIN lote l ON l.id_lote = r.id_lote"
				+ " ORDER BY l.fecha_ingreso DESC, r.codigo", MAPEO_RENTABILIDAD);
	}

	/**
	 * Obtiene el costo y la rentabilidad de un lote.
	 *
	 * @param idLote lote
	 * @return su rentabilidad, si existe
	 */
	public Optional<RentabilidadLoteDTO> buscarRentabilidad(Long idLote) {
		return jdbcTemplate.query("SELECT * FROM v_rentabilidad_lote WHERE id_lote = ?", MAPEO_RENTABILIDAD, idLote).stream()
				.findFirst();
	}

	/**
	 * Convierte el detalle en el arreglo JSON que espera el procedimiento. Todos
	 * los valores son numéricos, así que no requieren escape.
	 *
	 * @param detalle beneficiadoras de la jornada
	 * @return arreglo JSON
	 */
	static String detalleJson(List<LineaBeneficioDTO> detalle) {
		return detalle.stream()
				.map(l -> String.format(Locale.ROOT, "{\"idEmpleado\":%d,\"avesBeneficiadas\":%d,\"tarifaPorAve\":%s}",
						l.idEmpleado(), l.avesBeneficiadas(), l.tarifaPorAve().toPlainString()))
				.collect(Collectors.joining(",", "[", "]"));
	}
}
