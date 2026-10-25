package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.AplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroAplicacionSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraSanitariaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.SanidadRepository;

/**
 * Vitaminas y medicamentos: catálogo, compras y aplicaciones por lote. No se
 * lleva inventario de medicamentos (fuera del alcance); el costo de cada
 * aplicación se carga al lote al costo promedio de compra.
 */
@Service
public class SanidadService {

	/** Acceso a la sanidad. */
	private final SanidadRepository sanidadRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param sanidadRepository acceso a la sanidad
	 */
	public SanidadService(SanidadRepository sanidadRepository) {
		this.sanidadRepository = sanidadRepository;
	}

	/**
	 * Obtiene los productos sanitarios.
	 *
	 * @return productos
	 */
	public List<ProductoSanitarioDTO> obtenerProductos() {
		return sanidadRepository.obtenerProductos();
	}

	/**
	 * Registra un producto.
	 *
	 * @param dto datos del producto
	 * @return el producto creado
	 */
	public ProductoSanitarioDTO crearProducto(RegistroProductoSanitarioDTO dto) {
		return obtenerProducto(sanidadRepository.crearProducto(dto));
	}

	/**
	 * Modifica un producto.
	 *
	 * @param id  identificador
	 * @param dto nuevos datos
	 * @return el producto actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public ProductoSanitarioDTO actualizarProducto(Long id, RegistroProductoSanitarioDTO dto) {
		if (!sanidadRepository.actualizarProducto(id, dto)) {
			throw new RecursoNoEncontradoException("No existe el producto con id " + id + ".");
		}
		return obtenerProducto(id);
	}

	/**
	 * Obtiene las compras sanitarias.
	 *
	 * @return compras
	 */
	public List<CompraSanitariaDTO> obtenerCompras() {
		return sanidadRepository.obtenerCompras();
	}

	/**
	 * Registra una compra sanitaria.
	 *
	 * @param dto datos de la compra
	 * @return la compra registrada
	 */
	public CompraSanitariaDTO registrarCompra(RegistroCompraSanitariaDTO dto) {
		return sanidadRepository.registrarCompra(dto);
	}

	/**
	 * Obtiene las aplicaciones sanitarias.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return aplicaciones
	 */
	public List<AplicacionSanitariaDTO> obtenerAplicaciones(Long idLote) {
		return sanidadRepository.obtenerAplicaciones(idLote);
	}

	/**
	 * Registra una aplicación sanitaria.
	 *
	 * @param dto datos de la aplicación
	 * @return la aplicación registrada con su costo
	 */
	public AplicacionSanitariaDTO registrarAplicacion(RegistroAplicacionSanitariaDTO dto) {
		return sanidadRepository.registrarAplicacion(dto);
	}

	/**
	 * Busca un producto.
	 *
	 * @param id identificador
	 * @return el producto
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private ProductoSanitarioDTO obtenerProducto(Long id) {
		return sanidadRepository.buscarProducto(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id + "."));
	}
}
