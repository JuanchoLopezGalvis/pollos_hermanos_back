package co.edu.unbosque.pollos_hermanos_back.dto;

/**
 * Resultado de {@code sp_marcar_cartera_vencida}.
 *
 * @param ventasMarcadas ventas que pasaron a vencidas
 */
public record ResultadoCarteraDTO(int ventasMarcadas) {
}
