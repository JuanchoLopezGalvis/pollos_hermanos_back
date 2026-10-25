package co.edu.unbosque.pollos_hermanos_back.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.pollos_hermanos_back.dto.DetalleBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.GastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.JornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.OtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroGastoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroJornadaBeneficioDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RegistroOtroIngresoDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.RentabilidadLoteDTO;
import co.edu.unbosque.pollos_hermanos_back.exception.RecursoNoEncontradoException;
import co.edu.unbosque.pollos_hermanos_back.repository.GastoBeneficioRepository;

/**
 * Gastos, beneficio, otros ingresos y balance de los lotes. El costo del lote
 * suma sus componentes en PostgreSQL ({@code fn_costo_lote}) y la ganancia neta
 * es ingresos menos costos; los gastos generales de la finca no se reparten
 * entre lotes (D7).
 */
@Service
public class GastoBeneficioService {

	/** Acceso a gastos y beneficio. */
	private final GastoBeneficioRepository repositorio;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio acceso a gastos y beneficio
	 */
	public GastoBeneficioService(GastoBeneficioRepository repositorio) {
		this.repositorio = repositorio;
	}

	/**
	 * Obtiene los gastos.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return gastos
	 */
	public List<GastoDTO> obtenerGastos(Long idLote) {
		return repositorio.obtenerGastos(idLote);
	}

	/**
	 * Registra un gasto.
	 *
	 * @param dto datos del gasto
	 * @return el gasto registrado
	 */
	public GastoDTO registrarGasto(RegistroGastoDTO dto) {
		return repositorio.registrarGasto(dto);
	}

	/**
	 * Obtiene las jornadas de beneficio.
	 *
	 * @param idLote lote, o {@code null} para todas
	 * @return jornadas
	 */
	public List<JornadaBeneficioDTO> obtenerJornadas(Long idLote) {
		return repositorio.obtenerJornadas(idLote);
	}

	/**
	 * Obtiene quién participó en una jornada.
	 *
	 * @param idJornada jornada
	 * @return detalle
	 * @throws RecursoNoEncontradoException si la jornada no existe
	 */
	public List<DetalleBeneficioDTO> obtenerDetalle(Long idJornada) {
		buscarJornada(idJornada);
		return repositorio.obtenerDetalle(idJornada);
	}

	/**
	 * Registra una jornada de beneficio con sus beneficiadoras.
	 *
	 * @param dto datos de la jornada
	 * @return la jornada registrada con su costo
	 */
	public JornadaBeneficioDTO registrarJornada(RegistroJornadaBeneficioDTO dto) {
		return buscarJornada(repositorio.registrarJornada(dto));
	}

	/**
	 * Obtiene los otros ingresos de los lotes.
	 *
	 * @param idLote lote, o {@code null} para todos
	 * @return ingresos
	 */
	public List<OtroIngresoDTO> obtenerOtrosIngresos(Long idLote) {
		return repositorio.obtenerOtrosIngresos(idLote);
	}

	/**
	 * Registra otro ingreso de un lote (por ejemplo, la venta de gallinaza).
	 *
	 * @param dto datos del ingreso
	 * @return el ingreso registrado
	 */
	public OtroIngresoDTO registrarOtroIngreso(RegistroOtroIngresoDTO dto) {
		return repositorio.registrarOtroIngreso(dto);
	}

	/**
	 * Obtiene el costo y la rentabilidad de todos los lotes.
	 *
	 * @return rentabilidad por lote
	 */
	public List<RentabilidadLoteDTO> obtenerRentabilidad() {
		return repositorio.obtenerRentabilidad();
	}

	/**
	 * Obtiene el costo y la rentabilidad de un lote.
	 *
	 * @param idLote lote
	 * @return su rentabilidad
	 * @throws RecursoNoEncontradoException si el lote no existe
	 */
	public RentabilidadLoteDTO obtenerRentabilidad(Long idLote) {
		return repositorio.buscarRentabilidad(idLote)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote con id " + idLote + "."));
	}

	/**
	 * Busca una jornada.
	 *
	 * @param id identificador
	 * @return la jornada
	 * @throws RecursoNoEncontradoException si no existe
	 */
	private JornadaBeneficioDTO buscarJornada(Long id) {
		return repositorio.buscarJornada(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la jornada de beneficio con id " + id + "."));
	}
}
