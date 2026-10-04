package cr.ac.una.dbamonitor.instancia.dto;

import java.time.LocalDateTime;

/**
 * Inicio y tiempo de actividad tal como los devuelve la consulta 02_inicio_y_actividad.sql.
 * fechaInicio está en la hora local del servidor de SQL Server.
 */
public record ActividadInstanciaDto(LocalDateTime fechaInicio, long segundosActiva) {
}
