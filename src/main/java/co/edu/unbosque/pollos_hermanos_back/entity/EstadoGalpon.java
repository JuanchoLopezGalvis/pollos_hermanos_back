package co.edu.unbosque.pollos_hermanos_back.entity;

/**
 * Estados de un galpón. Corresponde al tipo enumerado {@code estado_galpon} de
 * PostgreSQL.
 * <p>
 * {@link #OCUPADO} no se asigna a mano: lo pone el trigger de {@code lote} al
 * recibir el primer lote activo y lo quita cuando sale el último.
 */
public enum EstadoGalpon {

	/** Sin lotes activos y listo para recibir lotes. */
	DISPONIBLE,

	/** Aloja al menos un lote activo; puede recibir más si hay capacidad. */
	OCUPADO,

	/** En limpieza o reparación; no recibe lotes. */
	MANTENIMIENTO
}
