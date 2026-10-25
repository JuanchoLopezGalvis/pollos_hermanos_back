package co.edu.unbosque.pollos_hermanos_back.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CarteraVencidaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroVentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentasClienteDTO;

/**
 * Acceso a ventas, pagos, entregas y cartera. Las escrituras pasan por
 * procedimientos que hacen {@code COMMIT}.
 */
@Repository
public class VentaRepository {

	/** Mapeo de ventas. */
	private static final DataClassRowMapper<VentaDTO> MAPEO_VENTA = new DataClassRowMapper<>(VentaDTO.class);

	/** Mapeo de pagos. */
	private static final DataClassRowMapper<PagoDTO> MAPEO_PAGO = new DataClassRowMapper<>(PagoDTO.class);

	/** Mapeo de entregas. */
	private static final DataClassRowMapper<EntregaDTO> MAPEO_ENTREGA = new DataClassRowMapper<>(EntregaDTO.class);

	/** Mapeo de la cartera vencida. */
	private static final DataClassRowMapper<CarteraVencidaDTO> MAPEO_CARTERA = new DataClassRowMapper<>(CarteraVencidaDTO.class);

	/** Mapeo del reporte por cliente. */
	private static final DataClassRowMapper<VentasClienteDTO> MAPEO_CLIENTE = new DataClassRowMapper<>(VentasClienteDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public VentaRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene las ventas, de la más reciente a la más antigua.
	 *
	 * @return ventas
	 */
	public List<VentaDTO> obtenerVentas() {
		return jdbcTemplate.query("SELECT * FROM v_venta ORDER BY fecha_venta DESC, id_venta DESC", MAPEO_VENTA);
	}

	/**
	 * Busca una venta.
	 *
	 * @param id identificador
	 * @return la venta, si existe
	 */
	public Optional<VentaDTO> buscarVenta(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_venta WHERE id_venta = ?", MAPEO_VENTA, id).stream().findFirst();
	}

	/**
	 * Registra una venta con {@code sp_registrar_venta}.
	 *
	 * @param dto datos de la venta
	 * @return identificador de la venta
	 */
	public Long registrarVenta(RegistroVentaDTO dto) {
		return ((Number) jdbcTemplate.queryForMap("CALL sp_registrar_venta(?, ?, ?, ?, ?, NULL, NULL)", dto.idPedido(),
				dto.fechaVenta(), dto.pesoTotalLibras(), dto.precioLibra(), dto.fechaLimitePago()).get("p_id_venta")).longValue();
	}

	/**
	 * Obtiene los pagos de una venta.
	 *
	 * @param idVenta venta
	 * @return pagos, del más antiguo al más reciente
	 */
	public List<PagoDTO> obtenerPagos(Long idVenta) {
		return jdbcTemplate.query("SELECT * FROM v_pago WHERE id_venta = ? ORDER BY fecha, id_pago", MAPEO_PAGO, idVenta);
	}

	/**
	 * Registra un pago con {@code sp_registrar_pago}.
	 *
	 * @param idVenta            venta
	 * @param codigoMetodo       código del método de pago
	 * @param fecha              fecha, o {@code null} para hoy
	 * @param monto              valor
	 * @param referenciaPasarela transacción aprobada, solo en pagos en línea
	 * @return identificador del pago
	 */
	public Long registrarPago(Long idVenta, String codigoMetodo, LocalDate fecha, BigDecimal monto, String referenciaPasarela) {
		return ((Number) jdbcTemplate.queryForMap("CALL sp_registrar_pago(?, ?, ?, ?, ?, NULL, NULL)", idVenta, codigoMetodo,
				fecha, monto, referenciaPasarela).get("p_id_pago")).longValue();
	}

	/**
	 * Busca un pago por la referencia de la pasarela.
	 *
	 * @param referencia identificador de la transacción
	 * @return el pago, si ya se registró
	 */
	public Optional<PagoDTO> buscarPagoPorReferencia(String referencia) {
		return jdbcTemplate.query("SELECT * FROM v_pago WHERE referencia_pasarela = ?", MAPEO_PAGO, referencia).stream()
				.findFirst();
	}

	/**
	 * Obtiene las entregas, de la más reciente a la más antigua.
	 *
	 * @return entregas
	 */
	public List<EntregaDTO> obtenerEntregas() {
		return jdbcTemplate.query("SELECT * FROM v_entrega ORDER BY id_entrega DESC", MAPEO_ENTREGA);
	}

	/**
	 * Busca una entrega.
	 *
	 * @param id identificador
	 * @return la entrega, si existe
	 */
	public Optional<EntregaDTO> buscarEntrega(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_entrega WHERE id_entrega = ?", MAPEO_ENTREGA, id).stream().findFirst();
	}

	/**
	 * Programa la entrega de una venta con {@code sp_registrar_entrega}.
	 *
	 * @param idVenta venta
	 * @param dto     transportista y dirección
	 * @return identificador de la entrega
	 */
	public Long registrarEntrega(Long idVenta, RegistroEntregaDTO dto) {
		return jdbcTemplate.queryForObject("CALL sp_registrar_entrega(?, ?, ?, NULL)", Long.class, idVenta, dto.idEmpleado(),
				dto.observaciones());
	}

	/**
	 * Marca una entrega como entregada o devuelta con {@code sp_actualizar_entrega}.
	 *
	 * @param id  entrega
	 * @param dto resultado
	 */
	public void actualizarEntrega(Long id, ActualizacionEntregaDTO dto) {
		jdbcTemplate.update("CALL sp_actualizar_entrega(?, ?, ?, ?)", id, dto.estado(), dto.fechaEntrega(), dto.observaciones());
	}

	/**
	 * Obtiene la cartera vencida con {@code fn_cartera_vencida}.
	 *
	 * @return ventas con saldo después de su fecha límite
	 */
	public List<CarteraVencidaDTO> obtenerCarteraVencida() {
		return jdbcTemplate.query("SELECT * FROM fn_cartera_vencida()", MAPEO_CARTERA);
	}

	/**
	 * Marca la cartera vencida con {@code sp_marcar_cartera_vencida}.
	 *
	 * @return ventas que pasaron a vencidas
	 */
	public int marcarCarteraVencida() {
		Integer marcadas = jdbcTemplate.queryForObject("CALL sp_marcar_cartera_vencida(NULL)", Integer.class);
		return marcadas == null ? 0 : marcadas;
	}

	/**
	 * Obtiene el reporte de ventas por cliente.
	 *
	 * @return ventas por cliente, de mayor a menor valor vendido
	 */
	public List<VentasClienteDTO> obtenerVentasPorCliente() {
		return jdbcTemplate.query("SELECT * FROM v_ventas_por_cliente ORDER BY valor_vendido DESC, cliente", MAPEO_CLIENTE);
	}

	/**
	 * Lista las ventas de un cliente, de la más reciente a la más antigua.
	 *
	 * @param idCliente identificador del cliente
	 * @return ventas del cliente con su saldo
	 */
	public List<VentaDTO> obtenerVentasDeCliente(Long idCliente) {
		return jdbcTemplate.query("SELECT * FROM v_venta WHERE id_cliente = ? ORDER BY fecha_venta DESC, id_venta DESC",
				MAPEO_VENTA, idCliente);
	}
}
