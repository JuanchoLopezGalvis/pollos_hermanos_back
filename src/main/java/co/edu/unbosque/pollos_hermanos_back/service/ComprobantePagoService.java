package co.edu.unbosque.pollos_hermanos_back.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.PagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.exception.CorreoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Envía al cliente el comprobante de un pago recién registrado, con el peso
 * de los pollos, el precio por libra, el valor pagado y el saldo que queda.
 * <p>
 * El pago ya quedó registrado cuando se llama: si el correo falla, solo se
 * deja constancia en el log y el pago no se deshace.
 */
@Service
public class ComprobantePagoService {

	/** Plantilla del comprobante. */
	static final String PLANTILLA = "correo/comprobante-pago";

	/** Registro de errores de envío. */
	private static final Logger log = LoggerFactory.getLogger(ComprobantePagoService.class);

	/** Fechas como las ve el cliente. */
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	/** Separadores colombianos: punto de miles y coma decimal. */
	private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.of("es", "CO"));

	/** Acceso a ventas y pagos. */
	private final VentaRepository ventaRepository;

	/** Acceso a clientes. */
	private final ClienteRepository clienteRepository;

	/** Usuarios, para el correo de la cuenta si el cliente no tiene uno. */
	private final UsuarioRepository usuarioRepository;

	/** Envío de correos. */
	private final CorreoService correoService;

	/**
	 * Crea el servicio.
	 *
	 * @param ventaRepository   acceso a ventas y pagos
	 * @param clienteRepository acceso a clientes
	 * @param usuarioRepository acceso a usuarios
	 * @param correoService     envío de correos
	 */
	public ComprobantePagoService(VentaRepository ventaRepository, ClienteRepository clienteRepository,
			UsuarioRepository usuarioRepository, CorreoService correoService) {
		this.ventaRepository = ventaRepository;
		this.clienteRepository = clienteRepository;
		this.usuarioRepository = usuarioRepository;
		this.correoService = correoService;
	}

	/**
	 * Envía el comprobante del pago con la referencia dada.
	 *
	 * @param referenciaPasarela transacción aprobada de la pasarela
	 * @return {@code true} si el correo salió
	 */
	public boolean enviar(String referenciaPasarela) {
		Optional<PagoDTO> pago = ventaRepository.buscarPagoPorReferencia(referenciaPasarela);
		Optional<VentaDTO> venta = pago.flatMap(p -> ventaRepository.buscarVenta(p.idVenta()));
		Optional<ClienteDTO> cliente = venta.flatMap(v -> clienteRepository.buscar(v.idCliente()));
		if (cliente.isEmpty()) {
			log.warn("No se envió el comprobante de la transacción {}: no se encontró el pago, la venta o el cliente.",
					referenciaPasarela);
			return false;
		}
		String destinatario = correoDe(cliente.get());
		if (destinatario == null) {
			log.warn("No se envió el comprobante de la venta {}: el cliente {} no tiene correo.", venta.get().idVenta(),
					cliente.get().idCliente());
			return false;
		}
		try {
			correoService.enviarPlantilla(destinatario, "Comprobante de pago - Compra #" + venta.get().idVenta(), PLANTILLA,
					variables(pago.get(), venta.get(), cliente.get()));
			return true;
		} catch (CorreoException e) {
			log.error("No se pudo enviar el comprobante del pago {} a {}.", pago.get().idPago(), destinatario, e);
			return false;
		}
	}

	/**
	 * Correo del cliente o, si no tiene, el de su cuenta del portal.
	 *
	 * @param cliente cliente
	 * @return el correo, o {@code null} si no tiene ninguno
	 */
	private String correoDe(ClienteDTO cliente) {
		if (cliente.correo() != null && !cliente.correo().isBlank()) {
			return cliente.correo().trim();
		}
		if (cliente.idUsuario() == null) {
			return null;
		}
		return usuarioRepository.findById(cliente.idUsuario()).map(Usuario::getCorreo).filter(c -> !c.isBlank()).orElse(null);
	}

	/**
	 * Valores ya formateados que muestra la plantilla.
	 *
	 * @param pago    pago
	 * @param venta   venta pagada, con el saldo después del pago
	 * @param cliente cliente
	 * @return variables de la plantilla
	 */
	static Map<String, Object> variables(PagoDTO pago, VentaDTO venta, ClienteDTO cliente) {
		Map<String, Object> v = new HashMap<>();
		v.put("cliente", cliente.nombre());
		v.put("documento", cliente.documento());
		v.put("numeroPago", pago.idPago());
		v.put("fechaPago", fecha(pago.fecha()));
		v.put("metodo", pago.metodo());
		v.put("referencia", pago.referenciaPasarela());
		v.put("montoPagado", pesos(pago.monto()));
		v.put("numeroVenta", venta.idVenta());
		v.put("fechaVenta", fecha(venta.fechaVenta()));
		v.put("cantidadAves", numero(BigDecimal.valueOf(venta.cantidadAves()), 0));
		v.put("pesoTotal", numero(venta.pesoTotalLibras(), 2) + " lb");
		v.put("pesoPromedio", numero(pesoPromedio(venta), 2) + " lb");
		v.put("precioLibra", pesos(venta.precioLibra()));
		v.put("valorTotal", pesos(venta.valorTotal()));
		v.put("totalPagado", pesos(venta.pagado()));
		v.put("saldo", pesos(venta.saldo()));
		v.put("pagada", venta.saldo().signum() <= 0);
		v.put("fechaLimite", fecha(venta.fechaLimitePago()));
		return v;
	}

	/**
	 * Peso promedio de cada pollo de la venta.
	 *
	 * @param venta venta
	 * @return libras por pollo
	 */
	static BigDecimal pesoPromedio(VentaDTO venta) {
		if (venta.cantidadAves() == null || venta.cantidadAves() == 0) {
			return BigDecimal.ZERO;
		}
		return venta.pesoTotalLibras().divide(BigDecimal.valueOf(venta.cantidadAves()), 2, RoundingMode.HALF_UP);
	}

	/**
	 * Valor en pesos sin decimales, por ejemplo {@code $ 1.250.000}.
	 *
	 * @param valor valor
	 * @return texto
	 */
	static String pesos(BigDecimal valor) {
		return "$ " + numero(valor, 0);
	}

	/**
	 * Número con punto de miles y hasta los decimales indicados.
	 *
	 * @param valor     valor
	 * @param decimales decimales máximos
	 * @return texto
	 */
	private static String numero(BigDecimal valor, int decimales) {
		DecimalFormat formato = new DecimalFormat(decimales == 0 ? "#,##0" : "#,##0." + "#".repeat(decimales), SIMBOLOS);
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return formato.format(valor == null ? BigDecimal.ZERO : valor);
	}

	/**
	 * Fecha en formato día/mes/año.
	 *
	 * @param fecha fecha
	 * @return texto, o un guion si no hay fecha
	 */
	private static String fecha(LocalDate fecha) {
		return fecha == null ? "-" : fecha.format(FECHA);
	}
}
