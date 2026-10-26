package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Fila de la consulta 02_espacio_filegroups.sql, antes de calcular el espacio disponible,
 * el porcentaje de uso y el estado.
 */
public record DatosFilegroupDto(
        String filegroup,
        boolean esPredeterminado,
        boolean soloLectura,
        double tamanoMb,
        double usadoMb,
        int archivos,
        int archivosConCrecimiento) {
}
