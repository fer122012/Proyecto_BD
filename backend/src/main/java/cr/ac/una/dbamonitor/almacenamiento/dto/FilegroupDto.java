package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Espacio de un filegroup con su interpretación: {@code estado} es NORMAL, ATENCION o CRITICO,
 * y {@code interpretacion} explica en español qué significa para el DBA.
 */
public record FilegroupDto(
        String filegroup,
        boolean esPredeterminado,
        boolean soloLectura,
        double tamanoMb,
        double usadoMb,
        double disponibleMb,
        double porcentajeUso,
        int archivos,
        int archivosConCrecimiento,
        String estado,
        String interpretacion) {
}
