package cr.ac.una.dbamonitor.respaldo.dto;

import java.time.LocalDateTime;

/**
 * Un respaldo o restauración que se está ejecutando en este momento
 * (consulta 03_respaldos_en_curso.sql).
 */
public record RespaldoEnCursoDto(
        int idSesion,
        String baseDatos,
        String comando,
        LocalDateTime inicio,
        double porcentajeAvance,
        long segundosRestantes) {
}
