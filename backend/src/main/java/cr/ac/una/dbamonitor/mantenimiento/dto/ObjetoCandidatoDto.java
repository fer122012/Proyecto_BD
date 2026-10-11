package cr.ac.una.dbamonitor.mantenimiento.dto;

/**
 * Un objeto con una dependencia sin resolver, candidato a recompilar,
 * según la consulta 02_objetos_invalidos.sql.
 */
public record ObjetoCandidatoDto(
        String esquema,
        String objeto,
        String tipo,
        String esquemaReferenciado,
        String objetoReferenciado) {
}
