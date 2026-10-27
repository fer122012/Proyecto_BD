package cr.ac.una.dbamonitor.rendimiento.dto;

import java.time.LocalDateTime;

/**
 * Sentencia SQL con mayor consumo de CPU, según la consulta 01_top_consultas_consumo.sql.
 * Los totales se acumulan desde que el plan entró en la caché.
 */
public record ConsultaConsumoDto(
        String baseDatos,
        String consulta,
        long ejecuciones,
        long cpuTotalMs,
        long duracionTotalMs,
        long lecturasLogicasTotal,
        long escriturasLogicasTotal,
        LocalDateTime ultimaEjecucion) {
}
