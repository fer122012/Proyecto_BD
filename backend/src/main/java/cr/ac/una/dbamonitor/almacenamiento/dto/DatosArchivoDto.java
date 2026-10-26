package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Fila de la consulta 03_archivos_datos.sql. {@code crecimiento} viene en páginas de 8 KB o en
 * porcentaje, según {@code crecimientoEnPorcentaje}; {@code tamanoMaximoMb} es null si es ilimitado.
 */
public record DatosArchivoDto(
        String nombreLogico,
        String tipo,
        String filegroup,
        String rutaFisica,
        String estado,
        double tamanoMb,
        Double tamanoMaximoMb,
        int crecimiento,
        boolean crecimientoEnPorcentaje) {
}
