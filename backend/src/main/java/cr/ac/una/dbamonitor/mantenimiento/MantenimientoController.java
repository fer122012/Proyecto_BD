package cr.ac.una.dbamonitor.mantenimiento;

import cr.ac.una.dbamonitor.mantenimiento.dto.EstadisticaTablaDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoCandidatoDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.RecalcularEstadisticasRequest;
import cr.ac.una.dbamonitor.mantenimiento.dto.RecompilarObjetoRequest;
import cr.ac.una.dbamonitor.mantenimiento.dto.ResultadoAccionDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 6 (Mantenimiento preventivo).
 * <p>
 * Todas las rutas reciben el parámetro {@code baseDatos}: la conexión del backend apunta a
 * master (ver application-local.properties), pero las estadísticas y las dependencias que
 * consulta y modifica este módulo son propias de cada base de datos de práctica, así que hay
 * que indicar sobre cuál trabajar en cada solicitud.
 */
@RestController
@RequestMapping("/api/mantenimiento")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    public MantenimientoController(MantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    /** Estadísticas de las tablas de usuario, con las más desactualizadas primero. */
    @GetMapping("/estadisticas")
    public List<EstadisticaTablaDto> obtenerEstadisticas(@RequestParam String baseDatos) {
        return mantenimientoService.obtenerEstadisticas(baseDatos);
    }

    /** Objetos con dependencias sin resolver: candidatos a recompilar. */
    @GetMapping("/objetos-invalidos")
    public List<ObjetoCandidatoDto> obtenerObjetosInvalidos(@RequestParam String baseDatos) {
        return mantenimientoService.obtenerObjetosInvalidos(baseDatos);
    }

    /** Ejecuta UPDATE STATISTICS sobre la tabla indicada y confirma el resultado. */
    @PostMapping("/estadisticas/recalcular")
    public ResultadoAccionDto recalcularEstadisticas(@Valid @RequestBody RecalcularEstadisticasRequest solicitud) {
        return mantenimientoService.recalcularEstadisticas(solicitud);
    }

    /** Ejecuta sp_recompile sobre el objeto indicado y confirma el resultado. */
    @PostMapping("/objetos/recompilar")
    public ResultadoAccionDto recompilarObjeto(@Valid @RequestBody RecompilarObjetoRequest solicitud) {
        return mantenimientoService.recompilarObjeto(solicitud);
    }
}
