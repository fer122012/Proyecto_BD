package cr.ac.una.dbamonitor.instancia.dto;

/**
 * Base de datos que no está ONLINE, con la explicación de qué significa su estado.
 */
public record BaseDatosConAlertaDto(String nombre, String estado, String motivo) {
}
