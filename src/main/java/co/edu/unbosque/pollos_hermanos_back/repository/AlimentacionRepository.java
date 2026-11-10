package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.CompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EtapaAlimentacionDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.InventarioAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TipoAlimentoDTO;

/**
 * Acceso a la alimentación: etapas, tipos de alimento, inventario, compras y
 * consumos. Las compras y consumos pasan por sus procedimientos; por eso estos
 * métodos no deben llamarse dentro de una transacción de Spring.
 */
@Repository
public class AlimentacionRepository {

	/** Mapeo de etapas. */
	private static final DataClassRowMapper<EtapaAlimentacionDTO> MAPEO_ETAPA = new DataClassRowMapper<>(EtapaAlimentacionDTO.class);

	/** Mapeo del inventario. */
	private static final DataClassRowMapper<InventarioAlimentoDTO> MAPEO_INVENTARIO = new DataClassRowMapper<>(InventarioAlimentoDTO.class);

	/** Mapeo de compras. */
	private static final DataClassRowMapper<CompraAlimentoDTO> MAPEO_COMPRA = new DataClassRowMapper<>(CompraAlimentoDTO.class);

	/** Mapeo de consumos. */
	private static final DataClassRowMapper<ConsumoAlimentoDTO> MAPEO_CONSUMO = new DataClassRowMapper<>(ConsumoAlimentoDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public AlimentacionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene las etapas de alimentación en orden.
	 *
	 * @return etapas
	 */
	public List<EtapaAlimentacionDTO> obtenerEtapas() {
		return jdbcTemplate.query("SELECT id_etapa, nombre, orden, dia_inicio, dia_fin FROM etapa_alimentacion ORDER BY orden",
				MAPEO_ETAPA);
	}

	/**
	 * Obtiene los tipos de alimento con su inventario.
	 *
	 * @return inventario por tipo de alimento, ordenado por etapa
	 */
	public List<InventarioAlimentoDTO> obtenerInventario() {
		return jdbcTemplate.query("SELECT * FROM v_inventario_alimento ORDER BY dia_inicio, nombre", MAPEO_INVENTARIO);
	}

	/**
	 * Registra un tipo de alimento.
	 *
	 * @param dto datos del tipo de alimento
	 * @return identificador asignado
	 */
	public Long crearTipo(TipoAlimentoDTO dto) {
		return jdbcTemplate.queryForObject("INSERT INTO tipo_alimento (nombre, id_etapa, peso_bulto_kg) VALUES (?, ?, COALESCE(?, 40))"
				+ " RETURNING id_tipo_alimento", Long.class, dto.nombre().trim(), dto.idEtapa(), dto.pesoBultoKg());
	}

	/**
	 * Obtiene las compras de alimento, de la más reciente a la más antigua.
	 *
	 * @return compras
	 */
	public List<CompraAlimentoDTO> obtenerCompras() {
		return jdbcTemplate.query("SELECT * FROM v_compra_alimento ORDER BY fecha DESC, id_compra_alimento DESC", MAPEO_COMPRA);
	}

	/**
	 * Registra una compra con {@code sp_registrar_compra_alimento}.
	 *
	 * @param dto datos de la compra
	 * @return identificador de la compra
	 */
	public Long registrarCompra(RegistroCompraAlimentoDTO dto) {
		return jdbcTemplate.queryForObject("CALL sp_registrar_compra_alimento(?, ?, ?, ?, ?, NULL)", Long.class,
				dto.idProveedor(), dto.idTipoAlimento(), dto.fecha(), dto.cantidadBultos(), dto.costoUnitario());
	}

	/**
	 * Obtiene los consumos de alimento, de todos los lotes o de uno.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return consumos, del más reciente al más antiguo
	 */
	public List<ConsumoAlimentoDTO> obtenerConsumos(Long idLote) {
		String sql = "SELECT * FROM v_consumo_alimento" + (idLote == null ? "" : " WHERE id_lote = ?")
				+ " ORDER BY fecha DESC, id_consumo_alimento DESC";
		return idLote == null ? jdbcTemplate.query(sql, MAPEO_CONSUMO) : jdbcTemplate.query(sql, MAPEO_CONSUMO, idLote);
	}

	/**
	 * Registra un consumo con {@code sp_registrar_consumo_alimento}.
	 *
	 * @param dto datos del consumo
	 * @return identificador del consumo
	 */
	public Long registrarConsumo(RegistroConsumoAlimentoDTO dto) {
		return ((Number) jdbcTemplate.queryForMap("CALL sp_registrar_consumo_alimento(?, ?, ?, ?, NULL, NULL)", dto.idLote(),
				dto.idTipoAlimento(), dto.fecha(), dto.cantidadBultos()).get("p_id_consumo")).longValue();
	}

	/**
	 * Busca un consumo registrado.
	 *
	 * @param id identificador
	 * @return el consumo
	 */
	public ConsumoAlimentoDTO buscarConsumo(Long id) {
		return jdbcTemplate.queryForObject("SELECT * FROM v_consumo_alimento WHERE id_consumo_alimento = ?", MAPEO_CONSUMO, id);
	}

	/**
	 * Busca una compra registrada.
	 *
	 * @param id identificador
	 * @return la compra
	 */
	public CompraAlimentoDTO buscarCompra(Long id) {
		return jdbcTemplate.queryForObject("SELECT * FROM v_compra_alimento WHERE id_compra_alimento = ?", MAPEO_COMPRA, id);
	}

	/**
	 * Busca el inventario de un tipo de alimento.
	 *
	 * @param idTipoAlimento identificador del tipo
	 * @return su inventario
	 */
	public InventarioAlimentoDTO buscarInventario(Long idTipoAlimento) {
		return jdbcTemplate.queryForObject("SELECT * FROM v_inventario_alimento WHERE id_tipo_alimento = ?", MAPEO_INVENTARIO,
				idTipoAlimento);
	}
}
