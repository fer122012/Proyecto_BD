package cr.ac.una.dbamonitor.mantenimiento.dto;

/**
 * Un objeto que no pasó la validación con sp_refreshsqlmodule, según la consulta
 * 05_validar_objetos.sql. error es el mensaje que devolvió SQL Server.
 */
public record ObjetoConErrorDto(String esquema, String objeto, String tipo, String error) {
}
