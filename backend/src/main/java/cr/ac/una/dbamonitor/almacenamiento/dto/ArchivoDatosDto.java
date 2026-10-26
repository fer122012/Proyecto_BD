package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Un archivo físico de la base (datos o log) con su crecimiento automático ya expresado en texto
 * legible, por ejemplo "64 MB", "10 %" o "Sin crecimiento automático".
 */
public record ArchivoDatosDto(
        String nombreLogico,
        String tipo,
        String filegroup,
        String rutaFisica,
        String estado,
        double tamanoMb,
        Double tamanoMaximoMb,
        String tamanoMaximoTexto,
        String crecimientoTexto) {
}
