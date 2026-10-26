package cr.ac.una.dbamonitor.almacenamiento;

import cr.ac.una.dbamonitor.almacenamiento.dto.DatosArchivoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.DatosFilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.ObjetoAlmacenamientoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.PuntoCrecimientoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.TamanoBaseDatosDto;
import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejecuta las consultas del Módulo 3 (database/queries/modulo3_almacenamiento/) y convierte
 * cada fila en un DTO. No contiene lógica: solo acceso a datos.
 * <p>
 * Las consultas por base de datos usan el marcador {@code {BASE}}, que se reemplaza por el nombre
 * ya validado por el service y envuelto en corchetes (igual que en el Módulo 6). La consulta de
 * crecimiento recibe el nombre como parámetro ("?") porque ahí es un valor del WHERE.
 */
@Repository
public class AlmacenamientoRepository {

    private static final String CARPETA = "modulo3_almacenamiento/";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaTamanoBases;
    private final String consultaFilegroups;
    private final String consultaArchivos;
    private final String consultaObjetos;
    private final String consultaCrecimiento;

    public AlmacenamientoRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaTamanoBases = cargadorConsultas.cargar(CARPETA + "01_tamano_bases_datos.sql");
        this.consultaFilegroups = cargadorConsultas.cargar(CARPETA + "02_espacio_filegroups.sql");
        this.consultaArchivos = cargadorConsultas.cargar(CARPETA + "03_archivos_datos.sql");
        this.consultaObjetos = cargadorConsultas.cargar(CARPETA + "04_objetos_mayor_tamano.sql");
        this.consultaCrecimiento = cargadorConsultas.cargar(CARPETA + "05_crecimiento_base_datos.sql");
    }

    public List<TamanoBaseDatosDto> consultarTamanoBases() {
        return jdbcTemplate.query(consultaTamanoBases, (fila, numeroFila) ->
                new TamanoBaseDatosDto(
                        fila.getString("nombre"),
                        fila.getString("estado"),
                        fila.getBoolean("es_sistema"),
                        fila.getDouble("tamano_datos_mb"),
                        fila.getDouble("tamano_log_mb"),
                        fila.getDouble("tamano_total_mb"),
                        fila.getInt("cantidad_archivos")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<DatosFilegroupDto> consultarFilegroups(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaFilegroups, baseDatos), (fila, numeroFila) ->
                new DatosFilegroupDto(
                        fila.getString("filegroup"),
                        fila.getBoolean("es_predeterminado"),
                        fila.getBoolean("solo_lectura"),
                        fila.getDouble("tamano_mb"),
                        fila.getDouble("usado_mb"),
                        fila.getInt("archivos"),
                        fila.getInt("archivos_con_crecimiento")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<DatosArchivoDto> consultarArchivos(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaArchivos, baseDatos), (fila, numeroFila) ->
                new DatosArchivoDto(
                        fila.getString("nombre_logico"),
                        fila.getString("tipo"),
                        fila.getString("filegroup"),
                        fila.getString("ruta_fisica"),
                        fila.getString("estado"),
                        fila.getDouble("tamano_mb"),
                        fila.getObject("tamano_maximo_mb") == null ? null : fila.getDouble("tamano_maximo_mb"),
                        fila.getInt("crecimiento"),
                        fila.getBoolean("crecimiento_en_porcentaje")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<ObjetoAlmacenamientoDto> consultarObjetosMayorTamano(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaObjetos, baseDatos), (fila, numeroFila) ->
                new ObjetoAlmacenamientoDto(
                        fila.getString("esquema"),
                        fila.getString("objeto"),
                        fila.getString("tipo_objeto"),
                        fila.getString("indice"),
                        fila.getString("tipo_indice"),
                        fila.getLong("filas"),
                        fila.getDouble("reservado_mb"),
                        fila.getDouble("usado_mb")));
    }

    /** Respaldos completos de la base, del más antiguo al más reciente (sin variación calculada). */
    public List<PuntoCrecimientoDto> consultarCrecimiento(String baseDatos) {
        return jdbcTemplate.query(consultaCrecimiento, (fila, numeroFila) ->
                new PuntoCrecimientoDto(
                        fila.getTimestamp("fecha").toLocalDateTime(),
                        fila.getDouble("tamano_datos_mb"),
                        fila.getDouble("tamano_log_mb"),
                        fila.getDouble("usado_mb"),
                        null),
                baseDatos);
    }

    private static String conBase(String consulta, String baseDatos) {
        return consulta.replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos));
    }
}
