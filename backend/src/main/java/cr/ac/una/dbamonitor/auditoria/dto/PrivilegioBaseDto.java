package cr.ac.una.dbamonitor.auditoria.dto;

/**
 * Un permiso otorgado o denegado a un usuario o rol de una base de datos, según la consulta
 * 07_privilegios_base.sql. recurso es null si el permiso aplica a toda la base.
 */
public record PrivilegioBaseDto(
        String principal,
        String tipoPrincipal,
        String permiso,
        String estado,
        String tipoRecurso,
        String recurso) {
}
