package co.edu.unbosque.pollos_hermanos_back.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Pruebas de los límites de los campos numéricos: coinciden con las columnas de
 * PostgreSQL para que un valor fuera de rango responda 400 y no un error de la base.
 */
class LimitesNumericosTest {

	/** Validador de Bean Validation. */
	private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

	/**
	 * Campos que no pasaron la validación.
	 *
	 * @param objeto DTO a validar
	 * @return nombres de los campos con error
	 */
	private Set<String> camposInvalidos(Object objeto) {
		return validador.validate(objeto).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString)
				.collect(Collectors.toSet());
	}

	/** Un lote dentro de los límites es válido. */
	@Test
	void loteValido() {
		assertTrue(camposInvalidos(new RegistroLoteDTO("L-1", 1L, LocalDate.now(), 5000, new BigDecimal("2500.50"), null)).isEmpty());
	}

	/** Más de 100.000 aves o un costo con 3 decimales se rechazan. */
	@Test
	void loteFueraDeLimites() {
		assertEquals(Set.of("cantidadInicial", "costoPollito"),
				camposInvalidos(new RegistroLoteDTO("L-1", 1L, LocalDate.now(), 100_001, new BigDecimal("2500.555"), null)));
	}

	/** Un monto con más de 12 cifras enteras no cabe en NUMERIC(14,2). */
	@Test
	void montoDemasiadoGrande() {
		assertEquals(Set.of("monto"),
				camposInvalidos(new RegistroGastoDTO(1L, null, LocalDate.now(), new BigDecimal("1000000000000"), "Luz")));
	}

	/** El peso total de una venta cabe en NUMERIC(10,2): máximo 8 cifras enteras. */
	@Test
	void pesoVentaDemasiadoGrande() {
		assertEquals(Set.of("pesoTotalLibras"),
				camposInvalidos(new RegistroVentaDTO(1L, LocalDate.now(), new BigDecimal("100000000"), new BigDecimal("9000"), null)));
	}

	/** Un bulto no pesa más de 100 kg. */
	@Test
	void bultoDemasiadoPesado() {
		assertEquals(Set.of("pesoBultoKg"), camposInvalidos(new TipoAlimentoDTO("Inicio", 1L, new BigDecimal("150"))));
	}

	/** El teléfono vacío se acepta (es opcional); con espacios o letras no. */
	@Test
	void telefonoYDocumento() {
		assertTrue(camposInvalidos(new RegistroClienteDTO(1L, "900123456-7", "Asadero", null, "", "a@b.co", null)).isEmpty());
		assertEquals(Set.of("documento", "telefono"),
				camposInvalidos(new RegistroClienteDTO(1L, "1.020.304", "Asadero", null, "300 123 4567", "a@b.co", null)));
	}
}
