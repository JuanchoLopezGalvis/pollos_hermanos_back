package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.EmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RetiroEmpleadoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.EmpleadoRepository;

/**
 * Pruebas de {@link EmpleadoService}.
 */
class EmpleadoServiceTest {

	/** Repositorio simulado. */
	private final EmpleadoRepository empleadoRepository = mock(EmpleadoRepository.class);

	/** Servicio bajo prueba. */
	private final EmpleadoService empleadoService = new EmpleadoService(empleadoRepository);

	/** Retirar llama al procedimiento con la fecha y devuelve al empleado inactivo. */
	@Test
	void retiraEmpleado() {
		LocalDate fecha = LocalDate.of(2026, 10, 20);
		when(empleadoRepository.buscar(4L)).thenReturn(Optional.of(new EmpleadoDTO(4L, "101", "Berta", "Uno", null, 5L,
				"BENEFICIADORA", "Beneficiadora", LocalDate.of(2026, 1, 1), fecha, false)));

		assertFalse(empleadoService.retirar(4L, new RetiroEmpleadoDTO(fecha)).activo());
		verify(empleadoRepository).retirar(4L, fecha);
	}

	/** Modificar un empleado inexistente responde 404. */
	@Test
	void modificarInexistente() {
		RegistroEmpleadoDTO dto = new RegistroEmpleadoDTO("101", "Berta", "Uno", null, 5L, LocalDate.of(2026, 1, 1));
		when(empleadoRepository.actualizar(9L, dto)).thenReturn(false);

		assertThrows(RecursoNoEncontradoException.class, () -> empleadoService.actualizar(9L, dto));
	}
}
