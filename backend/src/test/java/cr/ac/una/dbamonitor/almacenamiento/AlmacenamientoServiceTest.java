package cr.ac.una.dbamonitor.almacenamiento;

import cr.ac.una.dbamonitor.almacenamiento.dto.CrecimientoBaseDatosDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.DatosArchivoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.DatosFilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.FilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.PuntoCrecimientoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AlmacenamientoServiceTest {

    @Test
    void filegroupConEspacioEsNormal() {
        FilegroupDto filegroup = AlmacenamientoService.interpretarFilegroup(
                new DatosFilegroupDto("PRIMARY", true, false, 100, 40, 1, 1));
        assertEquals(AlmacenamientoService.ESTADO_NORMAL, filegroup.estado());
        assertEquals(60, filegroup.disponibleMb());
        assertEquals(40, filegroup.porcentajeUso());
    }

    @Test
    void filegroupLlenoQuePuedeCrecerRequiereAtencion() {
        FilegroupDto filegroup = AlmacenamientoService.interpretarFilegroup(
                new DatosFilegroupDto("PRIMARY", true, false, 96, 92.01, 1, 1));
        assertEquals(AlmacenamientoService.ESTADO_ATENCION, filegroup.estado());
        assertEquals(95.84, filegroup.porcentajeUso());
    }

    @Test
    void filegroupLlenoSinCrecimientoEsCritico() {
        FilegroupDto filegroup = AlmacenamientoService.interpretarFilegroup(
                new DatosFilegroupDto("Historico", false, false, 8, 7.5, 1, 0));
        assertEquals(AlmacenamientoService.ESTADO_CRITICO, filegroup.estado());
    }

    @Test
    void textoDeCrecimientoSegunUnidad() {
        assertEquals("8 MB", AlmacenamientoService.textoCrecimiento(1024, false));
        assertEquals("10 %", AlmacenamientoService.textoCrecimiento(10, true));
        assertEquals("Sin crecimiento automático", AlmacenamientoService.textoCrecimiento(0, false));
    }

    @Test
    void archivoSinCrecimientoTieneTamanoFijo() {
        assertEquals("Tamaño fijo", AlmacenamientoService.textoTamanoMaximo(
                new DatosArchivoDto("historico", "ROWS", "FG_HISTORICO", "C:\\h.ndf", "ONLINE", 8, null, 0, false)));
        assertEquals("Ilimitado", AlmacenamientoService.textoTamanoMaximo(
                new DatosArchivoDto("datos", "ROWS", "PRIMARY", "C:\\d.mdf", "ONLINE", 16, null, 2048, false)));
    }

    @Test
    void crecimientoSinRespaldosNoTieneTotales() {
        CrecimientoBaseDatosDto crecimiento = AlmacenamientoService.resumirCrecimiento("Practica", List.of());
        assertNull(crecimiento.crecimientoUsadoMb());
        assertEquals(0, crecimiento.puntos().size());
    }

    @Test
    void crecimientoCalculaVariacionYPromedioDiario() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 8, 0);
        CrecimientoBaseDatosDto crecimiento = AlmacenamientoService.resumirCrecimiento("Practica", List.of(
                new PuntoCrecimientoDto(inicio, 100, 10, 50, null),
                new PuntoCrecimientoDto(inicio.plusDays(5), 100, 10, 60, null),
                new PuntoCrecimientoDto(inicio.plusDays(10), 200, 10, 80, null)));

        assertNull(crecimiento.puntos().get(0).variacionUsadoMb());
        assertEquals(10, crecimiento.puntos().get(1).variacionUsadoMb());
        assertEquals(20, crecimiento.puntos().get(2).variacionUsadoMb());
        assertEquals(30, crecimiento.crecimientoUsadoMb());
        assertEquals(100, crecimiento.crecimientoDatosMb());
        assertEquals(10, crecimiento.diasObservados());
        assertEquals(3, crecimiento.promedioDiarioMb());
    }
}
