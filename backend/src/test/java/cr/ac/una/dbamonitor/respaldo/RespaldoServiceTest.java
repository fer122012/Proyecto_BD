package cr.ac.una.dbamonitor.respaldo;

import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoDto;
import cr.ac.una.dbamonitor.respaldo.dto.EstadoRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RespaldoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 26, 10, 0);

    private static DatosRespaldoBaseDto base(String modelo, Double horasCompleto, Double horasLog) {
        return new DatosRespaldoBaseDto("Practica", "ONLINE", modelo, false,
                horasCompleto == null ? null : AHORA.minusMinutes((long) (horasCompleto * 60)),
                null,
                horasLog == null ? null : AHORA.minusMinutes((long) (horasLog * 60)),
                horasCompleto, horasLog, 3);
    }

    @Test
    void baseSinRespaldoCompletoEsCritica() {
        EstadoRespaldoBaseDto estado = RespaldoService.interpretarBase(base("SIMPLE", null, null));
        assertEquals(RespaldoService.RIESGO_CRITICO, estado.nivelRiesgo());
        assertNull(estado.ultimoRespaldo());
        assertNull(estado.tipoUltimoRespaldo());
    }

    @Test
    void respaldoCompletoAntiguoRequiereAtencion() {
        EstadoRespaldoBaseDto estado = RespaldoService.interpretarBase(base("SIMPLE", 200.0, null));
        assertEquals(RespaldoService.RIESGO_ATENCION, estado.nivelRiesgo());
    }

    @Test
    void modeloFullSinRespaldosDeLogRequiereAtencion() {
        EstadoRespaldoBaseDto estado = RespaldoService.interpretarBase(base("FULL", 2.0, null));
        assertEquals(RespaldoService.RIESGO_ATENCION, estado.nivelRiesgo());
    }

    @Test
    void logAntiguoEnModeloFullRequiereAtencion() {
        EstadoRespaldoBaseDto estado = RespaldoService.interpretarBase(base("FULL", 2.0, 30.0));
        assertEquals(RespaldoService.RIESGO_ATENCION, estado.nivelRiesgo());
    }

    @Test
    void respaldosAlDiaSonAdecuados() {
        EstadoRespaldoBaseDto full = RespaldoService.interpretarBase(base("FULL", 20.0, 0.5));
        assertEquals(RespaldoService.RIESGO_ADECUADO, full.nivelRiesgo());
        assertEquals("Log", full.tipoUltimoRespaldo());

        EstadoRespaldoBaseDto simple = RespaldoService.interpretarBase(base("SIMPLE", 20.0, null));
        assertEquals(RespaldoService.RIESGO_ADECUADO, simple.nivelRiesgo());
        assertEquals("Completo", simple.tipoUltimoRespaldo());
    }

    @Test
    void estadoDelRespaldoSegunSusMarcas() {
        assertEquals(RespaldoService.ESTADO_EXITOSO, describir(false, false).estado());
        assertEquals(RespaldoService.ESTADO_DANADO, describir(true, false).estado());
        assertEquals(RespaldoService.ESTADO_INCOMPLETO, describir(false, true).estado());
        assertEquals("Diferencial", describir(false, false).tipoDescripcion());
    }

    @Test
    void antiguedadLegible() {
        assertEquals("menos de una hora", RespaldoService.antiguedad(0.4));
        assertEquals("1 hora", RespaldoService.antiguedad(1.5));
        assertEquals("30 horas", RespaldoService.antiguedad(30));
        assertEquals("8 días", RespaldoService.antiguedad(200));
    }

    private static RespaldoDto describir(boolean danado, boolean incompleto) {
        return RespaldoService.describirRespaldo(new DatosRespaldoDto(1, "I", AHORA, AHORA, 3, 16.5, "FULL",
                false, danado, incompleto, true, "usuario", "C:\\Respaldos\\practica.bak"));
    }
}
