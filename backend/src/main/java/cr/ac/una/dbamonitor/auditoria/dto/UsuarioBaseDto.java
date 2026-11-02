package cr.ac.una.dbamonitor.auditoria.dto;

import java.time.LocalDateTime;

/**
 * Un usuario de una base de datos, según la consulta 05_usuarios_base.sql.
 * loginAsociado es null si el usuario no tiene login (no puede conectarse).
 */
public record UsuarioBaseDto(
        String nombre,
        String tipo,
        String autenticacion,
        String loginAsociado,
        String esquemaPredeterminado,
        LocalDateTime fechaCreacion) {
}
