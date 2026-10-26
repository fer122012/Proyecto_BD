package cr.ac.una.dbamonitor.respaldo;

import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoEnCursoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ejecuta las consultas del Módulo 4 (database/queries/modulo4_respaldo/) y convierte cada fila
 * en un DTO. No contiene lógica: solo acceso a datos.
 * <p>
 * El historial recibe el nombre de la base como parámetro ("?"), porque en esa consulta es un
 * valor del WHERE y no un nombre de objeto.
 */
@Repository
public class RespaldoRepository {

    private static final String CARPETA = "modulo4_respaldo/";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaEstadoPorBase;
    private final String consultaHistorial;
    private final String consultaEnCurso;

    public RespaldoRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaEstadoPorBase = cargadorConsultas.cargar(CARPETA + "01_estado_respaldos_por_base.sql");
        this.consultaHistorial = cargadorConsultas.cargar(CARPETA + "02_historial_respaldos.sql");
        this.consultaEnCurso = cargadorConsultas.cargar(CARPETA + "03_respaldos_en_curso.sql");
    }

    public List<DatosRespaldoBaseDto> consultarEstadoPorBase() {
        return jdbcTemplate.query(consultaEstadoPorBase, (fila, numeroFila) ->
                new DatosRespaldoBaseDto(
                        fila.getString("base_datos"),
                        fila.getString("estado"),
                        fila.getString("modelo_recuperacion"),
                        fila.getBoolean("es_sistema"),
                        fecha(fila, "ultimo_completo"),
                        fecha(fila, "ultimo_diferencial"),
                        fecha(fila, "ultimo_log"),
                        decimalOpcional(fila, "horas_desde_completo"),
                        decimalOpcional(fila, "horas_desde_log"),
                        fila.getInt("total_respaldos")));
    }

    /** Los últimos 50 respaldos de la base, del más reciente al más antiguo. */
    public List<DatosRespaldoDto> consultarHistorial(String baseDatos) {
        return jdbcTemplate.query(consultaHistorial, (fila, numeroFila) ->
                new DatosRespaldoDto(
                        fila.getLong("id_respaldo"),
                        fila.getString("tipo"),
                        fecha(fila, "inicio"),
                        fecha(fila, "fin"),
                        fila.getLong("duracion_segundos"),
                        fila.getDouble("tamano_mb"),
                        fila.getString("modelo_recuperacion"),
                        fila.getBoolean("es_copia"),
                        fila.getBoolean("danado"),
                        fila.getBoolean("metadatos_incompletos"),
                        fila.getBoolean("con_checksum"),
                        fila.getString("usuario"),
                        fila.getString("destino")),
                baseDatos);
    }

    public List<RespaldoEnCursoDto> consultarEnCurso() {
        return jdbcTemplate.query(consultaEnCurso, (fila, numeroFila) ->
                new RespaldoEnCursoDto(
                        fila.getInt("id_sesion"),
                        fila.getString("base_datos"),
                        fila.getString("comando"),
                        fecha(fila, "inicio"),
                        fila.getDouble("porcentaje_avance"),
                        fila.getLong("segundos_restantes")));
    }

    private static LocalDateTime fecha(ResultSet fila, String columna) throws SQLException {
        Timestamp valor = fila.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private static Double decimalOpcional(ResultSet fila, String columna) throws SQLException {
        double valor = fila.getDouble(columna);
        return fila.wasNull() ? null : valor;
    }
}
