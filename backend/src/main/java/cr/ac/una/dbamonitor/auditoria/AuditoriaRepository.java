package cr.ac.una.dbamonitor.auditoria;

import cr.ac.una.dbamonitor.auditoria.dto.LoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolDto;
import cr.ac.una.dbamonitor.auditoria.dto.ObjetoInvalidoDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.PrivilegioLoginDto;
import cr.ac.una.dbamonitor.auditoria.dto.UsuarioBaseDto;
import cr.ac.una.dbamonitor.common.CargadorConsultasSql;
import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejecuta las consultas del Módulo 5 (database/queries/modulo5_auditoria/) y convierte
 * cada fila en un DTO. No contiene lógica: solo acceso a datos.
 * <p>
 * Las consultas por base de datos usan el marcador {@code {BASE}}, que se reemplaza por el
 * nombre ya validado por el service y envuelto en corchetes (igual que en los Módulos 3 y 6).
 */
@Repository
public class AuditoriaRepository {

    private static final String CARPETA = "modulo5_auditoria/";

    private final JdbcTemplate jdbcTemplate;
    private final String consultaLogins;
    private final String consultaRoles;
    private final String consultaPrivilegios;
    private final String consultaObjetosInvalidos;
    private final String consultaUsuariosBase;
    private final String consultaRolesBase;
    private final String consultaPrivilegiosBase;

    public AuditoriaRepository(JdbcTemplate jdbcTemplate, CargadorConsultasSql cargadorConsultas) {
        this.jdbcTemplate = jdbcTemplate;
        this.consultaLogins = cargadorConsultas.cargar(CARPETA + "01_login_registrados.sql");
        this.consultaRoles = cargadorConsultas.cargar(CARPETA + "02_roles_servidor.sql");
        this.consultaPrivilegios = cargadorConsultas.cargar(CARPETA + "03_privilegios_login.sql");
        this.consultaObjetosInvalidos = cargadorConsultas.cargar(CARPETA + "04_objetos_invalidos.sql");
        this.consultaUsuariosBase = cargadorConsultas.cargar(CARPETA + "05_usuarios_base.sql");
        this.consultaRolesBase = cargadorConsultas.cargar(CARPETA + "06_roles_base.sql");
        this.consultaPrivilegiosBase = cargadorConsultas.cargar(CARPETA + "07_privilegios_base.sql");
    }

    public List<LoginDto> consultarLogins() {
        return jdbcTemplate.query(consultaLogins, (fila, numeroFila) ->
                new LoginDto(
                        fila.getString("nombre"),
                        fila.getString("tipo"),
                        fila.getBoolean("habilitado"),
                        fila.getString("base_predeterminada"),
                        fila.getTimestamp("fecha_creacion").toLocalDateTime(),
                        fila.getBoolean("es_sysadmin")));
    }

    public List<MiembroRolDto> consultarRoles() {
        return jdbcTemplate.query(consultaRoles, (fila, numeroFila) ->
                new MiembroRolDto(
                        fila.getString("rol"),
                        fila.getString("miembro"),
                        fila.getString("tipo_miembro")));
    }

    public List<PrivilegioLoginDto> consultarPrivilegios() {
        return jdbcTemplate.query(consultaPrivilegios, (fila, numeroFila) ->
                new PrivilegioLoginDto(
                        fila.getString("login"),
                        fila.getString("permiso"),
                        fila.getString("estado")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<ObjetoInvalidoDto> consultarObjetosInvalidos(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaObjetosInvalidos, baseDatos), (fila, numeroFila) ->
                new ObjetoInvalidoDto(
                        fila.getString("esquema"),
                        fila.getString("objeto"),
                        fila.getString("tipo"),
                        fila.getString("esquema_referenciado"),
                        fila.getString("objeto_referenciado")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<UsuarioBaseDto> consultarUsuariosBase(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaUsuariosBase, baseDatos), (fila, numeroFila) ->
                new UsuarioBaseDto(
                        fila.getString("nombre"),
                        fila.getString("tipo"),
                        fila.getString("autenticacion"),
                        fila.getString("login_asociado"),
                        fila.getString("esquema_predeterminado"),
                        fila.getTimestamp("fecha_creacion").toLocalDateTime()));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<MiembroRolBaseDto> consultarRolesBase(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaRolesBase, baseDatos), (fila, numeroFila) ->
                new MiembroRolBaseDto(
                        fila.getString("rol"),
                        fila.getBoolean("es_fijo"),
                        fila.getString("miembro")));
    }

    /** @param baseDatos nombre de base de datos ya validado (sin corchetes) */
    public List<PrivilegioBaseDto> consultarPrivilegiosBase(String baseDatos) {
        return jdbcTemplate.query(conBase(consultaPrivilegiosBase, baseDatos), (fila, numeroFila) ->
                new PrivilegioBaseDto(
                        fila.getString("principal"),
                        fila.getString("tipo_principal"),
                        fila.getString("permiso"),
                        fila.getString("estado"),
                        fila.getString("tipo_recurso"),
                        fila.getString("recurso")));
    }

    private static String conBase(String consulta, String baseDatos) {
        return consulta.replace("{BASE}", SqlIdentificadores.entreCorchetes(baseDatos));
    }
}
