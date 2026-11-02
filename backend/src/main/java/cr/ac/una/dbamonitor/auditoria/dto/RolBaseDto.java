package cr.ac.una.dbamonitor.auditoria.dto;

import java.util.List;

/**
 * Un rol de una base de datos con la lista de sus miembros (usuarios u otros roles).
 * esFijo indica si es un rol predefinido de SQL Server (db_owner, db_ddladmin, etc.).
 */
public record RolBaseDto(String rol, boolean esFijo, List<String> miembros) {
}
