package cr.ac.una.dbamonitor.instancia;

import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.instancia.dto.ActividadInstanciaDto;
import cr.ac.una.dbamonitor.instancia.dto.BaseDatosDto;
import cr.ac.una.dbamonitor.instancia.dto.DatosMemoriaDto;
import cr.ac.una.dbamonitor.instancia.dto.IdentificacionInstanciaDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejecuta las consultas del Módulo 1 (database/queries/modulo1_instancia/) y convierte
 * cada fila en un DTO. No contiene lógica: solo acceso a datos.
 */
@Repository
public class InstanciaRepository {

    private static final String CARPETA = "modulo1_instancia/";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaIdentificacion;
    private final String consultaInicioYActividad;
    private final String consultaMemoria;
    private final String consultaBasesDeDatos;

    public InstanciaRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaIdentificacion = cargadorConsultas.cargar(CARPETA + "01_identificacion_instancia.sql");
        this.consultaInicioYActividad = cargadorConsultas.cargar(CARPETA + "02_inicio_y_actividad.sql");
        this.consultaMemoria = cargadorConsultas.cargar(CARPETA + "03_memoria_instancia.sql");
        this.consultaBasesDeDatos = cargadorConsultas.cargar(CARPETA + "04_bases_de_datos.sql");
    }

    public IdentificacionInstanciaDto consultarIdentificacion() {
        return jdbcTemplate.queryForObject(consultaIdentificacion, (fila, numeroFila) ->
                new IdentificacionInstanciaDto(
                        fila.getString("nombre_equipo"),
                        fila.getString("nombre_servidor"),
                        fila.getString("nombre_instancia"),
                        fila.getString("version_producto"),
                        fila.getString("nivel_producto"),
                        fila.getString("edicion"),
                        fila.getInt("edicion_motor"),
                        fila.getString("version_completa")));
    }

    public ActividadInstanciaDto consultarInicioYActividad() {
        return jdbcTemplate.queryForObject(consultaInicioYActividad, (fila, numeroFila) ->
                new ActividadInstanciaDto(
                        fila.getTimestamp("fecha_inicio").toLocalDateTime(),
                        fila.getLong("segundos_activa")));
    }

    public DatosMemoriaDto consultarMemoria() {
        return jdbcTemplate.queryForObject(consultaMemoria, (fila, numeroFila) ->
                new DatosMemoriaDto(
                        fila.getLong("memoria_asignada_mb"),
                        fila.getLong("memoria_objetivo_mb"),
                        fila.getLong("memoria_en_uso_mb"),
                        fila.getLong("memoria_maxima_configurada_mb")));
    }

    public List<BaseDatosDto> consultarBasesDeDatos() {
        return jdbcTemplate.query(consultaBasesDeDatos, (fila, numeroFila) ->
                new BaseDatosDto(
                        fila.getString("nombre"),
                        fila.getString("estado"),
                        fila.getString("modelo_recuperacion"),
                        fila.getInt("nivel_compatibilidad"),
                        fila.getTimestamp("fecha_creacion").toLocalDateTime(),
                        fila.getBoolean("es_sistema")));
    }
}
