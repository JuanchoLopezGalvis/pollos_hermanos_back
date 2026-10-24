package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import co.edu.unbosque.pollos_hermanos_back.configuration.ModelMapperConfig;
import co.edu.unbosque.pollos_hermanos_back.dto.GalponDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GalponOcupacionDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.EstadoGalpon;
import co.edu.unbosque.pollos_hermanos_back.entity.Galpon;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.GalponRepository;
import co.edu.unbosque.pollos_hermanos_back.repository.LoteRepository;

/**
 * Pruebas de {@link GalponService}.
 */
class GalponServiceTest {

	/** Repositorio simulado. */
	private final GalponRepository galponRepository = mock(GalponRepository.class);

	/** Acceso a lotes simulado. */
	private final LoteRepository loteRepository = mock(LoteRepository.class);

	/** Servicio bajo prueba, con el ModelMapper real. */
	private final GalponService galponService = new GalponService(galponRepository, loteRepository,
			new ModelMapperConfig().modelMapper());

	/** Crear un galpón sin estado lo deja disponible y no usa el id enviado. */
	@Test
	void creaGalponDisponible() {
		GalponDTO dto = new GalponDTO();
		dto.setId(99L);
		dto.setNombre("Galpón 1");
		dto.setCapacidadMaxima(5000);
		when(galponRepository.saveAndFlush(any(Galpon.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		GalponDTO creado = galponService.crear(dto);

		ArgumentCaptor<Galpon> guardado = ArgumentCaptor.forClass(Galpon.class);
		Mockito.verify(galponRepository).saveAndFlush(guardado.capture());
		assertNull(guardado.getValue().getId());
		assertEquals(EstadoGalpon.DISPONIBLE, creado.getEstado());
		assertEquals(5000, creado.getCapacidadMaxima());
	}

	/** Actualizar sin estado conserva el actual (por ejemplo, ocupado). */
	@Test
	void actualizarSinEstadoConservaElActual() {
		Galpon galpon = new Galpon("Galpón 1", 5000);
		galpon.setId(1L);
		galpon.setEstado(EstadoGalpon.OCUPADO);
		when(galponRepository.findById(1L)).thenReturn(Optional.of(galpon));
		when(galponRepository.saveAndFlush(any(Galpon.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		GalponDTO cambios = new GalponDTO();
		cambios.setNombre("Galpón norte");
		cambios.setCapacidadMaxima(6000);

		GalponDTO actualizado = galponService.actualizar(1L, cambios);

		assertEquals("Galpón norte", actualizado.getNombre());
		assertEquals(EstadoGalpon.OCUPADO, actualizado.getEstado());
	}

	/** La ocupación viene de la vista de PostgreSQL, con varios lotes por galpón. */
	@Test
	void ocupacionConVariosLotes() {
		when(loteRepository.obtenerOcupacionGalpones()).thenReturn(List.of(
				new GalponOcupacionDTO(1L, "Galpón 1", 600, "OCUPADO", 6, 580, 20, LocalDate.of(2026, 11, 2))));

		List<GalponOcupacionDTO> ocupacion = galponService.obtenerOcupacion();

		assertEquals(6, ocupacion.get(0).lotesActivos());
		assertEquals(20, ocupacion.get(0).capacidadLibre());
	}

	/** Consultar un galpón inexistente lanza 404. */
	@Test
	void galponInexistenteLanzaExcepcion() {
		when(galponRepository.findById(9L)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> galponService.obtenerPorId(9L));
	}
}
