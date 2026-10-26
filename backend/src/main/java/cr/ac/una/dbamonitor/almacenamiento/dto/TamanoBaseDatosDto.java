package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Tamaño actual de una base de datos, según la consulta 01_tamano_bases_datos.sql.
 */
public record TamanoBaseDatosDto(
        String nombre,
        String estado,
        boolean esSistema,
        double tamanoDatosMb,
        double tamanoLogMb,
        double tamanoTotalMb,
        int cantidadArchivos) {
}
