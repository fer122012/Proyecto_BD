package cr.ac.una.dbamonitor.salud;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expone GET /api/salud para verificar que la API está en línea y conectada a SQL Server.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private final SaludService saludService;

    public SaludController(SaludService saludService) {
        this.saludService = saludService;
    }

    @GetMapping
    public Map<String, Object> obtenerEstado() {
        return saludService.obtenerEstado();
    }
}
