package cr.ac.una.dbamonitor.auditoria.dto;

/**
 * Un permiso de servidor otorgado o denegado directamente a un login,
 * según la consulta 03_privilegios_login.sql.
 */
public record PrivilegioLoginDto(String login, String permiso, String estado) {
}
