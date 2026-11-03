package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.pollos_hermanos_back.dto.AplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroAplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroProductoSanitarioDTO;

/**
 * Acceso a la sanidad: productos, compras y aplicaciones. Las compras y
 * aplicaciones pasan por sus procedimientos, que hacen {@code COMMIT}.
 */
@Repository
public class SanidadRepository {

	/** Mapeo de productos. */
	private static final DataClassRowMapper<ProductoSanitarioDTO> MAPEO_PRODUCTO = new DataClassRowMapper<>(ProductoSanitarioDTO.class);

	/** Mapeo de compras. */
	private static final DataClassRowMapper<CompraSanitariaDTO> MAPEO_COMPRA = new DataClassRowMapper<>(CompraSanitariaDTO.class);

	/** Mapeo de aplicaciones. */
	private static final DataClassRowMapper<AplicacionSanitariaDTO> MAPEO_APLICACION = new DataClassRowMapper<>(AplicacionSanitariaDTO.class);

	/** Cliente JDBC. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Crea el repositorio.
	 *
	 * @param jdbcTemplate cliente JDBC
	 */
	public SanidadRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Obtiene los productos sanitarios.
	 *
	 * @return productos ordenados por nombre
	 */
	public List<ProductoSanitarioDTO> obtenerProductos() {
		return jdbcTemplate.query("SELECT * FROM v_producto_sanitario ORDER BY nombre, presentacion", MAPEO_PRODUCTO);
	}

	/**
	 * Busca un producto.
	 *
	 * @param id identificador
	 * @return el producto, si existe
	 */
	public Optional<ProductoSanitarioDTO> buscarProducto(Long id) {
		return jdbcTemplate.query("SELECT * FROM v_producto_sanitario WHERE id_producto = ?", MAPEO_PRODUCTO, id).stream()
				.findFirst();
	}

	/**
	 * Registra un producto.
	 *
	 * @param dto datos del producto
	 * @return identificador asignado
	 */
	public Long crearProducto(RegistroProductoSanitarioDTO dto) {
		return jdbcTemplate.queryForObject("INSERT INTO producto_sanitario (nombre, tipo, presentacion, unidad_medida, id_proveedor)"
				+ " VALUES (?, ?::tipo_producto_sanitario, ?, ?, ?) RETURNING id_producto", Long.class, dto.nombre().trim(),
				dto.tipo(), dto.presentacion().trim(), dto.unidadMedida().trim(), dto.idProveedor());
	}

	/**
	 * Modifica un producto. Si {@code activo} es {@code null}, no cambia.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return {@code true} si existía
	 */
	public boolean actualizarProducto(Long id, RegistroProductoSanitarioDTO dto) {
		return jdbcTemplate.update("UPDATE producto_sanitario SET nombre = ?, tipo = ?::tipo_producto_sanitario, presentacion = ?,"
				+ " unidad_medida = ?, id_proveedor = ?, activo = COALESCE(?, activo) WHERE id_producto = ?", dto.nombre().trim(),
				dto.tipo(), dto.presentacion().trim(), dto.unidadMedida().trim(), dto.idProveedor(), dto.activo(), id) > 0;
	}

	/**
	 * Obtiene las compras sanitarias.
	 *
	 * @return compras, de la más reciente a la más antigua
	 */
	public List<CompraSanitariaDTO> obtenerCompras() {
		return jdbcTemplate.query("SELECT * FROM v_compra_sanitaria ORDER BY fecha DESC, id_compra_sanitaria DESC", MAPEO_COMPRA);
	}

	/**
	 * Registra una compra con {@code sp_registrar_compra_sanitaria}.
	 *
	 * @param dto datos de la compra
	 * @return la compra registrada
	 */
	public CompraSanitariaDTO registrarCompra(RegistroCompraSanitariaDTO dto) {
		Long id = jdbcTemplate.queryForObject("CALL sp_registrar_compra_sanitaria(?, ?, ?, ?, NULL)", Long.class,
				dto.idProducto(), dto.fecha(), dto.cantidad(), dto.costoUnitario());
		return jdbcTemplate.queryForObject("SELECT * FROM v_compra_sanitaria WHERE id_compra_sanitaria = ?", MAPEO_COMPRA, id);
	}

	/**
	 * Obtiene las aplicaciones, de todos los lotes o de uno.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return aplicaciones, de la más reciente a la más antigua
	 */
	public List<AplicacionSanitariaDTO> obtenerAplicaciones(Long idLote) {
		String sql = "SELECT * FROM v_aplicacion_sanitaria" + (idLote == null ? "" : " WHERE id_lote = ?")
				+ " ORDER BY fecha DESC, id_aplicacion DESC";
		return idLote == null ? jdbcTemplate.query(sql, MAPEO_APLICACION) : jdbcTemplate.query(sql, MAPEO_APLICACION, idLote);
	}

	/**
	 * Registra una aplicación con {@code sp_registrar_aplicacion_sanitaria}.
	 *
	 * @param dto datos de la aplicación
	 * @return la aplicación registrada con su costo
	 */
	public AplicacionSanitariaDTO registrarAplicacion(RegistroAplicacionSanitariaDTO dto) {
		Number id = (Number) jdbcTemplate.queryForMap("CALL sp_registrar_aplicacion_sanitaria(?, ?, ?, ?, ?, ?, NULL, NULL)",
				dto.idLote(), dto.idProducto(), dto.idEmpleado(), dto.fecha(), dto.dosis(), dto.observaciones())
				.get("p_id_aplicacion");
		return jdbcTemplate.queryForObject("SELECT * FROM v_aplicacion_sanitaria WHERE id_aplicacion = ?", MAPEO_APLICACION,
				id.longValue());
	}
}
