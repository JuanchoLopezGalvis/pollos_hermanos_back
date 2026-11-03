package co.edu.unbosque.pollos_hermanos_back.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.GalponOcupacionDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LoteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.MortalidadRegistradaDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;

/**
 * Acceso a lotes y mortalidad en PostgreSQL con {@link JdbcTemplate}.
 * <p>
 * Las escrituras pasan por los procedimientos almacenados, que validan las
 * reglas del negocio y hacen {@code COMMIT} o {@code ROLLBACK}. Por eso estos
 * métodos no deben llamarse dentro de una transacción de Spring: PostgreSQL no
 * permite controlar la transacción desde un procedimiento invocado dentro de
 * otra. Las lecturas usan la vista {@code v_lote_resumen}.
 */
@Repository
public class LoteRepository {

	/** Consulta base de la vista de lotes. */
	private static final String SELECT_LOTE = "SELECT id_lote, codigo, id_galpon, galpon, fecha_ingreso, cantidad_inicial,"
			+ " costo_pollito, costo_pollitos, fecha_estimada_venta, estado::text AS estado, edad_dias,"
			+ " mortalidad_acumulada, porcentaje_mortalidad, aves_disponibles, alerta_mortalidad, aves_vendidas,"
			+ " consumo_alimento_bultos, consumo_alimento_kg FROM v_lote_resumen";

	/** Convierte una fila de la vista en DTO. */
	private static final RowMapper<LoteDTO> MAPEO_LOTE = LoteRepository::mapearLote;

	/** Convierte una fila de mortalidad en DTO. */
	private static final RowMapper<MortalidadDTO> MAPEO_MORTALIDAD = (rs, fila) -> new MortalidadDTO(
			rs.getLong("id_mortalidad"), rs.getObject("fecha", LocalDate.class), rs.getInt("cantidad"),
			rs.getString("causa"));

	/** Convierte una fila de ocupación en DTO. */
	private static final DataClassRowMapper<GalponOcupacionDTO> MAPEO_OCUPACION = new DataClassRowMapper<>(
			GalponOcupacionDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public LoteRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Registra un lote con {@code sp_registrar_lote}.
	 *
	 * @param codigo             código del lote
	 * @param idGalpon           galpón
	 * @param fechaIngreso       fecha de ingreso
	 * @param cantidadInicial    aves que ingresan
	 * @param costoPollito       costo unitario del pollito
	 * @param fechaEstimadaVenta fecha estimada de venta, o {@code null}
	 * @return identificador del lote creado
	 */
	public Long registrar(String codigo, Long idGalpon, LocalDate fechaIngreso, Integer cantidadInicial,
			BigDecimal costoPollito, LocalDate fechaEstimadaVenta) {
		return jdbcTemplate.queryForObject("CALL sp_registrar_lote(?, ?, ?, ?, ?, ?, NULL)", Long.class, codigo,
				idGalpon, fechaIngreso, cantidadInicial, costoPollito, fechaEstimadaVenta);
	}

	/**
	 * Corrige los datos editables de un lote. El trigger {@code trg_lote_validar}
	 * valida la capacidad del galpón, la mortalidad registrada y el ciclo.
	 *
	 * @param id                 identificador del lote
	 * @param cantidadInicial    aves con las que ingresó
	 * @param costoPollito       costo unitario del pollito
	 * @param fechaEstimadaVenta fecha estimada de venta
	 * @return {@code true} si el lote existía
	 */
	public boolean actualizar(Long id, Integer cantidadInicial, BigDecimal costoPollito, LocalDate fechaEstimadaVenta) {
		return jdbcTemplate.update(
				"UPDATE lote SET cantidad_inicial = ?, costo_pollito = ?, fecha_estimada_venta = ? WHERE id_lote = ?",
				cantidadInicial, costoPollito, fechaEstimadaVenta, id) > 0;
	}

	/**
	 * Cambia el estado de un lote con {@code sp_cambiar_estado_lote}.
	 *
	 * @param id     identificador del lote
	 * @param estado nuevo estado
	 */
	public void cambiarEstado(Long id, EstadoLote estado) {
		jdbcTemplate.update("CALL sp_cambiar_estado_lote(?, ?)", id, estado.name());
	}

	/**
	 * Obtiene todos los lotes, del más reciente al más antiguo.
	 *
	 * @return lotes
	 */
	public List<LoteDTO> obtenerTodos() {
		return jdbcTemplate.query(SELECT_LOTE + " ORDER BY fecha_ingreso DESC, codigo", MAPEO_LOTE);
	}

	/**
	 * Busca un lote por su identificador.
	 *
	 * @param id identificador del lote
	 * @return el lote, si existe
	 */
	public Optional<LoteDTO> buscarPorId(Long id) {
		return jdbcTemplate.query(SELECT_LOTE + " WHERE id_lote = ?", MAPEO_LOTE, id).stream().findFirst();
	}

	/**
	 * Busca un lote por su código.
	 *
	 * @param codigo código del lote
	 * @return el lote, si existe
	 */
	public Optional<LoteDTO> buscarPorCodigo(String codigo) {
		return jdbcTemplate.query(SELECT_LOTE + " WHERE codigo = ?", MAPEO_LOTE, codigo).stream().findFirst();
	}

	/**
	 * Registra la mortalidad de un día con {@code sp_registrar_mortalidad}.
	 *
	 * @param idLote   identificador del lote
	 * @param fecha    día de la mortalidad, o {@code null} para hoy
	 * @param cantidad aves muertas
	 * @param causa    causa, opcional
	 * @return registro creado y estado de la alerta del 5%
	 */
	public MortalidadRegistradaDTO registrarMortalidad(Long idLote, LocalDate fecha, Integer cantidad, String causa) {
		Map<String, Object> salida = jdbcTemplate.queryForMap("CALL sp_registrar_mortalidad(?, ?, ?, ?, NULL, NULL, NULL)",
				idLote, fecha, cantidad, causa);
		return new MortalidadRegistradaDTO(((Number) salida.get("p_id_mortalidad")).longValue(),
				(BigDecimal) salida.get("p_porcentaje"), Boolean.TRUE.equals(salida.get("p_alerta")));
	}

	/**
	 * Corrige la cantidad o la causa de un registro de mortalidad. El trigger
	 * {@code trg_mortalidad_validar} vuelve a comprobar las aves disponibles.
	 *
	 * @param idLote       identificador del lote
	 * @param idMortalidad identificador del registro
	 * @param cantidad     aves muertas
	 * @param causa        causa, opcional
	 * @return {@code true} si el registro existía en ese lote
	 */
	public boolean actualizarMortalidad(Long idLote, Long idMortalidad, Integer cantidad, String causa) {
		return jdbcTemplate.update("UPDATE mortalidad SET cantidad = ?, causa = ? WHERE id_mortalidad = ? AND id_lote = ?",
				cantidad, causa, idMortalidad, idLote) > 0;
	}

	/**
	 * Obtiene la mortalidad registrada de un lote, del día más reciente al más
	 * antiguo.
	 *
	 * @param idLote identificador del lote
	 * @return registros de mortalidad
	 */
	public List<MortalidadDTO> obtenerMortalidad(Long idLote) {
		return jdbcTemplate.query(
				"SELECT id_mortalidad, fecha, cantidad, causa FROM mortalidad WHERE id_lote = ? ORDER BY fecha DESC",
				MAPEO_MORTALIDAD, idLote);
	}

	/**
	 * Obtiene la ocupación de los galpones: lotes activos, aves alojadas,
	 * espacio libre y próxima salida.
	 *
	 * @return ocupación por galpón, ordenada por nombre
	 */
	public List<GalponOcupacionDTO> obtenerOcupacionGalpones() {
		return jdbcTemplate.query("SELECT * FROM v_galpon_ocupacion ORDER BY nombre", MAPEO_OCUPACION);
	}

	/**
	 * Convierte una fila de {@code v_lote_resumen} en DTO.
	 *
	 * @param rs   fila actual
	 * @param fila número de fila
	 * @return DTO del lote
	 * @throws SQLException si falla la lectura
	 */
	private static LoteDTO mapearLote(ResultSet rs, int fila) throws SQLException {
		return new LoteDTO(rs.getLong("id_lote"), rs.getString("codigo"), rs.getLong("id_galpon"), rs.getString("galpon"),
				rs.getObject("fecha_ingreso", LocalDate.class), rs.getInt("cantidad_inicial"),
				rs.getBigDecimal("costo_pollito"), rs.getBigDecimal("costo_pollitos"),
				rs.getObject("fecha_estimada_venta", LocalDate.class), EstadoLote.valueOf(rs.getString("estado")),
				rs.getInt("edad_dias"), rs.getInt("mortalidad_acumulada"), rs.getBigDecimal("porcentaje_mortalidad"),
				rs.getInt("aves_disponibles"), rs.getBoolean("alerta_mortalidad"), rs.getInt("aves_vendidas"),
				rs.getBigDecimal("consumo_alimento_bultos"), rs.getBigDecimal("consumo_alimento_kg"));
	}
}
