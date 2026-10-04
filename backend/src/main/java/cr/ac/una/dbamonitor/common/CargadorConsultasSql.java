package cr.ac.una.dbamonitor.common;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lee el texto de las consultas SQL documentadas en database/queries/.
 * Maven las copia al classpath bajo sql/ (ver pom.xml), así el backend ejecuta
 * exactamente el mismo SQL que está documentado, sin duplicarlo en el código.
 */
@Component
public class CargadorConsultasSql {

    private static final String CARPETA_CONSULTAS = "sql/";

    /**
     * @param rutaRelativa ruta dentro de database/queries/, por ejemplo
     *                     "modulo1_instancia/01_identificacion_instancia.sql"
     */
    public String cargar(String rutaRelativa) {
        ClassPathResource recurso = new ClassPathResource(CARPETA_CONSULTAS + rutaRelativa);
        try (InputStream contenido = recurso.getInputStream()) {
            return StreamUtils.copyToString(contenido, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se encontró la consulta SQL '" + rutaRelativa + "' en el classpath. "
                            + "Verifique que exista en database/queries/.", ex);
        }
    }
}
