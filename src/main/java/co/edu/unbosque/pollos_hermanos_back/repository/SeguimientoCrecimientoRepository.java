package co.edu.unbosque.pollos_hermanos_back.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import co.edu.unbosque.pollos_hermanos_back.document.SeguimientoCrecimiento;
import co.edu.unbosque.pollos_hermanos_back.dto.CurvaCrecimientoSemanalDTO;

/**
 * Repositorio de MongoDB del seguimiento diario del crecimiento.
 */
public interface SeguimientoCrecimientoRepository extends MongoRepository<SeguimientoCrecimiento, String> {

	/**
	 * Obtiene los seguimientos de un lote ordenados por fecha.
	 *
	 * @param codigoLote código del lote
	 * @return seguimientos del lote
	 */
	List<SeguimientoCrecimiento> findByCodigoLoteOrderByFechaAsc(String codigoLote);


	/**
	 * Calcula la curva de crecimiento por semana de vida del lote: la semana 1
	 * agrupa los días 1 a 7, la semana 2 los días 8 a 14, y así sucesivamente.
	 *
	 * @param codigoLote código del lote
	 * @return peso promedio por semana, ordenado por semana
	 */
	@Aggregation(pipeline = { "{ $match: { codigoLote: ?0, pesoPromedioGramos: { $ne: null } } }",
			"{ $group: { _id: { $ceil: { $divide: ['$edadDias', 7] } }, pesoPromedioGramos: { $avg: '$pesoPromedioGramos' }, registros: { $sum: 1 } } }",
			"{ $project: { _id: 0, semana: '$_id', pesoPromedioGramos: { $round: ['$pesoPromedioGramos', 1] }, registros: 1 } }",
			"{ $sort: { semana: 1 } }" })
	List<CurvaCrecimientoSemanalDTO> calcularCurvaSemanal(String codigoLote);
}
