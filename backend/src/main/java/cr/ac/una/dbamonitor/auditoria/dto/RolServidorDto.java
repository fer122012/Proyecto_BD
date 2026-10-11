package cr.ac.una.dbamonitor.auditoria.dto;

import java.util.List;

/**
 * Un rol de servidor con la lista de sus miembros (logins u otros roles anidados).
 * Se construye en el service agrupando las filas de 02_roles_servidor.sql por rol.
 */
public record RolServidorDto(String rol, List<String> miembros) {
}
