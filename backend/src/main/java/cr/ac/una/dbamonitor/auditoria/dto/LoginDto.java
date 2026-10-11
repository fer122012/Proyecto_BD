package cr.ac.una.dbamonitor.auditoria.dto;

import java.time.LocalDateTime;

/**
 * Un login del servidor, según la consulta 01_login_registrados.sql.
 */
public record LoginDto(
        String nombre,
        String tipo,
        boolean habilitado,
        String baseDatosPredeterminada,
        LocalDateTime fechaCreacion,
        boolean esSysadmin) {
}
