package cr.ac.una.dbamonitor.instancia.dto;

/**
 * Valores de memoria en MB tal como los devuelve la consulta 03_memoria_instancia.sql,
 * antes de que el service calcule el porcentaje de uso.
 */
public record DatosMemoriaDto(
        long asignadaMb,
        long objetivoMb,
        long enUsoMb,
        long maximaConfiguradaMb) {
}
