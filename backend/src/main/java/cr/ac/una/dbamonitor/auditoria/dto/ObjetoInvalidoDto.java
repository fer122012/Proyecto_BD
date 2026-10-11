package cr.ac.una.dbamonitor.auditoria.dto;

/**
 * Un objeto cuya referencia a otro objeto no se pudo resolver en el catálogo (el equivalente
 * en SQL Server a un "objeto inválido" de Oracle), según la consulta 04_objetos_invalidos.sql.
 */
public record ObjetoInvalidoDto(
        String esquema,
        String objeto,
        String tipo,
        String esquemaReferenciado,
        String objetoReferenciado) {
}
