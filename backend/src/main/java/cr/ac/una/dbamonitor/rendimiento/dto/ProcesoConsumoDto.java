package cr.ac.una.dbamonitor.rendimiento.dto;

/**
 * Proceso (sesión) con mayor consumo de CPU o memoria, según la consulta 05_procesos_cpu_memoria.sql.
 * {@code memoriaConcedidaKb}, {@code comandoActual} y {@code consultaActual} son null si el
 * proceso no está ejecutando nada.
 */
public record ProcesoConsumoDto(
        int idProceso,
        String tipoProceso,
        String usuario,
        String programa,
        String baseDatos,
        String estado,
        long cpuMs,
        Double porcentajeCpu,
        long memoriaSesionKb,
        Long memoriaConcedidaKb,
        long lecturasLogicas,
        String comandoActual,
        String consultaActual) {
}
