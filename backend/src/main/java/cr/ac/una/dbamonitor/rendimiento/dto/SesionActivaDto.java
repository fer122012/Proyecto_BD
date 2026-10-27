package cr.ac.una.dbamonitor.rendimiento.dto;

import java.time.LocalDateTime;

/**
 * Sesión de usuario conectada a la instancia, según la consulta 03_sesiones_activas.sql.
 * Los campos de la solicitud (estado, comando y espera) son null si la sesión no ejecuta nada.
 */
public record SesionActivaDto(
        int idSesion,
        String usuario,
        String equipo,
        String programa,
        String baseDatos,
        String estadoSesion,
        String estadoSolicitud,
        String comando,
        String tipoEspera,
        int cpuMs,
        int memoriaKb,
        LocalDateTime inicioSesion,
        LocalDateTime ultimaSolicitud) {
}
