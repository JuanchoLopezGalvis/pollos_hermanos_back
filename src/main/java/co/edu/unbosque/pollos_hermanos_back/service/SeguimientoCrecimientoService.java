package co.edu.unbosque.pollos_hermanos_back.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.document.SeguimientoCrecimiento;
import co.edu.unbosque.pollos_hermanos_back.dto.CurvaCrecimientoSemanalDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.SeguimientoCrecimientoDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.exception.ReglaNegocioException;
import co.edu.unbosque.pollos_hermanos_back.repository.SeguimientoCrecimientoRepository;

/**
 * Registra y consulta en MongoDB el seguimiento diario del crecimiento de los
 * lotes.
 * <p>
 * Antes de guardar un documento verifica en PostgreSQL, mediante
 * {@link VerificadorLote}, que el lote exista y esté activo, y calcula la edad
 * con su fecha de ingreso. No implementa {@link CRUDOperation} porque los
 * documentos de MongoDB se identifican con un {@code String}.
 */
@Service
public class SeguimientoCrecimientoService {

	/** Repositorio de MongoDB. */
	private final SeguimientoCrecimientoRepository seguimientoRepository;

	/** Consulta el lote en PostgreSQL. */
	private final VerificadorLote verificadorLote;

	/** Conversor entre documento y DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param seguimientoRepository repositorio de MongoDB
	 * @param verificadorLote       consulta el lote en PostgreSQL
	 * @param modelMapper           conversor entre documento y DTO
	 */
	public SeguimientoCrecimientoService(SeguimientoCrecimientoRepository seguimientoRepository,
			VerificadorLote verificadorLote, ModelMapper modelMapper) {
		this.seguimientoRepository = seguimientoRepository;
		this.verificadorLote = verificadorLote;
		this.modelMapper = modelMapper;
	}

	/**
	 * Registra el seguimiento de un lote en un día. Si no se envía la fecha, se usa
	 * la actual.
	 *
	 * @param dto datos del seguimiento
	 * @return el seguimiento guardado, con su edad calculada
	 * Se admiten varios seguimientos del mismo lote el mismo día.
	 *
	 * @throws ReglaNegocioException si el lote no está activo o la fecha es
	 *         anterior a su ingreso
	 */
	public SeguimientoCrecimientoDTO crear(SeguimientoCrecimientoDTO dto) {
		SeguimientoCrecimiento seguimiento = modelMapper.map(dto, SeguimientoCrecimiento.class);
		seguimiento.setId(null);
		if (seguimiento.getFecha() == null) {
			seguimiento.setFecha(LocalDate.now());
		}
		seguimiento.setEdadDias(calcularEdad(seguimiento.getCodigoLote(), seguimiento.getFecha()));
		return aDTO(seguimientoRepository.save(seguimiento));
	}

	/**
	 * Obtiene todos los seguimientos.
	 *
	 * @return seguimientos registrados
	 */
	public List<SeguimientoCrecimientoDTO> obtenerTodos() {
		return seguimientoRepository.findAll().stream().map(this::aDTO).toList();
	}

	/**
	 * Obtiene un seguimiento por su identificador.
	 *
	 * @param id identificador del documento
	 * @return el seguimiento
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public SeguimientoCrecimientoDTO obtenerPorId(String id) {
		return aDTO(buscar(id));
	}

	/**
	 * Obtiene los seguimientos de un lote ordenados por fecha.
	 *
	 * @param codigoLote código del lote
	 * @return seguimientos del lote
	 */
	public List<SeguimientoCrecimientoDTO> obtenerPorLote(String codigoLote) {
		return seguimientoRepository.findByCodigoLoteOrderByFechaAsc(codigoLote).stream().map(this::aDTO).toList();
	}

	/**
	 * Calcula la curva de crecimiento semanal de un lote.
	 *
	 * @param codigoLote código del lote
	 * @return peso promedio por semana de vida
	 */
	public List<CurvaCrecimientoSemanalDTO> calcularCurvaSemanal(String codigoLote) {
		return seguimientoRepository.calcularCurvaSemanal(codigoLote);
	}

	/**
	 * Actualiza las mediciones de un seguimiento. El lote y la fecha no cambian,
	 * porque identifican el registro; para corregirlos se elimina y se registra de
	 * nuevo. El lote debe seguir activo.
	 *
	 * @param id  identificador del documento
	 * @param dto nuevos datos; los campos en {@code null} se dejan como estaban
	 * @return el seguimiento actualizado
	 * @throws RecursoNoEncontradoException si no existe
	 * @throws ReglaNegocioException        si el lote ya no está activo
	 */
	public SeguimientoCrecimientoDTO actualizar(String id, SeguimientoCrecimientoDTO dto) {
		SeguimientoCrecimiento seguimiento = buscar(id);
		String codigoLote = seguimiento.getCodigoLote();
		LocalDate fecha = seguimiento.getFecha();
		modelMapper.map(dto, seguimiento);
		seguimiento.setId(id);
		seguimiento.setCodigoLote(codigoLote);
		seguimiento.setFecha(fecha);
		seguimiento.setEdadDias(calcularEdad(codigoLote, fecha));
		return aDTO(seguimientoRepository.save(seguimiento));
	}

	/**
	 * Elimina un seguimiento.
	 *
	 * @param id identificador del documento
	 * @throws RecursoNoEncontradoException si no existe
	 */
	public void eliminar(String id) {
		seguimientoRepository.delete(buscar(id));
	}

	/**
	 * Verifica el lote en PostgreSQL y calcula su edad en la fecha dada. El día de
	 * ingreso es el día 1.
	 *
	 * @param codigoLote código del lote
	 * @param fecha      día del seguimiento
	 * @return edad del lote en días
	 * @throws ReglaNegocioException si la fecha es anterior al ingreso del lote
	 */
	private int calcularEdad(String codigoLote, LocalDate fecha) {
		LocalDate fechaIngreso = verificadorLote.obtenerFechaIngresoLoteActivo(codigoLote);
		if (fecha.isBefore(fechaIngreso)) {
			throw new ReglaNegocioException("La fecha del seguimiento (" + fecha
					+ ") es anterior al ingreso del lote " + codigoLote + " (" + fechaIngreso + ").");
		}
		return (int) ChronoUnit.DAYS.between(fechaIngreso, fecha) + 1;
	}

	/**
	 * Busca un seguimiento por su identificador.
	 *
	 * @param id identificador del documento
	 * @return el seguimiento
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private SeguimientoCrecimiento buscar(String id) {
		return seguimientoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el seguimiento con id " + id + "."));
	}

	/**
	 * Convierte el documento en DTO.
	 *
	 * @param seguimiento documento
	 * @return DTO del seguimiento
	 */
	private SeguimientoCrecimientoDTO aDTO(SeguimientoCrecimiento seguimiento) {
		return modelMapper.map(seguimiento, SeguimientoCrecimientoDTO.class);
	}
}
