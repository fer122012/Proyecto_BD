package cr.ac.una.dbamonitor.mantenimiento.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/mantenimiento/estadisticas/recalcular.
 */
public record RecalcularEstadisticasRequest(
        @NotBlank String baseDatos,
        @NotBlank String esquema,
        @NotBlank String tabla) {
}
