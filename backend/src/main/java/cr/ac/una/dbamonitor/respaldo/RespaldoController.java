package cr.ac.una.dbamonitor.respaldo;

import cr.ac.una.dbamonitor.respaldo.dto.EstadoRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoEnCursoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 4 (Respaldo y recuperación).
 * <p>
 * El historial de respaldos de toda la instancia está en msdb, así que la conexión a master
 * alcanza; el historial de una base solo necesita su nombre para filtrar.
 */
@RestController
@RequestMapping("/api/respaldos")
public class RespaldoController {

    private final RespaldoService respaldoService;

    public RespaldoController(RespaldoService respaldoService) {
        this.respaldoService = respaldoService;
    }

    /** Último respaldo de cada tipo por base de datos, con el nivel de riesgo y su explicación. */
    @GetMapping
    public List<EstadoRespaldoBaseDto> obtenerEstadoPorBase() {
        return respaldoService.obtenerEstadoPorBase();
    }

    /** Los últimos 50 respaldos de la base indicada, con su tipo y estado. */
    @GetMapping("/historial")
    public List<RespaldoDto> obtenerHistorial(@RequestParam String baseDatos) {
        return respaldoService.obtenerHistorial(baseDatos);
    }

    /** Respaldos y restauraciones que se están ejecutando ahora, con su avance. */
    @GetMapping("/en-curso")
    public List<RespaldoEnCursoDto> obtenerEnCurso() {
        return respaldoService.obtenerEnCurso();
    }
}
