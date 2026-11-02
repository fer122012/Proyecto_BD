package cr.ac.una.dbamonitor.auditoria;

import cr.ac.una.dbamonitor.auditoria.dto.LoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolDto;
import cr.ac.una.dbamonitor.auditoria.dto.ObjetoInvalidoDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioLoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolServidorDto;
import cr.ac.una.dbamonitor.auditoria.dto.UsuarioBaseDto;
import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica del Módulo 5: expone los logins y los usuarios de cada base, agrupa las filas rol-miembro
 * en un rol por fila con su lista de miembros, expone los privilegios de servidor y de base, y los
 * objetos con dependencias sin resolver (el equivalente en SQL Server a los "objetos inválidos"
 * de Oracle).
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

    public List<RolServidorDto> obtenerRoles() {
        return agruparRolesServidor(auditoriaRepository.consultarRoles());
    }

    /** Un rol por fila con su lista de miembros; la lista queda vacía si el miembro llega en null. */
    static List<RolServidorDto> agruparRolesServidor(List<MiembroRolDto> filas) {
        Map<String, List<String>> miembrosPorRol = new LinkedHashMap<>();
        for (MiembroRolDto fila : filas) {
            List<String> miembros = miembrosPorRol.computeIfAbsent(fila.rol(), r -> new ArrayList<>());
            if (fila.miembro() != null) {
                miembros.add(fila.miembro());
            }
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

    public List<ObjetoInvalidoDto> obtenerObjetosInvalidos(String baseDatos) {
        return auditoriaRepository.consultarObjetosInvalidos(validarBase(baseDatos));
    }

    public List<UsuarioBaseDto> obtenerUsuariosBase(String baseDatos) {
        return auditoriaRepository.consultarUsuariosBase(validarBase(baseDatos));
    }

    public List<RolBaseDto> obtenerRolesBase(String baseDatos) {
        return agruparRolesBase(auditoriaRepository.consultarRolesBase(validarBase(baseDatos)));
    }

    public List<PrivilegioBaseDto> obtenerPrivilegiosBase(String baseDatos) {
        return auditoriaRepository.consultarPrivilegiosBase(validarBase(baseDatos));
    }

    /** Un rol por fila con su lista de miembros; la lista queda vacía si el miembro llega en null. */
    static List<RolBaseDto> agruparRolesBase(List<MiembroRolBaseDto> filas) {
        Map<String, RolBaseDto> rolesPorNombre = new LinkedHashMap<>();
        for (MiembroRolBaseDto fila : filas) {
            RolBaseDto rol = rolesPorNombre.computeIfAbsent(fila.rol(),
                    nombre -> new RolBaseDto(nombre, fila.esFijo(), new ArrayList<>()));
            if (fila.miembro() != null) {
                rol.miembros().add(fila.miembro());
            }
        }
        return new ArrayList<>(rolesPorNombre.values());
    }

    /** El nombre se valida antes de insertarlo en la consulta, para evitar inyección SQL. */
    private static String validarBase(String baseDatos) {
        return SqlIdentificadores.validar(baseDatos, "La base de datos");
    }
}
