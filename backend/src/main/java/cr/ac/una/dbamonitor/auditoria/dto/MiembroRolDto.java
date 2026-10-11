package cr.ac.una.dbamonitor.auditoria.dto;

/**
 * Una fila rol-miembro, según la consulta 02_roles_servidor.sql.
 * El service agrupa estas filas por rol para construir {@link RolServidorDto}.
 */
public record MiembroRolDto(String rol, String miembro, String tipoMiembro) {
}
