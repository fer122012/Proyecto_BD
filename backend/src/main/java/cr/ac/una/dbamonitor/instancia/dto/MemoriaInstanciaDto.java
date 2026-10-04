package cr.ac.una.dbamonitor.instancia.dto;

/**
 * Memoria de la instancia lista para mostrar.
 * porcentajeUso = memoria en uso / memoria objetivo * 100.
 * maximaConfiguradaMb es null cuando 'max server memory' no tiene límite.
 */
public record MemoriaInstanciaDto(
        long asignadaMb,
        long objetivoMb,
        long enUsoMb,
        double porcentajeUso,
        Long maximaConfiguradaMb,
        String maximaConfiguradaTexto) {
}
