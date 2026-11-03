package co.edu.unbosque.pollos_hermanos_back.entity;

/**
 * Estados de un lote. Corresponde al tipo enumerado {@code estado_lote} de
 * PostgreSQL. Las transiciones válidas las controla el trigger
 * {@code trg_lote_validar}.
 */
public enum EstadoLote {

	/** Las aves están en su ciclo de crecimiento. */
	EN_ENGORDE,

	/** Las aves alcanzaron el peso de venta. */
	LISTO_PARA_VENTA,

	/** Se vendieron todas las aves disponibles. */
	VENDIDO,

	/** El lote terminó su ciclo y ya no admite registros. */
	CERRADO;

	/**
	 * Indica si el lote está activo, es decir, si todavía ocupa su galpón y admite
	 * registros diarios.
	 *
	 * @return {@code true} en engorde o listo para venta
	 */
	public boolean esActivo() {
		return this == EN_ENGORDE || this == LISTO_PARA_VENTA;
	}
}
