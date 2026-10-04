package cr.ac.una.dbamonitor.salud;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Ejecuta la consulta a SQL Server que identifica el servidor al que está conectada la aplicación.
 */
@Repository
public class SaludRepository {

    private static final String CONSULTA_DATOS_SERVIDOR = """
            SELECT @@SERVERNAME                                            AS servidor,
                   CAST(SERVERPROPERTY('Edition') AS NVARCHAR(128))        AS edicion,
                   CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(128)) AS version
            """;

    private final JdbcTemplate jdbcTemplate;

    public SaludRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> consultarDatosServidor() {
        return jdbcTemplate.queryForMap(CONSULTA_DATOS_SERVIDOR);
    }
}
