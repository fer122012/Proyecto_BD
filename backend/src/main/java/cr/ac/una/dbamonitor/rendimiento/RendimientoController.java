package cr.ac.una.dbamonitor.rendimiento;

import cr.ac.una.dbamonitor.rendimiento.dto.ConsultaConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ProcesoConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ResumenBloqueosDto;
import cr.ac.una.dbamonitor.rendimiento.dto.SesionActivaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.TiempoPromedioConsultaDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 2 (Monitoreo de rendimiento).
 * <p>
 * Todo se consulta para la instancia completa: las DMVs de rendimiento no dependen de la base
 * de datos, así que ninguna ruta recibe el parámetro {@code baseDatos}.
 */
@RestController
@RequestMapping("/api/rendimiento")
public class RendimientoController {

    private final RendimientoService rendimientoService;

    public RendimientoController(RendimientoService rendimientoService) {
        this.rendimientoService = rendimientoService;
    }

    /** Las 20 sentencias con mayor consumo de CPU acumulado desde que entraron en la caché. */
    @GetMapping("/consultas-consumo")
    public List<ConsultaConsumoDto> obtenerTopConsumo() {
        return rendimientoService.obtenerTopConsumo();
    }

    /** Las 20 sentencias con mayor duración promedio por ejecución. */
    @GetMapping("/tiempo-promedio")
    public List<TiempoPromedioConsultaDto> obtenerTiempoPromedio() {
        return rendimientoService.obtenerTiempoPromedio();
    }

    /** Sesiones de usuario conectadas, con lo que están ejecutando ahora. */
    @GetMapping("/sesiones")
    public List<SesionActivaDto> obtenerSesionesActivas() {
        return rendimientoService.obtenerSesionesActivas();
    }

    /** Sesiones bloqueadas, quién las bloquea y un resumen (incluye el caso sin bloqueos). */
    @GetMapping("/bloqueos")
    public ResumenBloqueosDto obtenerBloqueos() {
        return rendimientoService.obtenerBloqueos();
    }

    /** Los 20 procesos con mayor consumo, ordenados por {@code cpu} (predeterminado) o {@code memoria}. */
    @GetMapping("/procesos")
    public List<ProcesoConsumoDto> obtenerProcesos(@RequestParam(defaultValue = "cpu") String orden) {
        return rendimientoService.obtenerProcesos(orden);
    }
}
