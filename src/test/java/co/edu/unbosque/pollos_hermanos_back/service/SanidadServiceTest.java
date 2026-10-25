package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.ProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroProductoSanitarioDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.SanidadRepository;

/**
 * Pruebas de {@link SanidadService}.
 */
class SanidadServiceTest {

	/** Repositorio simulado. */
	private final SanidadRepository sanidadRepository = mock(SanidadRepository.class);

	/** Servicio bajo prueba. */
	private final SanidadService sanidadService = new SanidadService(sanidadRepository);

	/** Datos de un producto. */
	private final RegistroProductoSanitarioDTO dto = new RegistroProductoSanitarioDTO("Vitamina AD3E", "VITAMINA", "Frasco 1 L",
			"ml", 2L, null);

	/** Crear un producto devuelve el registro con su proveedor. */
	@Test
	void creaProducto() {
		when(sanidadRepository.crearProducto(dto)).thenReturn(6L);
		when(sanidadRepository.buscarProducto(6L)).thenReturn(Optional.of(new ProductoSanitarioDTO(6L, "Vitamina AD3E",
				"VITAMINA", "Frasco 1 L", "ml", 2L, "Veterinaria", true, (BigDecimal) null)));

		assertEquals("Veterinaria", sanidadService.crearProducto(dto).proveedor());
	}

	/** Modificar un producto inexistente responde 404. */
	@Test
	void modificarInexistente() {
		when(sanidadRepository.actualizarProducto(9L, dto)).thenReturn(false);

		assertThrows(RecursoNoEncontradoException.class, () -> sanidadService.actualizarProducto(9L, dto));
	}
}
