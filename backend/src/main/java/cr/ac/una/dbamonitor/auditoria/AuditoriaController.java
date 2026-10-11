package cr.ac.una.dbamonitor.auditoria;

import cr.ac.una.dbamonitor.auditoria.dto.LoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.ObjetoInvalidoDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioLoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolServidorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la API del Módulo 5 (Auditoría).
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /** Logins registrados en el servidor (SQL y Windows), con su estado y base predeterminada. */
    @GetMapping("/logins")
    public List<LoginDto> obtenerLogins() {
        return auditoriaService.obtenerLogins();
    }

    /** Roles fijos de servidor con sus miembros. */
    @GetMapping("/roles")
    public List<RolServidorDto> obtenerRoles() {
        return auditoriaService.obtenerRoles();
    }

    /** Permisos de servidor otorgados o denegados directamente a cada login. */
    @GetMapping("/privilegios")
    public List<PrivilegioLoginDto> obtenerPrivilegios() {
        return auditoriaService.obtenerPrivilegios();
    }

    /** Objetos (vistas, procedimientos, funciones) con dependencias que no se pudieron resolver. */
    @GetMapping("/objetos-invalidos")
    public List<ObjetoInvalidoDto> obtenerObjetosInvalidos() {
        return auditoriaService.obtenerObjetosInvalidos();
    }
}
