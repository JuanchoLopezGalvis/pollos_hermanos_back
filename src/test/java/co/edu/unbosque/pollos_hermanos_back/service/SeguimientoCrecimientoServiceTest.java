package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.pollos_hermanos_back.configuration.ModelMapperConfig;
import co.edu.unbosque.pollos_hermanos_back.document.SeguimientoCrecimiento;
import co.edu.unbosque.pollos_hermanos_back.dto.MuestreoPesoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.NovedadGalponDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.SeguimientoCrecimientoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.SeguimientoCrecimientoRepository;

/**
 * Pruebas de {@link SeguimientoCrecimientoService}.
 */
class SeguimientoCrecimientoServiceTest {

	/** Repositorio simulado. */
	private final SeguimientoCrecimientoRepository repositorio = mock(SeguimientoCrecimientoRepository.class);

	/** Verificador de lotes simulado. */
	private final VerificadorLote verificadorLote = mock(VerificadorLote.class);

	/** Servicio bajo prueba, con el ModelMapper real. */
	private final SeguimientoCrecimientoService servicio = new SeguimientoCrecimientoService(repositorio,
			verificadorLote, new ModelMapperConfig().modelMapper());

	/**
	 * Construye un seguimiento de prueba.
	 *
	 * @param fecha día del seguimiento
	 * @return DTO del seguimiento
	 */
	private static SeguimientoCrecimientoDTO seguimiento(LocalDate fecha) {
		SeguimientoCrecimientoDTO dto = new SeguimientoCrecimientoDTO();
		dto.setCodigoLote("L-001");
		dto.setFecha(fecha);
		dto.setPesoPromedioGramos(185.5);
		return dto;
	}

	/** La edad se calcula con la fecha de ingreso: el día de ingreso es el día 1. */
	@Test
	void calculaLaEdadConLaFechaDeIngreso() {
		when(verificadorLote.obtenerFechaIngresoLoteActivo("L-001")).thenReturn(LocalDate.of(2026, 10, 1));
		when(repositorio.save(any(SeguimientoCrecimiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		SeguimientoCrecimientoDTO dto = seguimiento(LocalDate.of(2026, 10, 8));
		MuestreoPesoDTO muestreo = new MuestreoPesoDTO();
		muestreo.setAvesPesadas(50);
		muestreo.setUniformidadPorcentaje(82.0);
		dto.setMuestreo(muestreo);
		NovedadGalponDTO novedad = new NovedadGalponDTO();
		novedad.setTipo("Cama húmeda");
		dto.setNovedades(List.of(novedad));

		SeguimientoCrecimientoDTO creado = servicio.crear(dto);

		assertEquals(8, creado.getEdadDias());
		assertEquals(50, creado.getMuestreo().getAvesPesadas());
		assertEquals("Cama húmeda", creado.getNovedades().get(0).getTipo());
	}

	/** Si no se envía la fecha, se usa la de hoy. */
	@Test
	void usaLaFechaActualSiNoSeEnvia() {
		when(verificadorLote.obtenerFechaIngresoLoteActivo("L-001")).thenReturn(LocalDate.now().minusDays(2));
		when(repositorio.save(any(SeguimientoCrecimiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		SeguimientoCrecimientoDTO creado = servicio.crear(seguimiento(null));

		assertEquals(LocalDate.now(), creado.getFecha());
		assertEquals(3, creado.getEdadDias());
	}

	/** Si el lote no está activo, no se guarda nada en MongoDB. */
	@Test
	void noGuardaSiElLoteNoEstaActivo() {
		when(verificadorLote.obtenerFechaIngresoLoteActivo("L-001"))
				.thenThrow(new ReglaNegocioException("El lote L-001 no está activo."));

		assertThrows(ReglaNegocioException.class, () -> servicio.crear(seguimiento(LocalDate.of(2026, 10, 8))));
		verify(repositorio, never()).save(any());
	}

	/** No se permite un seguimiento anterior al ingreso del lote. */
	@Test
	void rechazaFechaAnteriorAlIngreso() {
		when(verificadorLote.obtenerFechaIngresoLoteActivo("L-001")).thenReturn(LocalDate.of(2026, 10, 10));

		assertThrows(ReglaNegocioException.class, () -> servicio.crear(seguimiento(LocalDate.of(2026, 10, 8))));
		verify(repositorio, never()).save(any());
	}

	/** No se permiten dos seguimientos del mismo lote el mismo día. */
	@Test
	void rechazaSeguimientoDuplicadoDelMismoDia() {
		when(repositorio.existsByCodigoLoteAndFecha("L-001", LocalDate.of(2026, 10, 8))).thenReturn(true);

		assertThrows(ReglaNegocioException.class, () -> servicio.crear(seguimiento(LocalDate.of(2026, 10, 8))));
		verify(repositorio, never()).save(any());
	}

	/** Actualizar no cambia el lote ni la fecha. */
	@Test
	void actualizarConservaLoteYFecha() {
		SeguimientoCrecimiento existente = new SeguimientoCrecimiento();
		existente.setId("abc");
		existente.setCodigoLote("L-001");
		existente.setFecha(LocalDate.of(2026, 10, 8));
		existente.setPesoPromedioGramos(180.0);
		when(repositorio.findById("abc")).thenReturn(Optional.of(existente));
		when(verificadorLote.obtenerFechaIngresoLoteActivo("L-001")).thenReturn(LocalDate.of(2026, 10, 1));
		when(repositorio.save(any(SeguimientoCrecimiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		SeguimientoCrecimientoDTO cambios = new SeguimientoCrecimientoDTO();
		cambios.setCodigoLote("L-999");
		cambios.setFecha(LocalDate.of(2026, 10, 9));
		cambios.setPesoPromedioGramos(190.0);

		SeguimientoCrecimientoDTO actualizado = servicio.actualizar("abc", cambios);

		assertEquals("L-001", actualizado.getCodigoLote());
		assertEquals(LocalDate.of(2026, 10, 8), actualizado.getFecha());
		assertEquals(190.0, actualizado.getPesoPromedioGramos());
	}
}
