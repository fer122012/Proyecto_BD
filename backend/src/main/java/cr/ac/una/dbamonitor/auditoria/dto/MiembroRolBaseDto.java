package cr.ac.una.dbamonitor.auditoria.dto;

/**
 * Una fila rol-miembro, según la consulta 06_roles_base.sql. miembro es null si el rol no tiene
 * miembros. El service agrupa estas filas por rol para construir {@link RolBaseDto}.
 */
public record MiembroRolBaseDto(String rol, boolean esFijo, String miembro) {
}
