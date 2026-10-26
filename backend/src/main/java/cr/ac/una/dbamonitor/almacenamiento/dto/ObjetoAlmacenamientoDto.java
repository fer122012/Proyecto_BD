package cr.ac.una.dbamonitor.almacenamiento.dto;

/**
 * Tabla o índice con mayor consumo de espacio, según la consulta 04_objetos_mayor_tamano.sql.
 */
public record ObjetoAlmacenamientoDto(
        String esquema,
        String objeto,
        String tipoObjeto,
        String indice,
        String tipoIndice,
        long filas,
        double reservadoMb,
        double usadoMb) {
}
