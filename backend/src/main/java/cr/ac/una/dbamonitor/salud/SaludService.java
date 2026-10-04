package cr.ac.una.dbamonitor.salud;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Arma la respuesta de salud a partir de los datos reales del servidor.
 * Si la consulta falla, la excepción la maneja GlobalExceptionHandler.
 */
@Service
public class SaludService {

    private final SaludRepository saludRepository;

    public SaludService(SaludRepository saludRepository) {
        this.saludRepository = saludRepository;
    }

    public Map<String, Object> obtenerEstado() {
        Map<String, Object> datos = saludRepository.consultarDatosServidor();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("servidor", datos.get("servidor"));
        respuesta.put("edicion", datos.get("edicion"));
        respuesta.put("version", datos.get("version"));
        respuesta.put("estado", "CONECTADO");
        return respuesta;
    }
}
