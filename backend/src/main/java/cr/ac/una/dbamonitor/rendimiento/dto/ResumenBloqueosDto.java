package cr.ac.una.dbamonitor.rendimiento.dto;

import java.util.List;

/**
 * Resultado de la revisión de bloqueos: las sesiones bloqueadas y un mensaje que resume la
 * situación, para que la interfaz no muestre una tabla vacía sin contexto.
 */
public record ResumenBloqueosDto(
        List<SesionBloqueadaDto> sesiones,
        int sesionesBloqueadoras,
        String mensaje) {
}
