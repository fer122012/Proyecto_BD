package cr.ac.una.dbamonitor.respaldo.dto;

import java.time.LocalDateTime;

/**
 * Un respaldo del historial con su tipo en texto y su estado: EXITOSO, DANADO o INCOMPLETO.
 * {@code detalleEstado} explica el estado en español.
 */
public record RespaldoDto(
        long idRespaldo,
        String tipo,
        String tipoDescripcion,
        LocalDateTime inicio,
        LocalDateTime fin,
        long duracionSegundos,
        double tamanoMb,
        String modeloRecuperacion,
        boolean esCopia,
        boolean conChecksum,
        String usuario,
        String destino,
        String estado,
        String detalleEstado) {
}
