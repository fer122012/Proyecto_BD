package cr.ac.una.dbamonitor.instancia;

import cr.ac.una.dbamonitor.instancia.dto.ActividadInstanciaDto;
import cr.ac.una.dbamonitor.instancia.dto.BaseDatosConAlertaDto;
import cr.ac.una.dbamonitor.instancia.dto.BaseDatosDto;
import cr.ac.una.dbamonitor.instancia.dto.DatosMemoriaDto;
import cr.ac.una.dbamonitor.instancia.dto.IdentificacionInstanciaDto;
import cr.ac.una.dbamonitor.instancia.dto.MemoriaInstanciaDto;
import cr.ac.una.dbamonitor.instancia.dto.ResumenInstanciaDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lógica del Módulo 1: combina los datos reales de la instancia, calcula el estado,
 * el tiempo de actividad legible y el porcentaje de memoria.
 * Si alguna consulta falla, la excepción la maneja GlobalExceptionHandler.
 */
@Service
public class InstanciaService {

    static final String ESTADO_EN_LINEA = "EN_LINEA";
    static final String ESTADO_ATENCION = "ATENCION";

    private static final String TIPO_GESTOR = "SQL Server";
    private static final String ESTADO_BASE_EN_LINEA = "ONLINE";
    private static final String NOMBRE_INSTANCIA_PREDETERMINADA = "MSSQLSERVER (instancia predeterminada)";
    private static final long MEMORIA_SIN_LIMITE_MB = 2_147_483_647L;
    private static final int EDICION_MOTOR_EXPRESS = 4;

    private final InstanciaRepository instanciaRepository;

    public InstanciaService(InstanciaRepository instanciaRepository) {
        this.instanciaRepository = instanciaRepository;
    }

    public ResumenInstanciaDto obtenerResumen() {
        IdentificacionInstanciaDto identificacion = instanciaRepository.consultarIdentificacion();
        ActividadInstanciaDto actividad = instanciaRepository.consultarInicioYActividad();
        DatosMemoriaDto datosMemoria = instanciaRepository.consultarMemoria();
        List<BaseDatosConAlertaDto> basesConAlerta = buscarBasesConAlerta(instanciaRepository.consultarBasesDeDatos());

        return new ResumenInstanciaDto(
                identificacion.nombreEquipo(),
                identificacion.nombreServidor(),
                nombreInstanciaLegible(identificacion.nombreInstancia()),
                TIPO_GESTOR,
                versionComercial(identificacion.versionProducto()),
                identificacion.versionProducto(),
                identificacion.nivelProducto(),
                identificacion.edicion(),
                identificacion.versionCompleta(),
                basesConAlerta.isEmpty() ? ESTADO_EN_LINEA : ESTADO_ATENCION,
                basesConAlerta,
                actividad.fechaInicio(),
                actividad.segundosActiva(),
                formatearTiempoActividad(actividad.segundosActiva()),
                construirMemoria(datosMemoria),
                limitacionesDeEdicion(identificacion.edicionMotor()));
    }

    public List<BaseDatosDto> obtenerBasesDeDatos() {
        return instanciaRepository.consultarBasesDeDatos();
    }

    /** Toda base (de sistema o de usuario) que no esté ONLINE pone la instancia en ATENCION. */
    private List<BaseDatosConAlertaDto> buscarBasesConAlerta(List<BaseDatosDto> basesDeDatos) {
        List<BaseDatosConAlertaDto> basesConAlerta = new ArrayList<>();
        for (BaseDatosDto base : basesDeDatos) {
            if (!ESTADO_BASE_EN_LINEA.equals(base.estado())) {
                basesConAlerta.add(new BaseDatosConAlertaDto(base.nombre(), base.estado(), motivoDelEstado(base.estado())));
            }
        }
        return basesConAlerta;
    }

    static String motivoDelEstado(String estado) {
        return switch (estado) {
            case "OFFLINE" -> "La base está fuera de línea (puesta así manualmente o por un error) y no acepta conexiones.";
            case "RESTORING" -> "La base se está restaurando desde un respaldo y no acepta conexiones.";
            case "RECOVERING" -> "La base está en proceso de recuperación; estará disponible al terminar.";
            case "RECOVERY_PENDING" -> "SQL Server no pudo iniciar la recuperación (posible archivo faltante o falta de recursos).";
            case "SUSPECT" -> "La recuperación falló y la base está marcada como sospechosa; puede estar dañada.";
            case "EMERGENCY" -> "La base está en modo de emergencia: solo lectura y solo para sysadmin.";
            default -> "La base está en estado " + estado + " y no está disponible para uso normal.";
        };
    }

    private static String nombreInstanciaLegible(String nombreInstancia) {
        return nombreInstancia == null ? NOMBRE_INSTANCIA_PREDETERMINADA : nombreInstancia;
    }

    /** Traduce la versión principal del motor (por ejemplo 16.0.x) al nombre comercial. */
    static String versionComercial(String versionProducto) {
        String versionPrincipal = versionProducto.split("\\.")[0];
        return switch (versionPrincipal) {
            case "17" -> "SQL Server 2025";
            case "16" -> "SQL Server 2022";
            case "15" -> "SQL Server 2019";
            case "14" -> "SQL Server 2017";
            case "13" -> "SQL Server 2016";
            case "12" -> "SQL Server 2014";
            case "11" -> "SQL Server 2012";
            default -> "SQL Server (versión " + versionPrincipal + ")";
        };
    }

    /** Convierte segundos en un texto como "3 días, 4 h, 12 min". */
    static String formatearTiempoActividad(long segundos) {
        long dias = segundos / 86_400;
        long horas = (segundos % 86_400) / 3_600;
        long minutos = (segundos % 3_600) / 60;

        List<String> partes = new ArrayList<>();
        if (dias > 0) {
            partes.add(dias + (dias == 1 ? " día" : " días"));
        }
        if (dias > 0 || horas > 0) {
            partes.add(horas + " h");
        }
        partes.add(minutos + " min");
        return String.join(", ", partes);
    }

    static MemoriaInstanciaDto construirMemoria(DatosMemoriaDto datos) {
        double porcentajeUso = datos.objetivoMb() > 0
                ? Math.round(datos.enUsoMb() * 1000.0 / datos.objetivoMb()) / 10.0
                : 0.0;
        boolean sinLimite = datos.maximaConfiguradaMb() >= MEMORIA_SIN_LIMITE_MB;
        return new MemoriaInstanciaDto(
                datos.asignadaMb(),
                datos.objetivoMb(),
                datos.enUsoMb(),
                porcentajeUso,
                sinLimite ? null : datos.maximaConfiguradaMb(),
                sinLimite ? "Sin límite (valor predeterminado)" : datos.maximaConfiguradaMb() + " MB");
    }

    /** Limitaciones conocidas de la edición, para mostrarlas junto a los indicadores. */
    static List<String> limitacionesDeEdicion(int edicionMotor) {
        if (edicionMotor != EDICION_MOTOR_EXPRESS) {
            return List.of();
        }
        return List.of(
                "SQL Server Agent no está disponible: no se pueden programar jobs de respaldo ni de mantenimiento.",
                "El buffer pool (caché de datos) está limitado a 1410 MB por instancia.",
                "Cada base de datos está limitada a 10 GB de datos.",
                "El motor usa como máximo el menor entre 1 socket o 4 núcleos.");
    }
}
