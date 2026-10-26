package cr.ac.una.dbamonitor.respaldo.dto;

import java.time.LocalDateTime;

/**
 * Estado de los respaldos de una base con su interpretación: {@code nivelRiesgo} es ADECUADO,
 * ATENCION o CRITICO, e {@code interpretacion} explica en español qué significa para el DBA.
 * {@code ultimoRespaldo} y {@code tipoUltimoRespaldo} corresponden al respaldo más reciente de
 * cualquier tipo; son null si la base nunca se respaldó.
 */
public record EstadoRespaldoBaseDto(
        String baseDatos,
        String estado,
        String modeloRecuperacion,
        boolean esSistema,
        LocalDateTime ultimoCompleto,
        LocalDateTime ultimoDiferencial,
        LocalDateTime ultimoLog,
        LocalDateTime ultimoRespaldo,
        String tipoUltimoRespaldo,
        int totalRespaldos,
        String nivelRiesgo,
        String interpretacion) {
}
