package cr.ac.una.dbamonitor.mantenimiento;

import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoCandidatoDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.RecompilarObjetoRequest;
import cr.ac.una.dbamonitor.mantenimiento.dto.ResultadoAccionDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MantenimientoServiceTest {

    private static final RecompilarObjetoRequest SOLICITUD =
            new RecompilarObjetoRequest("Practica", "rrhh", "usp_CalcularBonos");

    private final MantenimientoRepository repositorio = mock(MantenimientoRepository.class);
    private final MantenimientoService servicio = new MantenimientoService(repositorio);

    @Test
    void recompilarConfirmaElExitoSiElObjetoQuedaSinReferenciasRotas() {
        when(repositorio.consultarObjetosInvalidos("Practica")).thenReturn(List.of(
                new ObjetoCandidatoDto("ventas", "otro_objeto", "VIEW", "ventas", "Descuento")));

        ResultadoAccionDto resultado = servicio.recompilarObjeto(SOLICITUD);

        assertTrue(resultado.exito());
    }

    @Test
    void recompilarInformaLasReferenciasQueSiguenRotas() {
        when(repositorio.consultarObjetosInvalidos("Practica")).thenReturn(List.of(
                new ObjetoCandidatoDto("rrhh", "usp_CalcularBonos", "SQL_STORED_PROCEDURE", "rrhh", "Bono")));

        ResultadoAccionDto resultado = servicio.recompilarObjeto(SOLICITUD);

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("rrhh.Bono"));
    }

    @Test
    void recompilarIncluyeElErrorDeSqlServer() {
        doThrow(new UncategorizedSQLException("recompilar", "EXEC", new SQLException("Invalid column name 'x'.")))
                .when(repositorio).recompilarObjeto("Practica", "rrhh", "usp_CalcularBonos");

        ResultadoAccionDto resultado = servicio.recompilarObjeto(SOLICITUD);

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("Invalid column name 'x'."));
    }

    @Test
    void validarObjetosRechazaUnaBaseInvalidaSinConsultar() {
        assertThrows(IllegalArgumentException.class, () -> servicio.validarObjetos("x; DROP"));
        verify(repositorio, never()).validarObjetos("x; DROP");
    }
}
