package cr.ac.una.dbamonitor.instancia.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de GET /api/instancia: visión general del estado de la instancia monitoreada.
 */
public record ResumenInstanciaDto(
        String servidor,
        String nombreConexion,
        String instancia,
        String tipoGestor,
        String versionComercial,
        String version,
        String nivel,
        String edicion,
        String versionCompleta,
        String estado,
        List<BaseDatosConAlertaDto> basesConAlerta,
        LocalDateTime fechaInicio,
        long tiempoActividadSegundos,
        String tiempoActividadTexto,
        MemoriaInstanciaDto memoria,
        List<String> limitacionesEdicion) {
}
