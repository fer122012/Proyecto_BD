package cr.ac.una.dbamonitor.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Convierte las excepciones de la aplicación en respuestas JSON claras y en español.
 * El detalle técnico solo se registra en el log; al cliente nunca se le envían
 * cadenas de conexión, usuarios ni contraseñas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<ApiError> manejarErrorDeConexion(CannotGetJdbcConnectionException ex,
                                                           HttpServletRequest request) {
        log.error("No se pudo conectar a SQL Server", ex);
        return construirRespuesta(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo establecer conexión con SQL Server. Verifique que la instancia esté "
                        + "en ejecución y que la configuración de conexión sea correcta.",
                request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> manejarErrorDeBaseDeDatos(DataAccessException ex,
                                                              HttpServletRequest request) {
        log.error("Error al ejecutar una consulta en SQL Server", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error al consultar SQL Server. Puede deberse a un error en la consulta "
                        + "o a que el usuario de la aplicación no tiene los permisos necesarios.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorInesperado(Exception ex, HttpServletRequest request) {
        // Las excepciones propias de Spring MVC (ruta inexistente, método no permitido, etc.)
        // ya traen su código HTTP: son errores de la solicitud, no fallas de la aplicación.
        if (ex instanceof ErrorResponse errorDeSolicitud) {
            HttpStatusCode codigo = errorDeSolicitud.getStatusCode();
            log.warn("Solicitud rechazada ({}): {} {}", codigo.value(), request.getMethod(), request.getRequestURI());
            return construirRespuesta(codigo, mensajeParaErrorDeSolicitud(codigo), request);
        }
        log.error("Error inesperado", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente de nuevo más tarde.",
                request);
    }

    private static String mensajeParaErrorDeSolicitud(HttpStatusCode codigo) {
        return switch (codigo.value()) {
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            default -> "La solicitud no es válida.";
        };
    }

    private ResponseEntity<ApiError> construirRespuesta(HttpStatusCode status, String mensaje,
                                                        HttpServletRequest request) {
        ApiError error = new ApiError(LocalDateTime.now(), status.value(), mensaje, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
