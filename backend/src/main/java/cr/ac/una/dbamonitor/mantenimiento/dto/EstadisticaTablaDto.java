package cr.ac.una.dbamonitor.mantenimiento.dto;

import java.time.LocalDateTime;

/**
 * El estado de una estadística de una tabla, según la consulta 01_estadisticas_desactualizadas.sql.
 * {@code ultimaActualizacion} es null si la estadística nunca se calculó; en ese caso filas y
 * filasModificadas valen 0.
 */
public record EstadisticaTablaDto(
        String esquema,
        String tabla,
        String estadistica,
        LocalDateTime ultimaActualizacion,
        long filas,
        long filasModificadas) {
}
