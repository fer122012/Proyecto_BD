package cr.ac.una.dbamonitor.rendimiento.dto;

/**
 * Sesión bloqueada por otra, con un estado según el tiempo que lleva esperando.
 */
public record SesionBloqueadaDto(
        int idSesionBloqueada,
        String usuarioBloqueado,
        String baseDatos,
        int idSesionBloqueadora,
        String usuarioBloqueador,
        String equipoBloqueador,
        String programaBloqueador,
        String tipoEspera,
        long tiempoEsperaMs,
        String recursoEsperado,
        String consultaBloqueada,
        String consultaBloqueadora,
        String estado,
        String interpretacion) {
}
