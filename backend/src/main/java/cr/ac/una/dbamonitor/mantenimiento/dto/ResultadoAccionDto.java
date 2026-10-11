package cr.ac.una.dbamonitor.mantenimiento.dto;

import java.time.LocalDateTime;

/**
 * Resultado de ejecutar una acción de mantenimiento (recalcular estadísticas o recompilar un
 * objeto). exito indica si la operación terminó correctamente; mensaje explica el resultado
 * en español, tanto en éxito como en error, para que el usuario siempre reciba confirmación.
 */
public record ResultadoAccionDto(
        boolean exito,
        String mensaje,
        String objeto,
        LocalDateTime fecha) {
}
