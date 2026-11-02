package cr.ac.una.dbamonitor.auditoria;

import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.MiembroRolDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolBaseDto;
import cr.ac.una.dbamonitor.auditoria.dto.RolServidorDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaServiceTest {

    @Test
    void agrupaLosMiembrosDeCadaRolDeServidor() {
        List<RolServidorDto> roles = AuditoriaService.agruparRolesServidor(List.of(
                new MiembroRolDto("sysadmin", "sa", "SQL_LOGIN"),
                new MiembroRolDto("sysadmin", "admin", "WINDOWS_LOGIN")));
        assertEquals(1, roles.size());
        assertEquals(List.of("sa", "admin"), roles.get(0).miembros());
    }

    @Test
    void rolDeServidorSinMiembrosQuedaConListaVacia() {
        List<RolServidorDto> roles = AuditoriaService.agruparRolesServidor(List.of(
                new MiembroRolDto("dbcreator", null, null)));
        assertEquals("dbcreator", roles.get(0).rol());
        assertTrue(roles.get(0).miembros().isEmpty());
    }

    @Test
    void agrupaLosRolesDeBaseYConservaSiSonFijos() {
        List<RolBaseDto> roles = AuditoriaService.agruparRolesBase(List.of(
                new MiembroRolBaseDto("rol_ventas", false, "usr_cajero"),
                new MiembroRolBaseDto("rol_sin_miembros", false, null),
                new MiembroRolBaseDto("db_ddladmin", true, "dba_monitor")));
        assertEquals(3, roles.size());
        assertEquals(List.of("usr_cajero"), roles.get(0).miembros());
        assertFalse(roles.get(0).esFijo());
        assertTrue(roles.get(1).miembros().isEmpty());
        assertTrue(roles.get(2).esFijo());
    }
}
