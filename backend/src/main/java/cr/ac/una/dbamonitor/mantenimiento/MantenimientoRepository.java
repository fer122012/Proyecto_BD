package cr.ac.una.dbamonitor.mantenimiento;

import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import cr.ac.una.dbamonitor.mantenimiento.dto.EstadisticaTablaDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoCandidatoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejecuta las consultas y acciones del Módulo 6 (database/queries/modulo6_mantenimiento/).
 * <p>
 * Las consultas de lectura y las plantillas de acción se cargan desde archivos .sql con
 * marcadores como {@code {BASE}}, {@code {ESQUEMA}} o {@code {TABLA}}; este repository los
 * reemplaza por los identificadores recibidos, ENVUELTOS EN CORCHETES (igual que QUOTENAME de
 * SQL Server). Los identificadores deben llegar ya validados por {@link SqlIdentificadores}
 * (responsabilidad del service), porque T-SQL no permite enviar un nombre de objeto como
 * parámetro de consulta ("?"): la sintaxis de UPDATE STATISTICS y de las DMV con nombre de
 * base de datos exige el nombre en el propio texto del comando.
 */
@Repository
public class MantenimientoRepository {

    private static final String CARPETA = "modulo6_mantenimiento/";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaEstadisticas;
    private final String consultaObjetosInvalidos;
    private final String plantillaRecalcularEstadisticas;
    private final String plantillaRecompilarObjeto;

    public MantenimientoRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaEstadisticas = cargadorConsultas.cargar(CARPETA + "01_estadisticas_desactualizadas.sql");
        this.consultaObjetosInvalidos = cargadorConsultas.cargar(CARPETA + "02_objetos_invalidos.sql");
        this.plantillaRecalcularEstadisticas = cargadorConsultas.cargar(CARPETA + "03_recalcular_estadisticas.sql");
        this.plantillaRecompilarObjeto = cargadorConsultas.cargar(CARPETA + "04_recompilar_objeto.sql");
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<EstadisticaTablaDto> consultarEstadisticas(String baseDatos) {
        String consulta = consultaEstadisticas.replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos));
        return jdbcTemplate.query(consulta, (fila, numeroFila) ->
                new EstadisticaTablaDto(
                        fila.getString("esquema"),
                        fila.getString("tabla"),
                        fila.getString("estadistica"),
                        // NULL si SQL Server nunca calculó la estadística (ver la consulta).
                        fila.getTimestamp("ultima_actualizacion") == null
                                ? null
                                : fila.getTimestamp("ultima_actualizacion").toLocalDateTime(),
                        fila.getLong("filas"),
                        fila.getLong("filas_modificadas")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<ObjetoCandidatoDto> consultarObjetosInvalidos(String baseDatos) {
        String consulta = consultaObjetosInvalidos.replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos));
        return jdbcTemplate.query(consulta, (fila, numeroFila) ->
                new ObjetoCandidatoDto(
                        fila.getString("esquema"),
                        fila.getString("objeto"),
                        fila.getString("tipo"),
                        fila.getString("esquema_referenciado"),
                        fila.getString("objeto_referenciado")));
    }

    /** @param baseDatos, esquema, tabla ya validados (sin corchetes) */
    public void recalcularEstadisticas(String baseDatos, String esquema, String tabla) {
        String comando = plantillaRecalcularEstadisticas
                .replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos))
                .replace("{ESQUEMA}", SqlIdentificadores.entreCorchetes(esquema))
                .replace("{TABLA}", SqlIdentificadores.entreCorchetes(tabla));
        jdbcTemplate.execute(comando);
    }

    /** @param baseDatos, esquema, objeto ya validados (sin corchetes, por eso es seguro concatenarlos) */
    public void recompilarObjeto(String baseDatos, String esquema, String objeto) {
        String nombreObjeto = SqlIdentificadores.entreCorchetes(esquema) + "." + SqlIdentificadores.entreCorchetes(objeto);
        String comando = plantillaRecompilarObjeto
                .replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos))
                .replace("{OBJETO}", nombreObjeto);
        jdbcTemplate.execute(comando);
    }
}
