package cr.ac.una.dbamonitor.rendimiento.dto;

/**
 * Fila de la consulta 04_sesiones_bloqueadas.sql, tal como llega de SQL Server.
 * El service la convierte en {@link SesionBloqueadaDto} agregando su estado e interpretación.
 */
public record DatosSesionBloqueadaDto(
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
        String consultaBloqueadora) {
}
