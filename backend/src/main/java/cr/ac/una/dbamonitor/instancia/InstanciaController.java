package cr.ac.una.dbamonitor.instancia;

import cr.ac.una.dbamonitor.instancia.dto.BaseDatosDto;
import cr.ac.una.dbamonitor.instancia.dto.ResumenInstanciaDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 1 (Estado general de la instancia).
 */
@RestController
@RequestMapping("/api/instancia")
public class InstanciaController {

    private final InstanciaService instanciaService;

    public InstanciaController(InstanciaService instanciaService) {
        this.instanciaService = instanciaService;
    }

    /** Resumen: identificación, versión, estado, inicio, tiempo de actividad y memoria. */
    @GetMapping
    public ResumenInstanciaDto obtenerResumen() {
        return instanciaService.obtenerResumen();
    }

    /** Bases de datos de la instancia con su estado y modelo de recuperación. */
    @GetMapping("/bases-de-datos")
    public List<BaseDatosDto> obtenerBasesDeDatos() {
        return instanciaService.obtenerBasesDeDatos();
    }
}
