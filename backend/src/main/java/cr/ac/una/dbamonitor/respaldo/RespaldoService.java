package cr.ac.una.dbamonitor.respaldo;

import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.DatosRespaldoDto;
import cr.ac.una.dbamonitor.respaldo.dto.EstadoRespaldoBaseDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoDto;
import cr.ac.una.dbamonitor.respaldo.dto.RespaldoEnCursoDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica del Módulo 4: interpreta si los respaldos de cada base dejan una condición adecuada o un
 * riesgo de pérdida de datos, y traduce el tipo y las marcas de cada respaldo del historial a un
 * estado legible.
 * <p>
 * Criterios de riesgo (umbrales en las constantes de esta clase):
 * <ul>
 *   <li>CRITICO: la base nunca tuvo un respaldo completo; si se pierde, no hay desde dónde restaurarla.</li>
 *   <li>ATENCION: el último completo es más antiguo que {@link #HORAS_MAXIMAS_COMPLETO}, o la base está
 *       en modelo FULL/BULK_LOGGED y no tiene respaldos de log recientes ({@link #HORAS_MAXIMAS_LOG}).
 *       En ese modelo el log solo se libera al respaldarlo, así que además crece sin límite.</li>
 *   <li>ADECUADO: en cualquier otro caso.</li>
 * </ul>
 */
@Service
public class RespaldoService {

    static final String RIESGO_ADECUADO = "ADECUADO";
    static final String RIESGO_ATENCION = "ATENCION";
    static final String RIESGO_CRITICO = "CRITICO";

    static final String ESTADO_EXITOSO = "EXITOSO";
    static final String ESTADO_DANADO = "DANADO";
    static final String ESTADO_INCOMPLETO = "INCOMPLETO";

    /** Una semana: frecuencia mínima habitual de un respaldo completo. */
    static final double HORAS_MAXIMAS_COMPLETO = 7 * 24.0;

    /** Un día: más allá de esto, una falla perdería las transacciones de todo ese periodo. */
    static final double HORAS_MAXIMAS_LOG = 24.0;

    private final RespaldoRepository respaldoRepository;

    public RespaldoService(RespaldoRepository respaldoRepository) {
        this.respaldoRepository = respaldoRepository;
    }

    public List<EstadoRespaldoBaseDto> obtenerEstadoPorBase() {
        return respaldoRepository.consultarEstadoPorBase().stream()
                .map(RespaldoService::interpretarBase)
                .toList();
    }

    public List<RespaldoDto> obtenerHistorial(String baseDatos) {
        String base = SqlIdentificadores.validar(baseDatos, "La base de datos");
        return respaldoRepository.consultarHistorial(base).stream()
                .map(RespaldoService::describirRespaldo)
                .toList();
    }

    public List<RespaldoEnCursoDto> obtenerEnCurso() {
        return respaldoRepository.consultarEnCurso();
    }

    static EstadoRespaldoBaseDto interpretarBase(DatosRespaldoBaseDto datos) {
        String nivel;
        String interpretacion;
        boolean usaLog = !"SIMPLE".equals(datos.modeloRecuperacion());

        if (datos.ultimoCompleto() == null) {
            nivel = RIESGO_CRITICO;
            interpretacion = "Nunca se ha hecho un respaldo completo: si la base se pierde o se daña, no hay desde "
                    + "dónde restaurarla. Hacer un BACKUP DATABASE cuanto antes.";
        } else if (datos.horasDesdeCompleto() > HORAS_MAXIMAS_COMPLETO) {
            nivel = RIESGO_ATENCION;
            interpretacion = "El último respaldo completo es de hace " + antiguedad(datos.horasDesdeCompleto())
                    + ". Restaurar desde un respaldo tan antiguo exige aplicar muchos respaldos posteriores o perder "
                    + "ese periodo; se recomienda al menos un respaldo completo por semana.";
        } else if (usaLog && datos.ultimoLog() == null) {
            nivel = RIESGO_ATENCION;
            interpretacion = "La base está en modelo " + datos.modeloRecuperacion() + " pero no tiene respaldos de log: "
                    + "el log de transacciones crece sin límite y no se puede restaurar a un punto en el tiempo. "
                    + "Programar BACKUP LOG o cambiar a modelo SIMPLE si no se necesita.";
        } else if (usaLog && datos.horasDesdeLog() > HORAS_MAXIMAS_LOG) {
            nivel = RIESGO_ATENCION;
            interpretacion = "El último respaldo de log es de hace " + antiguedad(datos.horasDesdeLog())
                    + ": si la base falla ahora, se perderían las transacciones de ese periodo.";
        } else if (usaLog) {
            nivel = RIESGO_ADECUADO;
            interpretacion = "Respaldos completos y de log al día: se puede restaurar a un punto en el tiempo. La pérdida "
                    + "máxima ante una falla sería lo ocurrido desde el último respaldo de log (hace "
                    + antiguedad(datos.horasDesdeLog()) + ").";
        } else {
            nivel = RIESGO_ADECUADO;
            interpretacion = "Respaldo completo reciente (hace " + antiguedad(datos.horasDesdeCompleto()) + "). En modelo "
                    + "SIMPLE solo se puede restaurar al momento del último respaldo completo o diferencial.";
        }

        LocalDateTime ultimoRespaldo = masReciente(datos.ultimoCompleto(), datos.ultimoDiferencial(), datos.ultimoLog());
        return new EstadoRespaldoBaseDto(
                datos.baseDatos(),
                datos.estado(),
                datos.modeloRecuperacion(),
                datos.esSistema(),
                datos.ultimoCompleto(),
                datos.ultimoDiferencial(),
                datos.ultimoLog(),
                ultimoRespaldo,
                tipoDe(ultimoRespaldo, datos),
                datos.totalRespaldos(),
                nivel,
                interpretacion);
    }

    /** Solo los respaldos que terminan quedan en msdb, así que todos son exitosos salvo por estas marcas. */
    static RespaldoDto describirRespaldo(DatosRespaldoDto datos) {
        String estado;
        String detalle;
        if (datos.danado()) {
            estado = ESTADO_DANADO;
            detalle = "Se respaldó a pesar de encontrar páginas dañadas (CONTINUE_AFTER_ERROR): no es confiable para "
                    + "restaurar. Revisar la base con DBCC CHECKDB.";
        } else if (datos.metadatosIncompletos()) {
            estado = ESTADO_INCOMPLETO;
            detalle = "Respaldo de la cola del log con metadatos incompletos: se tomó con la base dañada y solo sirve "
                    + "para completar una restauración.";
        } else {
            estado = ESTADO_EXITOSO;
            detalle = datos.conChecksum()
                    ? "Terminó correctamente y verificó cada página con CHECKSUM."
                    : "Terminó correctamente, pero sin CHECKSUM: no se verificaron las páginas al respaldar.";
        }

        return new RespaldoDto(
                datos.idRespaldo(),
                datos.tipo(),
                descripcionTipo(datos.tipo()),
                datos.inicio(),
                datos.fin(),
                datos.duracionSegundos(),
                datos.tamanoMb(),
                datos.modeloRecuperacion(),
                datos.esCopia(),
                datos.conChecksum(),
                datos.usuario(),
                datos.destino(),
                estado,
                detalle);
    }

    /** Códigos de msdb.dbo.backupset.type. */
    static String descripcionTipo(String tipo) {
        return switch (tipo) {
            case "D" -> "Completo";
            case "I" -> "Diferencial";
            case "L" -> "Log";
            case "F" -> "Archivo o filegroup";
            case "G" -> "Diferencial de archivo";
            case "P" -> "Parcial";
            case "Q" -> "Diferencial parcial";
            default -> "Desconocido (" + tipo + ")";
        };
    }

    /** "menos de una hora", "5 horas" o "3 días". */
    static String antiguedad(double horas) {
        if (horas < 1) {
            return "menos de una hora";
        }
        if (horas < 48) {
            long enteras = Math.round(Math.floor(horas));
            return enteras + (enteras == 1 ? " hora" : " horas");
        }
        return Math.round(Math.floor(horas / 24)) + " días";
    }

    private static LocalDateTime masReciente(LocalDateTime... fechas) {
        LocalDateTime resultado = null;
        for (LocalDateTime fecha : fechas) {
            if (fecha != null && (resultado == null || fecha.isAfter(resultado))) {
                resultado = fecha;
            }
        }
        return resultado;
    }

    private static String tipoDe(LocalDateTime ultimoRespaldo, DatosRespaldoBaseDto datos) {
        if (ultimoRespaldo == null) {
            return null;
        }
        if (ultimoRespaldo.equals(datos.ultimoCompleto())) {
            return "Completo";
        }
        return ultimoRespaldo.equals(datos.ultimoDiferencial()) ? "Diferencial" : "Log";
    }
}
