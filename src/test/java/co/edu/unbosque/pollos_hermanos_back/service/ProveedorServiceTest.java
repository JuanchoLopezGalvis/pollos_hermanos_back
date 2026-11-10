package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.ProveedorDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ProveedorRepository;

/**
 * Pruebas de {@link ProveedorService}.
 */
class ProveedorServiceTest {

	/** Repositorio simulado. */
	private final ProveedorRepository proveedorRepository = mock(ProveedorRepository.class);

	/** Servicio bajo prueba. */
	private final ProveedorService proveedorService = new ProveedorService(proveedorRepository);

	/** Datos de un proveedor de alimento. */
	private final ProveedorDTO dto = new ProveedorDTO(null, "900111", "Concentrados", null, null, null, true, false, null);

	/** Crear un proveedor devuelve el registro activo. */
	@Test
	void creaProveedor() {
		when(proveedorRepository.crear(dto)).thenReturn(2L);
		when(proveedorRepository.buscar(2L))
				.thenReturn(Optional.of(new ProveedorDTO(2L, "900111", "Concentrados", null, null, null, true, false, true)));

		assertTrue(proveedorService.crear(dto).activo());
	}

	/** Modificar un proveedor inexistente responde 404. */
	@Test
	void modificarInexistente() {
		when(proveedorRepository.actualizar(9L, dto)).thenReturn(false);

		assertThrows(RecursoNoEncontradoException.class, () -> proveedorService.actualizar(9L, dto));
	}
}
