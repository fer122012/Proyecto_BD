package cr.ac.una.dbamonitor.rendimiento.dto;

/**
 * Tiempo promedio de ejecución de una sentencia, según la consulta 02_tiempo_promedio_consultas.sql.
 */
public record TiempoPromedioConsultaDto(
        String baseDatos,
        String consulta,
        long ejecuciones,
        double duracionPromedioMs,
        double cpuPromedioMs,
        long lecturasPromedio,
        long duracionMinMs,
        long duracionMaxMs) {
}
