package cr.ac.una.dbamonitor.auditoria;

import cr.ac.una.dbamonitor.auditoria.dto.LoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolDto;
import cr.ac.una.dbamonitor.auditoria.dto.ObjetoInvalidoDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioLoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolServidorDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica del Módulo 5: expone los logins, agrupa las filas rol-miembro en un rol por fila
 * con su lista de miembros, expone los privilegios de servidor y los objetos con dependencias
 * sin resolver (el equivalente en SQL Server a los "objetos inválidos" de Oracle).
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public List<LoginDto> obtenerLogins() {
        return auditoriaRepository.consultarLogins();
    }

    /** Agrupa las filas rol-miembro de la consulta en un rol por fila, con su lista de miembros. */
    public List<RolServidorDto> obtenerRoles() {
        Map<String, List<String>> miembrosPorRol = new LinkedHashMap<>();
        for (MiembroRolDto fila : auditoriaRepository.consultarRoles()) {
            miembrosPorRol.computeIfAbsent(fila.rol(), r -> new ArrayList<>()).add(fila.miembro());
        }
        List<RolServidorDto> roles = new ArrayList<>();
        for (Map.Entry<String, List<String>> entrada : miembrosPorRol.entrySet()) {
            roles.add(new RolServidorDto(entrada.getKey(), entrada.getValue()));
        }
        return roles;
    }

    public List<PrivilegioLoginDto> obtenerPrivilegios() {
        return auditoriaRepository.consultarPrivilegios();
    }

    public List<ObjetoInvalidoDto> obtenerObjetosInvalidos() {
        return auditoriaRepository.consultarObjetosInvalidos();
    }
}
