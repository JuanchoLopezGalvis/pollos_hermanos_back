package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gasto de un lote o de la finca, leído de {@code v_gasto}.
 *
 * @param idGasto          identificador
 * @param fecha            fecha del gasto
 * @param idCategoriaGasto categoría
 * @param categoria        nombre de la categoría
 * @param idLote           lote, o {@code null} si es un gasto general de la finca
 * @param codigoLote       código del lote, o {@code null}
 * @param monto            valor
 * @param descripcion      descripción
 */
public record GastoDTO(Long idGasto, LocalDate fecha, Long idCategoriaGasto, String categoria, Long idLote,
		String codigoLote, BigDecimal monto, String descripcion) {
}
