package cr.ac.una.dbamonitor.instancia.dto;

import java.time.LocalDateTime;

/**
 * Una base de datos de la instancia, según la consulta 04_bases_de_datos.sql.
 */
public record BaseDatosDto(
        String nombre,
        String estado,
        String modeloRecuperacion,
        int nivelCompatibilidad,
        LocalDateTime fechaCreacion,
        boolean esSistema) {
}
