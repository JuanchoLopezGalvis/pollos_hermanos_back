package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.CompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EtapaAlimentacionDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.InventarioAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroCompraAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroConsumoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TipoAlimentoDTO;
import co.edu.unbosque.pollos_hermanos_back.repository.AlimentacionRepository;

/**
 * Compras y consumo de alimento por etapas. PostgreSQL valida el proveedor, la
 * etapa según la edad del lote y el inventario, y congela el costo promedio del
 * bulto en cada consumo. Sus métodos no son {@code @Transactional} porque los
 * procedimientos hacen su propio {@code COMMIT}.
 */
@Service
public class AlimentacionService {

	/** Acceso a la alimentación. */
	private final AlimentacionRepository alimentacionRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param alimentacionRepository acceso a la alimentación
	 */
	public AlimentacionService(AlimentacionRepository alimentacionRepository) {
		this.alimentacionRepository = alimentacionRepository;
	}

	/**
	 * Obtiene las etapas de alimentación.
	 *
	 * @return etapas en orden
	 */
	public List<EtapaAlimentacionDTO> obtenerEtapas() {
		return alimentacionRepository.obtenerEtapas();
	}

	/**
	 * Obtiene el inventario por tipo de alimento.
	 *
	 * @return inventario
	 */
	public List<InventarioAlimentoDTO> obtenerInventario() {
		return alimentacionRepository.obtenerInventario();
	}

	/**
	 * Registra un tipo de alimento.
	 *
	 * @param dto datos del tipo
	 * @return el tipo creado con su inventario en cero
	 */
	public InventarioAlimentoDTO crearTipo(TipoAlimentoDTO dto) {
		return alimentacionRepository.buscarInventario(alimentacionRepository.crearTipo(dto));
	}

	/**
	 * Obtiene las compras de alimento.
	 *
	 * @return compras
	 */
	public List<CompraAlimentoDTO> obtenerCompras() {
		return alimentacionRepository.obtenerCompras();
	}

	/**
	 * Registra una compra de alimento.
	 *
	 * @param dto datos de la compra
	 * @return la compra registrada
	 */
	public CompraAlimentoDTO registrarCompra(RegistroCompraAlimentoDTO dto) {
		return alimentacionRepository.buscarCompra(alimentacionRepository.registrarCompra(dto));
	}

	/**
	 * Obtiene los consumos de alimento.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return consumos
	 */
	public List<ConsumoAlimentoDTO> obtenerConsumos(Long idLote) {
		return alimentacionRepository.obtenerConsumos(idLote);
	}

	/**
	 * Registra el consumo de alimento de un lote.
	 *
	 * @param dto datos del consumo
	 * @return el consumo registrado con su costo
	 */
	public ConsumoAlimentoDTO registrarConsumo(RegistroConsumoAlimentoDTO dto) {
		return alimentacionRepository.buscarConsumo(alimentacionRepository.registrarConsumo(dto));
	}
}
