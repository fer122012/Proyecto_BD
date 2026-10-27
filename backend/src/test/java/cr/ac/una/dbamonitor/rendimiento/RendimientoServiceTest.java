package cr.ac.una.dbamonitor.rendimiento;

import cr.ac.una.dbamonitor.rendimiento.dto.DatosSesionBloqueadaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ResumenBloqueosDto;
import cr.ac.una.dbamonitor.rendimiento.dto.SesionBloqueadaDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendimientoServiceTest {

    @Test
    void sinBloqueosDevuelveMensaje() {
        ResumenBloqueosDto resumen = RendimientoService.resumirBloqueos(List.of());
        assertTrue(resumen.sesiones().isEmpty());
        assertEquals(0, resumen.sesionesBloqueadoras());
        assertEquals("No hay sesiones bloqueadas en este momento.", resumen.mensaje());
    }

    @Test
    void bloqueoBreveRequiereAtencion() {
        SesionBloqueadaDto sesion = RendimientoService.interpretarBloqueo(bloqueo(72, 69, 4581), true);
        assertEquals(RendimientoService.ESTADO_ATENCION, sesion.estado());
        assertTrue(sesion.interpretacion().startsWith("Lleva 4,6 s esperando a la sesión 69."));
    }

    @Test
    void bloqueoLargoEsCritico() {
        SesionBloqueadaDto sesion = RendimientoService.interpretarBloqueo(bloqueo(72, 69, 150_000), true);
        assertEquals(RendimientoService.ESTADO_CRITICO, sesion.estado());
        assertTrue(sesion.interpretacion().contains("2,5 min"));
        assertTrue(sesion.interpretacion().contains("KILL 69"));
    }

    @Test
    void cadenaDeBloqueosIdentificaLaSesionRaiz() {
        // 69 bloquea a 72, y 72 bloquea a 80: la raíz es 69.
        ResumenBloqueosDto resumen = RendimientoService.resumirBloqueos(List.of(
                bloqueo(72, 69, 10_000),
                bloqueo(80, 72, 5_000)));

        assertEquals(2, resumen.sesionesBloqueadoras());
        assertTrue(resumen.mensaje().contains("Sesión raíz del bloqueo: 69."));
        assertFalse(resumen.sesiones().get(0).interpretacion().contains("más arriba en la cadena"));
        assertTrue(resumen.sesiones().get(1).interpretacion().contains("más arriba en la cadena"));
    }

    @Test
    void ordenDeProcesos() {
        assertFalse(RendimientoService.esOrdenPorMemoria(null));
        assertFalse(RendimientoService.esOrdenPorMemoria("CPU"));
        assertTrue(RendimientoService.esOrdenPorMemoria(" memoria "));
        assertThrows(IllegalArgumentException.class, () -> RendimientoService.esOrdenPorMemoria("disco"));
    }

    @Test
    void consultaDeProcesosSePuedeOrdenarPorMemoria() {
        String porMemoria = RendimientoRepository.ordenarPorMemoria("SELECT 1 FROM procesos ORDER BY cpu_ms DESC;");
        assertEquals("SELECT 1 FROM procesos " + RendimientoRepository.ORDEN_POR_MEMORIA + ";", porMemoria);
        assertThrows(IllegalStateException.class, () -> RendimientoRepository.ordenarPorMemoria("SELECT 1"));
    }

    private static DatosSesionBloqueadaDto bloqueo(int bloqueada, int bloqueadora, long esperaMs) {
        return new DatosSesionBloqueadaDto(bloqueada, "usuario", "Practica", bloqueadora, "usuario", "EQUIPO",
                "SQLCMD", "LCK_M_S", esperaMs, "KEY: 2:1 (abc)", "SELECT 1", "UPDATE t SET c = 1");
    }
}
