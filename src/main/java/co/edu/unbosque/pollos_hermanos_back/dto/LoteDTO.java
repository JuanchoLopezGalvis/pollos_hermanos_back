package co.edu.unbosque.pollos_hermanos_back.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unbosque.pollos_hermanos_back.entity.EstadoLote;

/**
 * Datos de salida de un lote, leídos de la vista {@code v_lote_resumen}. Las
 * cifras derivadas (edad, mortalidad, aves vivas, ventas, consumo de alimento,
 * alerta) las calcula PostgreSQL.
 *
 * @param id                   identificador del lote
 * @param codigo               código único del lote
 * @param idGalpon             identificador del galpón
 * @param galpon               nombre del galpón
 * @param fechaIngreso         fecha en que ingresaron las aves
 * @param cantidadInicial      aves con las que ingresó el lote
 * @param costoPollito         costo unitario del pollito de un día
 * @param costoPollitos        costo total de los pollitos
 * @param fechaEstimadaVenta   fecha estimada de venta
 * @param estado               estado del lote
 * @param edadDias             edad del lote hoy; el día de ingreso es el día 1
 * @param mortalidadAcumulada  aves muertas registradas
 * @param porcentajeMortalidad mortalidad acumulada sobre las aves iniciales
 * @param avesDisponibles      aves vivas que no se han vendido
 * @param alertaMortalidad     {@code true} si la mortalidad supera el 5%
 * @param avesVendidas         aves vendidas
 * @param consumoAlimentoBultos bultos de alimento consumidos
 * @param consumoAlimentoKg    kilos de alimento consumidos
 */
public record LoteDTO(Long id, String codigo, Long idGalpon, String galpon, LocalDate fechaIngreso,
		Integer cantidadInicial, BigDecimal costoPollito, BigDecimal costoPollitos, LocalDate fechaEstimadaVenta,
		EstadoLote estado, Integer edadDias, Integer mortalidadAcumulada, BigDecimal porcentajeMortalidad,
		Integer avesDisponibles, Boolean alertaMortalidad, Integer avesVendidas, BigDecimal consumoAlimentoBultos,
		BigDecimal consumoAlimentoKg) {
}
