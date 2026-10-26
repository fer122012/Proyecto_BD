package cr.ac.una.dbamonitor.respaldo.dto;

import java.time.LocalDateTime;

/**
 * Un respaldo del historial (consulta 02_historial_respaldos.sql), sin interpretar. {@code tipo}
 * es el código de msdb: D (completo), I (diferencial) o L (log).
 */
public record DatosRespaldoDto(
        long idRespaldo,
        String tipo,
        LocalDateTime inicio,
        LocalDateTime fin,
        long duracionSegundos,
        double tamanoMb,
        String modeloRecuperacion,
        boolean esCopia,
        boolean danado,
        boolean metadatosIncompletos,
        boolean conChecksum,
        String usuario,
        String destino) {
}
