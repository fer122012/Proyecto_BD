package cr.ac.una.dbamonitor.almacenamiento.dto;

import java.util.List;

/**
 * Historial de crecimiento de una base y su resumen. Los totales son null cuando hay menos de
 * dos respaldos completos para comparar; {@code promedioDiarioMb} es null si el periodo
 * observado es menor a un día.
 */
public record CrecimientoBaseDatosDto(
        String baseDatos,
        List<PuntoCrecimientoDto> puntos,
        Double crecimientoUsadoMb,
        Double crecimientoDatosMb,
        Double diasObservados,
        Double promedioDiarioMb,
        String interpretacion) {
}
