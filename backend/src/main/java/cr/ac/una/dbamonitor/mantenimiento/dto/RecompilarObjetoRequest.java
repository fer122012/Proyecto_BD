package cr.ac.una.dbamonitor.mantenimiento.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/mantenimiento/objetos/recompilar.
 */
public record RecompilarObjetoRequest(
        @NotBlank String baseDatos,
        @NotBlank String esquema,
        @NotBlank String objeto) {
}
