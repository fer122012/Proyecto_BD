package cr.ac.una.dbamonitor.mantenimiento;

import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import cr.ac.una.dbamonitor.mantenimiento.dto.EstadisticaTablaDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoCandidatoDto;
import cr.ac.una.dbamonitor.mantenimiento.dto.ObjetoConErrorDto;
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
 * dependencias sin resolver, y ejecuta las acciones administrativas (recalcular estadísticas,
 * recompilar un objeto y validar todos los objetos de una base).
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

    /** Busca errores de compilación en las vistas, procedimientos y funciones de la base. */
    public List<ObjetoConErrorDto> validarObjetos(String baseDatos) {
        return mantenimientoRepository.validarObjetos(validarBase(baseDatos));
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
            // Recompilar no repara el objeto: hay que comprobar si todavía le falta algo.
            List<String> referenciasRotas = buscarReferenciasRotas(base, esquema, nombreObjeto);
            if (!referenciasRotas.isEmpty()) {
                log.info("Objeto recompilado, pero con referencias rotas: {} -> {}", objeto, referenciasRotas);
                return new ResultadoAccionDto(false,
                        "Se recompiló, pero el objeto sigue inválido porque no existe: "
                                + String.join(", ", referenciasRotas)
                                + ". Cree lo que falta o corrija el objeto con ALTER y vuelva a recompilar.",
                        objeto, LocalDateTime.now());
            }
            log.info("Objeto recompilado: {}", objeto);
            return new ResultadoAccionDto(true, "El objeto se recompiló y no tiene referencias rotas.", objeto, LocalDateTime.now());
        } catch (DataAccessException ex) {
            log.warn("No se pudo recompilar {}: {}", objeto, ex.getMessage());
            return new ResultadoAccionDto(false,
                    "No se pudo recompilar el objeto. SQL Server respondió: " + ex.getMostSpecificCause().getMessage(),
                    objeto, LocalDateTime.now());
        }
    }

    private List<String> buscarReferenciasRotas(String base, String esquema, String nombreObjeto) {
        return mantenimientoRepository.consultarObjetosInvalidos(base).stream()
                .filter(candidato -> esquema.equals(candidato.esquema()) && nombreObjeto.equals(candidato.objeto()))
                .map(MantenimientoService::nombreReferencia)
                .toList();
    }

    private static String nombreReferencia(ObjetoCandidatoDto candidato) {
        return candidato.esquemaReferenciado() == null
                ? candidato.objetoReferenciado()
                : candidato.esquemaReferenciado() + "." + candidato.objetoReferenciado();
    }

    private static String validarBase(String baseDatos) {
        return SqlIdentificadores.validar(baseDatos, "La base de datos");
    }
}
