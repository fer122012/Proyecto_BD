package cr.ac.una.dbamonitor.mantenimiento;

import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import cr.ac.una.dbamonitor.mantenimiento.dto.EstadisticaTablaDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoCandidatoDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.RecalcularEstadisticasRequest;
import cr.ac.una.dbamonitor.mantenimiento.dto.RecompilarObjetoRequest;
import cr.ac.una.dbamonitor.mantenimiento.dto.ResultadoAccionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica del Módulo 6: consulta el estado de las estadísticas y de los objetos con
 * dependencias sin resolver, y ejecuta las dos acciones administrativas (recalcular
 * estadísticas y recompilar un objeto).
 * <p>
 * Antes de construir cualquier SQL dinámico, valida los identificadores que eligió el usuario
 * (base de datos, esquema, tabla u objeto) con {@link SqlIdentificadores}, para evitar
 * inyección SQL. Las acciones no dejan que un error de SQL Server se propague como un 500
 * genérico: lo capturan y devuelven un {@link ResultadoAccionDto} con exito = false y un
 * mensaje claro, tal como exige la rúbrica del Módulo 6 ("confirmación del resultado").
 */
@Service
public class MantenimientoService {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoService.class);

    private final MantenimientoRepository mantenimientoRepository;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository) {
        this.mantenimientoRepository = mantenimientoRepository;
    }

    public List<EstadisticaTablaDto> obtenerEstadisticas(String baseDatos) {
        return mantenimientoRepository.consultarEstadisticas(validarBase(baseDatos));
    }

    public List<ObjetoCandidatoDto> obtenerObjetosInvalidos(String baseDatos) {
        return mantenimientoRepository.consultarObjetosInvalidos(validarBase(baseDatos));
    }

    public ResultadoAccionDto recalcularEstadisticas(RecalcularEstadisticasRequest solicitud) {
        String base = validarBase(solicitud.baseDatos());
        String esquema = SqlIdentificadores.validar(solicitud.esquema(), "El esquema");
        String tabla = SqlIdentificadores.validar(solicitud.tabla(), "La tabla");
        String objeto = base + "." + esquema + "." + tabla;
        try {
            mantenimientoRepository.recalcularEstadisticas(base, esquema, tabla);
            log.info("Estadísticas recalculadas para {}", objeto);
            return new ResultadoAccionDto(true, "Estadísticas recalculadas correctamente.", objeto, LocalDateTime.now());
        } catch (DataAccessException ex) {
            log.warn("No se pudieron recalcular las estadísticas de {}: {}", objeto, ex.getMessage());
            return new ResultadoAccionDto(false,
                    "No se pudieron recalcular las estadísticas. Verifique que la tabla exista y que "
                            + "el usuario de la aplicación tenga el permiso ALTER sobre ella (rol db_ddladmin).",
                    objeto, LocalDateTime.now());
        }
    }

    public ResultadoAccionDto recompilarObjeto(RecompilarObjetoRequest solicitud) {
        String base = validarBase(solicitud.baseDatos());
        String esquema = SqlIdentificadores.validar(solicitud.esquema(), "El esquema");
        String nombreObjeto = SqlIdentificadores.validar(solicitud.objeto(), "El objeto");
        String objeto = base + "." + esquema + "." + nombreObjeto;
        try {
            mantenimientoRepository.recompilarObjeto(base, esquema, nombreObjeto);
            log.info("Objeto recompilado: {}", objeto);
            return new ResultadoAccionDto(true, "El objeto se marcó para recompilación correctamente.", objeto, LocalDateTime.now());
        } catch (DataAccessException ex) {
            log.warn("No se pudo recompilar {}: {}", objeto, ex.getMessage());
            return new ResultadoAccionDto(false,
                    "No se pudo recompilar el objeto. Verifique que exista y que el usuario de la "
                            + "aplicación tenga el permiso ALTER sobre él (rol db_ddladmin).",
                    objeto, LocalDateTime.now());
        }
    }

    private static String validarBase(String baseDatos) {
        return SqlIdentificadores.validar(baseDatos, "La base de datos");
    }
}
