package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.dto.ClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroClienteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.ClienteRepository;

/**
 * Pruebas de {@link ClienteService}.
 */
class ClienteServiceTest {

	/** Repositorio simulado. */
	private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

	/** Servicio bajo prueba. */
	private final ClienteService clienteService = new ClienteService(clienteRepository);

	/** Crear un cliente devuelve el registro leído de la vista. */
	@Test
	void creaCliente() {
		RegistroClienteDTO dto = new RegistroClienteDTO(2L, "900123", "Asadero", null, "3001112233", null, null);
		when(clienteRepository.crear(dto)).thenReturn(4L);
		when(clienteRepository.buscar(4L)).thenReturn(Optional.of(
				new ClienteDTO(4L, 2L, "Restaurante", "900123", "Asadero", null, "3001112233", null, true)));

		assertEquals("Restaurante", clienteService.crear(dto).tipoCliente());
	}

	/** Modificar un cliente inexistente responde 404. */
	@Test
	void modificarInexistente() {
		RegistroClienteDTO dto = new RegistroClienteDTO(2L, "900123", "Asadero", null, "3001112233", null, null);
		when(clienteRepository.actualizar(9L, dto)).thenReturn(false);

		assertThrows(RecursoNoEncontradoException.class, () -> clienteService.actualizar(9L, dto));
	}
}
