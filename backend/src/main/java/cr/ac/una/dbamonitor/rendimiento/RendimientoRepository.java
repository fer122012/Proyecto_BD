package cr.ac.una.dbamonitor.rendimiento;

import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.rendimiento.dto.ConsultaConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.DatosSesionBloqueadaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ProcesoConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.SesionActivaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.TiempoPromedioConsultaDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ejecuta las consultas del Módulo 2 (database/queries/modulo2_rendimiento/) y convierte
 * cada fila en un DTO. No contiene lógica: solo acceso a datos.
 * <p>
 * Todas las consultas leen DMVs de la instancia, así que no reciben parámetros. La única
 * variación es el orden de los procesos: por CPU (como está en el archivo) o por memoria.
 */
@Repository
public class RendimientoRepository {

    private static final String CARPETA = "modulo2_rendimiento/";

    /** Orden que trae 05_procesos_cpu_memoria.sql y el que lo reemplaza para ordenar por memoria. */
    static final String ORDEN_POR_CPU = "ORDER BY cpu_ms DESC";
    static final String ORDEN_POR_MEMORIA = "ORDER BY ISNULL(memoria_concedida_kb, 0) + memoria_sesion_kb DESC";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaTopConsumo;
    private final String consultaTiempoPromedio;
    private final String consultaSesionesActivas;
    private final String consultaSesionesBloqueadas;
    private final String consultaProcesosPorCpu;
    private final String consultaProcesosPorMemoria;

    public RendimientoRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaTopConsumo = cargadorConsultas.cargar(CARPETA + "01_top_consultas_consumo.sql");
        this.consultaTiempoPromedio = cargadorConsultas.cargar(CARPETA + "02_tiempo_promedio_consultas.sql");
        this.consultaSesionesActivas = cargadorConsultas.cargar(CARPETA + "03_sesiones_activas.sql");
        this.consultaSesionesBloqueadas = cargadorConsultas.cargar(CARPETA + "04_sesiones_bloqueadas.sql");
        this.consultaProcesosPorCpu = cargadorConsultas.cargar(CARPETA + "05_procesos_cpu_memoria.sql");
        this.consultaProcesosPorMemoria = ordenarPorMemoria(consultaProcesosPorCpu);
    }

    public List<ConsultaConsumoDto> consultarTopConsumo() {
        return jdbcTemplate.query(consultaTopConsumo, (fila, numeroFila) ->
                new ConsultaConsumoDto(
                        fila.getString("base_datos"),
                        fila.getString("consulta"),
                        fila.getLong("ejecuciones"),
                        fila.getLong("cpu_total_ms"),
                        fila.getLong("duracion_total_ms"),
                        fila.getLong("lecturas_logicas_total"),
                        fila.getLong("escrituras_logicas_total"),
                        fecha(fila, "ultima_ejecucion")));
    }

    public List<TiempoPromedioConsultaDto> consultarTiempoPromedio() {
        return jdbcTemplate.query(consultaTiempoPromedio, (fila, numeroFila) ->
                new TiempoPromedioConsultaDto(
                        fila.getString("base_datos"),
                        fila.getString("consulta"),
                        fila.getLong("ejecuciones"),
                        fila.getDouble("duracion_promedio_ms"),
                        fila.getDouble("cpu_promedio_ms"),
                        fila.getLong("lecturas_promedio"),
                        fila.getLong("duracion_min_ms"),
                        fila.getLong("duracion_max_ms")));
    }

    public List<SesionActivaDto> consultarSesionesActivas() {
        return jdbcTemplate.query(consultaSesionesActivas, (fila, numeroFila) ->
                new SesionActivaDto(
                        fila.getInt("id_sesion"),
                        fila.getString("usuario"),
                        fila.getString("equipo"),
                        fila.getString("programa"),
                        fila.getString("base_datos"),
                        fila.getString("estado_sesion"),
                        fila.getString("estado_solicitud"),
                        fila.getString("comando"),
                        fila.getString("tipo_espera"),
                        fila.getInt("cpu_ms"),
                        fila.getInt("memoria_kb"),
                        fecha(fila, "inicio_sesion"),
                        fecha(fila, "ultima_solicitud")));
    }

    public List<DatosSesionBloqueadaDto> consultarSesionesBloqueadas() {
        return jdbcTemplate.query(consultaSesionesBloqueadas, (fila, numeroFila) ->
                new DatosSesionBloqueadaDto(
                        fila.getInt("id_sesion_bloqueada"),
                        fila.getString("usuario_bloqueado"),
                        fila.getString("base_datos"),
                        fila.getInt("id_sesion_bloqueadora"),
                        fila.getString("usuario_bloqueador"),
                        fila.getString("equipo_bloqueador"),
                        fila.getString("programa_bloqueador"),
                        fila.getString("tipo_espera"),
                        fila.getLong("tiempo_espera_ms"),
                        fila.getString("recurso_esperado"),
                        fila.getString("consulta_bloqueada"),
                        fila.getString("consulta_bloqueadora")));
    }

    /** @param porMemoria true para ordenar por memoria; false para ordenar por CPU */
    public List<ProcesoConsumoDto> consultarProcesos(boolean porMemoria) {
        String consulta = porMemoria ? consultaProcesosPorMemoria : consultaProcesosPorCpu;
        return jdbcTemplate.query(consulta, (fila, numeroFila) ->
                new ProcesoConsumoDto(
                        fila.getInt("id_proceso"),
                        fila.getString("tipo_proceso"),
                        fila.getString("usuario"),
                        fila.getString("programa"),
                        fila.getString("base_datos"),
                        fila.getString("estado"),
                        fila.getLong("cpu_ms"),
                        fila.getObject("porcentaje_cpu") == null ? null : fila.getDouble("porcentaje_cpu"),
                        fila.getLong("memoria_sesion_kb"),
                        fila.getObject("memoria_concedida_kb") == null ? null : fila.getLong("memoria_concedida_kb"),
                        fila.getLong("lecturas_logicas"),
                        fila.getString("comando_actual"),
                        fila.getString("consulta_actual")));
    }

    /**
     * Cambia el ORDER BY de la consulta de procesos. Si alguien modifica el archivo y el orden
     * ya no coincide, falla al arrancar en lugar de devolver silenciosamente el orden equivocado.
     */
    static String ordenarPorMemoria(String consultaPorCpu) {
        if (!consultaPorCpu.contains(ORDEN_POR_CPU)) {
            throw new IllegalStateException(
                    "05_procesos_cpu_memoria.sql debe terminar con '" + ORDEN_POR_CPU + "' para poder ordenar por memoria.");
        }
        return consultaPorCpu.replace(ORDEN_POR_CPU, ORDEN_POR_MEMORIA);
    }

    private static LocalDateTime fecha(ResultSet fila, String columna) throws SQLException {
        Timestamp valor = fila.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }
}
