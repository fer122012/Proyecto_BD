package cr.ac.una.dbamonitor.almacenamiento;

import cr.ac.una.dbamonitor.almacenamiento.dto.ArchivoDatosDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.CrecimientoBaseDatosDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.FilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.ObjetoAlmacenamientoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.TamanoBaseDatosDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 3 (Gestión del almacenamiento).
 * <p>
 * El tamaño de las bases se consulta para toda la instancia. El resto de las rutas recibe el
 * parámetro {@code baseDatos}, porque los filegroups, archivos y objetos son propios de cada base
 * y la conexión del backend apunta a master.
 */
@RestController
@RequestMapping("/api/almacenamiento")
public class AlmacenamientoController {

    private final AlmacenamientoService almacenamientoService;

    public AlmacenamientoController(AlmacenamientoService almacenamientoService) {
        this.almacenamientoService = almacenamientoService;
    }

    /** Tamaño actual (datos, log y total) de cada base de la instancia, de mayor a menor. */
    @GetMapping("/bases-de-datos")
    public List<TamanoBaseDatosDto> obtenerTamanoBases() {
        return almacenamientoService.obtenerTamanoBases();
    }

    /** Espacio usado y disponible por filegroup, con su estado. */
    @GetMapping("/filegroups")
    public List<FilegroupDto> obtenerFilegroups(@RequestParam String baseDatos) {
        return almacenamientoService.obtenerFilegroups(baseDatos);
    }

    /** Archivos de datos y de log: ruta, tamaño, máximo y crecimiento automático. */
    @GetMapping("/archivos")
    public List<ArchivoDatosDto> obtenerArchivos(@RequestParam String baseDatos) {
        return almacenamientoService.obtenerArchivos(baseDatos);
    }

    /** Las 20 tablas e índices que más espacio reservan. */
    @GetMapping("/objetos")
    public List<ObjetoAlmacenamientoDto> obtenerObjetosMayorTamano(@RequestParam String baseDatos) {
        return almacenamientoService.obtenerObjetosMayorTamano(baseDatos);
    }

    /** Historial de tamaño según los respaldos completos, con la variación entre ellos. */
    @GetMapping("/crecimiento")
    public CrecimientoBaseDatosDto obtenerCrecimiento(@RequestParam String baseDatos) {
        return almacenamientoService.obtenerCrecimiento(baseDatos);
    }
}
