package co.edu.unbosque.pollos_hermanos_back.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.function.LongPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.CheckoutPagoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.IniciarPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.ResultadoPagoEnLineaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.TransaccionPasarelaDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.VentaDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.VentaRepository;

/**
 * Pago en línea de las ventas con el checkout de Wompi en su ambiente de pruebas.
 * <p>
 * El backend arma la URL firmada del checkout; el cliente paga en la página de
 * Wompi, de modo que los datos de tarjeta nunca pasan por el sistema. Al volver,
 * el backend consulta la transacción directamente a Wompi y solo registra el
 * pago (con {@code sp_registrar_pago}) si la pasarela la confirma como
 * aprobada. Solo se aceptan llaves de prueba: no se mueve dinero real.
 */
@Service
public class PagoEnLineaService {

	/** Moneda del sistema (S2). */
	static final String MONEDA = "COP";

	/** Estado de una transacción aprobada en Wompi. */
	static final String APROBADA = "APPROVED";

	/** Forma de las referencias que genera el sistema: PH-V{venta}-{marca de tiempo}. */
	private static final Pattern REFERENCIA = Pattern.compile("^PH-V(\\d+)-\\d+$");

	/** Acceso a ventas y pagos. */
	private final VentaRepository ventaRepository;

	/** Consulta de transacciones en la pasarela. */
	private final ClientePasarela clientePasarela;

	/** Llave pública del comercio en el sandbox. */
	private final String llavePublica;

	/** Secreto de integridad para firmar el checkout. */
	private final String secretoIntegridad;

	/** URL del checkout web de Wompi. */
	private final String urlCheckout;

	/** Reloj para generar referencias únicas. */
	private final Clock reloj;

	/**
	 * Crea el servicio.
	 *
	 * @param ventaRepository   acceso a ventas y pagos
	 * @param clientePasarela   consulta de transacciones
	 * @param llavePublica      llave pública del sandbox ({@code pub_test_...})
	 * @param secretoIntegridad secreto de integridad del sandbox
	 * @param urlCheckout       URL del checkout web
	 */
	@Autowired
	public PagoEnLineaService(VentaRepository ventaRepository, ClientePasarela clientePasarela,
			@Value("${pollos.wompi.llave-publica:}") String llavePublica,
			@Value("${pollos.wompi.secreto-integridad:}") String secretoIntegridad,
			@Value("${pollos.wompi.url-checkout:https://checkout.wompi.co/p/}") String urlCheckout) {
		this(ventaRepository, clientePasarela, llavePublica, secretoIntegridad, urlCheckout, Clock.systemUTC());
	}

	/**
	 * Crea el servicio con un reloj dado (para pruebas).
	 *
	 * @param ventaRepository   acceso a ventas y pagos
	 * @param clientePasarela   consulta de transacciones
	 * @param llavePublica      llave pública del sandbox
	 * @param secretoIntegridad secreto de integridad
	 * @param urlCheckout       URL del checkout web
	 * @param reloj             reloj
	 */
	PagoEnLineaService(VentaRepository ventaRepository, ClientePasarela clientePasarela, String llavePublica,
			String secretoIntegridad, String urlCheckout, Clock reloj) {
		this.ventaRepository = ventaRepository;
		this.clientePasarela = clientePasarela;
		this.llavePublica = llavePublica;
		this.secretoIntegridad = secretoIntegridad;
		this.urlCheckout = urlCheckout;
		this.reloj = reloj;
	}

	/**
	 * Prepara el checkout para pagar parte o todo el saldo de una venta.
	 *
	 * @param idVenta venta
	 * @param dto     monto y URL de retorno
	 * @return URL firmada del checkout
	 * @throws ReglaNegocioException si la pasarela no está configurada, la venta
	 *                               ya está pagada o el monto supera el saldo
	 */
	public CheckoutPagoDTO iniciar(Long idVenta, IniciarPagoEnLineaDTO dto) {
		verificarConfiguracion();
		VentaDTO venta = ventaRepository.buscarVenta(idVenta)
				.orElseThrow(() -> new ReglaNegocioException("No existe la venta con id " + idVenta + "."));
		if (venta.saldo().signum() <= 0) {
			throw new ReglaNegocioException("La venta " + idVenta + " ya está pagada.");
		}
		if (dto.monto().compareTo(venta.saldo()) > 0) {
			throw new ReglaNegocioException("El monto (" + dto.monto().toPlainString() + ") supera el saldo pendiente de la venta "
					+ idVenta + " (" + venta.saldo().toPlainString() + ").");
		}
		long centavos = dto.monto().movePointRight(2).longValueExact();
		String referencia = "PH-V" + idVenta + "-" + reloj.millis();
		String firma = sha256(referencia + centavos + MONEDA + secretoIntegridad);
		String url = urlCheckout + "?public-key=" + codificar(llavePublica) + "&currency=" + MONEDA + "&amount-in-cents="
				+ centavos + "&reference=" + codificar(referencia) + "&signature%3Aintegrity=" + firma + "&redirect-url="
				+ codificar(dto.urlRetorno());
		return new CheckoutPagoDTO(url, referencia, centavos, MONEDA);
	}

	/**
	 * Confirma un pago al volver de la pasarela. Consulta la transacción en
	 * Wompi y, si está aprobada, registra el pago en la venta. Es idempotente:
	 * confirmar dos veces la misma transacción no duplica el pago. Solo acepta
	 * ventas que pasen el filtro: el portal lo usa para que un cliente no
	 * confirme ni consulte pagos de ventas ajenas.
	 *
	 * @param idTransaccion transacción de la pasarela
	 * @param ventaPermitida indica si quien confirma puede ver la venta
	 * @return resultado del pago
	 */
	public ResultadoPagoEnLineaDTO confirmar(String idTransaccion, LongPredicate ventaPermitida) {
		TransaccionPasarelaDTO t = clientePasarela.consultarTransaccion(idTransaccion.trim());
		BigDecimal monto = BigDecimal.valueOf(t.montoEnCentavos()).movePointLeft(2);
		Long idVenta = idVentaDeReferencia(t.referencia());
		if (idVenta == null || !MONEDA.equals(t.moneda()) || !ventaPermitida.test(idVenta)) {
			return new ResultadoPagoEnLineaDTO(t.id(), t.estado(), false, null, monto, null,
					"La transacción no corresponde a un pago de este sistema.");
		}
		if (!APROBADA.equals(t.estado())) {
			return new ResultadoPagoEnLineaDTO(t.id(), t.estado(), false, idVenta, monto, null, mensajeEstado(t.estado()));
		}

		boolean yaEstaba = ventaRepository.buscarPagoPorReferencia(t.id()).isPresent();
		if (!yaEstaba) {
			try {
				ventaRepository.registrarPago(idVenta, "EN_LINEA", null, monto, t.id());
			} catch (DataAccessException e) {
				// El aviso de Wompi y el regreso del cliente pueden llegar al mismo tiempo:
				// si el otro ya lo registró, la referencia única lo rechaza y no es un error.
				if (ventaRepository.buscarPagoPorReferencia(t.id()).isEmpty()) {
					throw e;
				}
				yaEstaba = true;
			}
		}
		BigDecimal saldo = ventaRepository.buscarVenta(idVenta).map(VentaDTO::saldo).orElse(null);
		return new ResultadoPagoEnLineaDTO(t.id(), t.estado(), true, idVenta, monto, saldo,
				yaEstaba ? "Este pago ya estaba registrado en la venta " + idVenta + "."
						: "Pago aprobado y registrado en la venta " + idVenta + ".");
	}

	/**
	 * Obtiene la venta a partir de la referencia generada por el sistema.
	 *
	 * @param referencia referencia de la transacción
	 * @return identificador de la venta, o {@code null} si la referencia no es del sistema
	 */
	static Long idVentaDeReferencia(String referencia) {
		if (referencia == null) {
			return null;
		}
		Matcher m = REFERENCIA.matcher(referencia);
		return m.matches() ? Long.valueOf(m.group(1)) : null;
	}

	/**
	 * Explica al usuario un estado de transacción no aprobado.
	 *
	 * @param estado estado según la pasarela
	 * @return mensaje en español
	 */
	static String mensajeEstado(String estado) {
		return switch (estado) {
		case "DECLINED" -> "La pasarela rechazó el pago. No se registró nada en la venta.";
		case "VOIDED" -> "El pago fue anulado en la pasarela. No se registró nada en la venta.";
		case "PENDING" -> "El pago sigue pendiente en la pasarela. Vuelva a confirmarlo en unos minutos.";
		default -> "La pasarela reportó un error en el pago. No se registró nada en la venta.";
		};
	}

	/**
	 * Verifica que la pasarela tenga llaves de prueba configuradas.
	 *
	 * @throws ReglaNegocioException si faltan las llaves o no son de prueba
	 */
	private void verificarConfiguracion() {
		if (llavePublica == null || llavePublica.isBlank() || secretoIntegridad == null || secretoIntegridad.isBlank()) {
			throw new ReglaNegocioException("La pasarela de pagos no está configurada: faltan WOMPI_LLAVE_PUBLICA y"
					+ " WOMPI_SECRETO_INTEGRIDAD en el servidor.");
		}
		if (!llavePublica.startsWith("pub_test_")) {
			throw new ReglaNegocioException("La pasarela solo opera con llaves de prueba (pub_test_...): el sistema no cobra dinero real.");
		}
	}

	/**
	 * Calcula el SHA-256 en hexadecimal.
	 *
	 * @param texto texto a firmar
	 * @return firma en hexadecimal
	 */
	static String sha256(String texto) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible", e);
		}
	}

	/**
	 * Codifica un valor para la URL.
	 *
	 * @param valor valor
	 * @return valor codificado
	 */
	private static String codificar(String valor) {
		return URLEncoder.encode(valor, StandardCharsets.UTF_8);
	}
}
