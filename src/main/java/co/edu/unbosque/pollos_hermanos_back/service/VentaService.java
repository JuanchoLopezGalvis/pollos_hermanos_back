package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ActualizacionEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.CarteraVencidaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.EntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEntregaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroVentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoCarteraDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentasClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Ventas, pagos parciales, entregas y cartera. Las reglas (pedido confirmado,
 * precio fijo, saldo, transportista, cartera vencida) viven en PostgreSQL. Sus
 * métodos no son {@code @Transactional} porque los procedimientos hacen su
 * propio {@code COMMIT}.
 */
@Service
public class VentaService {

	/** Acceso a las ventas. */
	private final VentaRepository ventaRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param ventaRepository acceso a las ventas
	 */
	public VentaService(VentaRepository ventaRepository) {
		this.ventaRepository = ventaRepository;
	}

	/**
	 * Obtiene las ventas.
	 *
	 * @return ventas
	 */
	public List<VentaDTO> obtenerVentas() {
		return ventaRepository.obtenerVentas();
	}

	/**
	 * Obtiene una venta.
	 *
	 * @param id identificador
	 * @return la venta con su saldo
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public VentaDTO obtenerVenta(Long id) {
		return ventaRepository.buscarVenta(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id + "."));
	}

	/**
	 * Registra la venta de un pedido confirmado.
	 *
	 * @param dto datos de la venta
	 * @return la venta registrada
	 */
	public VentaDTO registrarVenta(RegistroVentaDTO dto) {
		return obtenerVenta(ventaRepository.registrarVenta(dto));
	}

	/**
	 * Obtiene los pagos de una venta.
	 *
	 * @param idVenta venta
	 * @return pagos
	 * @throws RecursoNoEncontradoException si la venta no existe
	 */
	public List<PagoDTO> obtenerPagos(Long idVenta) {
		obtenerVenta(idVenta);
		return ventaRepository.obtenerPagos(idVenta);
	}

	/**
	 * Registra un pago manual (efectivo, transferencia o crédito).
	 *
	 * @param idVenta venta
	 * @param dto     datos del pago
	 * @return la venta con su saldo actualizado
	 */
	public VentaDTO registrarPago(Long idVenta, RegistroPagoDTO dto) {
		ventaRepository.registrarPago(idVenta, dto.codigoMetodo(), dto.fecha(), dto.monto(), null);
		return obtenerVenta(idVenta);
	}

	/**
	 * Obtiene las entregas.
	 *
	 * @return entregas
	 */
	public List<EntregaDTO> obtenerEntregas() {
		return ventaRepository.obtenerEntregas();
	}

	/**
	 * Programa la entrega de una venta.
	 *
	 * @param idVenta venta
	 * @param dto     transportista y dirección
	 * @return la entrega programada
	 */
	public EntregaDTO registrarEntrega(Long idVenta, RegistroEntregaDTO dto) {
		return obtenerEntrega(ventaRepository.registrarEntrega(idVenta, dto));
	}

	/**
	 * Marca una entrega como entregada o devuelta.
	 *
	 * @param id  entrega
	 * @param dto resultado
	 * @return la entrega actualizada
	 */
	public EntregaDTO actualizarEntrega(Long id, ActualizacionEntregaDTO dto) {
		ventaRepository.actualizarEntrega(id, dto);
		return obtenerEntrega(id);
	}

	/**
	 * Obtiene la cartera vencida.
	 *
	 * @return ventas con saldo después de su fecha límite
	 */
	public List<CarteraVencidaDTO> obtenerCarteraVencida() {
		return ventaRepository.obtenerCarteraVencida();
	}

	/**
	 * Marca como vencidas las ventas con saldo después de su fecha límite.
	 *
	 * @return ventas marcadas
	 */
	public ResultadoCarteraDTO marcarCarteraVencida() {
		return new ResultadoCarteraDTO(ventaRepository.marcarCarteraVencida());
	}

	/**
	 * Obtiene el reporte de ventas por cliente.
	 *
	 * @return ventas por cliente
	 */
	public List<VentasClienteDTO> obtenerVentasPorCliente() {
		return ventaRepository.obtenerVentasPorCliente();
	}

	/**
	 * Busca una entrega.
	 *
	 * @param id identificador
	 * @return la entrega
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private EntregaDTO obtenerEntrega(Long id) {
		return ventaRepository.buscarEntrega(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la entrega con id " + id + "."));
	}
}
