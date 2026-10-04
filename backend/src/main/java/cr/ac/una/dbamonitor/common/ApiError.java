package cr.ac.una.dbamonitor.common;

import java.time.LocalDateTime;

/**
 * Cuerpo JSON que devuelve la API cuando ocurre un error.
 */
public record ApiError(LocalDateTime timestamp, int status, String mensaje, String ruta) {
}
