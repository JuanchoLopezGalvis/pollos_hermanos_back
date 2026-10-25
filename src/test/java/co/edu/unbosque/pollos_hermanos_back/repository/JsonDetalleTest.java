package co.edu.unbosque.pollos_hermanos_back.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.LineaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.LineaPedidoDTO;

/**
 * Pruebas del JSON que se envía a los procedimientos con detalle.
 */
class JsonDetalleTest {

	/** El detalle de un pedido se envía como arreglo JSON. */
	@Test
	void detalleDePedido() {
		assertEquals("[{\"idLote\":1,\"cantidadAves\":100},{\"idLote\":2,\"cantidadAves\":50}]",
				PedidoRepository.detalleJson(List.of(new LineaPedidoDTO(1L, 100), new LineaPedidoDTO(2L, 50))));
	}

	/** El detalle del beneficio conserva los decimales de la tarifa sin notación científica. */
	@Test
	void detalleDeBeneficio() {
		assertEquals("[{\"idEmpleado\":4,\"avesBeneficiadas\":300,\"tarifaPorAve\":900.50}]",
				GastoBeneficioRepository.detalleJson(List.of(new LineaBeneficioDTO(4L, 300, new BigDecimal("900.50")))));
	}
}
