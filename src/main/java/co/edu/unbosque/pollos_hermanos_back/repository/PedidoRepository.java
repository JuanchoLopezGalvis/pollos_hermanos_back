package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.DetallePedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LineaPedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PedidoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPedidoDTO;

/**
 * Acceso a los pedidos. Crear, confirmar y cancelar pasan por procedimientos que
 * hacen {@code COMMIT}; por eso no se llaman dentro de una transacción de Spring.
 */
@Repository
public class PedidoRepository {

	/** Mapeo de pedidos. */
	private static final DataClassRowMapper<PedidoDTO> MAPEO = new DataClassRowMapper<>(PedidoDTO.class);

	/** Mapeo del detalle. */
	private static final DataClassRowMapper<DetallePedidoDTO> MAPEO_DETALLE = new DataClassRowMapper<>(DetallePedidoDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public PedidoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los pedidos, del más reciente al más antiguo.
	 *
	 * @param estado estado para filtrar, o {@code null} para todos
	 * @return pedidos
	 */
	public List<PedidoDTO> obtener(String estado) {
		return estado == null || estado.isBlank()
				? jdbcTemplate.query("SELECT * FROM v_pedido ORDER BY fecha_pedido DESC, id_pedido DESC", MAPEO)
				: jdbcTemplate.query("SELECT * FROM v_pedido WHERE estado = ? ORDER BY fecha_pedido DESC, id_pedido DESC", MAPEO,
						estado);
	}

	/**
	 * Busca un pedido.
	 *
	 * @param id identificador
	 * @return el pedido, si existe
	 */
	public Optional<PedidoDTO> buscar(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_pedido WHERE id_pedido = ?", MAPEO, id).stream().findFirst();
	}

	/**
	 * Obtiene las aves por lote de un pedido.
	 *
	 * @param idPedido pedido
	 * @return detalle
	 */
	public List<DetallePedidoDTO> obtenerDetalle(Long idPedido) {
		return jdbcTemplate.query("SELECT * FROM v_detalle_pedido WHERE id_pedido = ? ORDER BY codigo_lote", MAPEO_DETALLE, idPedido);
	}

	/**
	 * Crea un pedido con {@code sp_crear_pedido}.
	 *
	 * @param dto datos del pedido
	 * @return identificador del pedido
	 */
	public Long crear(RegistroPedidoDTO dto) {
		return jdbcTemplate.queryForObject("CALL sp_crear_pedido(?, ?, ?, ?::jsonb, NULL)", Long.class, dto.idCliente(),
				dto.fechaEstimadaEntrega(), dto.observaciones(), detalleJson(dto.detalle()));
	}

	/**
	 * Confirma un pedido con {@code sp_confirmar_pedido}.
	 *
	 * @param id pedido
	 */
	public void confirmar(Long id) {
		jdbcTemplate.update("CALL sp_confirmar_pedido(?)", id);
	}

	/**
	 * Cancela un pedido con {@code sp_cancelar_pedido}.
	 *
	 * @param id pedido
	 */
	public void cancelar(Long id) {
		jdbcTemplate.update("CALL sp_cancelar_pedido(?)", id);
	}

	/**
	 * Convierte el detalle en el arreglo JSON que espera el procedimiento. Todos
	 * los valores son numéricos, así que no requieren escape.
	 *
	 * @param detalle aves por lote
	 * @return arreglo JSON
	 */
	static String detalleJson(List<LineaPedidoDTO> detalle) {
		return detalle.stream()
				.map(l -> String.format(Locale.ROOT, "{\"idLote\":%d,\"cantidadAves\":%d}", l.idLote(), l.cantidadAves()))
				.collect(Collectors.joining(",", "[", "]"));
	}
}
