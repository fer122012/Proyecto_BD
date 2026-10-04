package cr.ac.una.dbamonitor.instancia.dto;

/**
 * Datos de identificación tal como los devuelve la consulta 01_identificacion_instancia.sql.
 * nombreInstancia es null cuando se trata de la instancia predeterminada (MSSQLSERVER).
 */
public record IdentificacionInstanciaDto(
        String nombreEquipo,
        String nombreServidor,
        String nombreInstancia,
        String versionProducto,
        String nivelProducto,
        String edicion,
        int edicionMotor,
        String versionCompleta) {
}
