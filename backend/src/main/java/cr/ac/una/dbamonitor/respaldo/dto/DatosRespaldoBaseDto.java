package cr.ac.una.dbamonitor.respaldo.dto;

import java.time.LocalDateTime;

/**
 * Una fila de la consulta 01_estado_respaldos_por_base.sql, sin interpretar. Las fechas y las
 * horas transcurridas son null cuando la base nunca tuvo un respaldo de ese tipo.
 */
public record DatosRespaldoBaseDto(
        String baseDatos,
        String estado,
        String modeloRecuperacion,
        boolean esSistema,
        LocalDateTime ultimoCompleto,
        LocalDateTime ultimoDiferencial,
        LocalDateTime ultimoLog,
        Double horasDesdeCompleto,
        Double horasDesdeLog,
        int totalRespaldos) {
}
