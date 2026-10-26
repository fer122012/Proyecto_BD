package cr.ac.una.dbamonitor.almacenamiento.dto;

import java.time.LocalDateTime;

/**
 * Tamaño de la base en la fecha de un respaldo completo. {@code variacionUsadoMb} es la
 * diferencia con el respaldo anterior (null en el primero).
 */
public record PuntoCrecimientoDto(
        LocalDateTime fecha,
        double tamanoDatosMb,
        double tamanoLogMb,
        double usadoMb,
        Double variacionUsadoMb) {
}
