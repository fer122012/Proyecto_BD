package cr.ac.una.dbamonitor.rendimiento;

import cr.ac.una.dbamonitor.rendimiento.dto.ConsultaConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.DatosSesionBloqueadaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ProcesoConsumoDto;
import cr.ac.una.dbamonitor.rendimiento.dto.ResumenBloqueosDto;
import cr.ac.una.dbamonitor.rendimiento.dto.SesionActivaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.SesionBloqueadaDto;
import cr.ac.una.dbamonitor.rendimiento.dto.TiempoPromedioConsultaDto;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lógica del Módulo 2: clasifica cada sesión bloqueada según cuánto lleva esperando, identifica
 * la sesión que origina la cadena de bloqueos y valida el orden pedido para los procesos.
 * <p>
 * Las demás consultas se devuelven tal como llegan: ya vienen ordenadas y con las unidades
 * convertidas desde el SQL.
 */
@Service
public class RendimientoService {

    static final String ESTADO_ATENCION = "ATENCION";
    static final String ESTADO_CRITICO = "CRITICO";

    /** A partir de esta espera un bloqueo deja de ser una contención normal y pasa a ser crítico. */
    static final long UMBRAL_CRITICO_MS = 30_000;

    static final String ORDEN_CPU = "cpu";
    static final String ORDEN_MEMORIA = "memoria";

    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    private final RendimientoRepository rendimientoRepository;

    public RendimientoService(RendimientoRepository rendimientoRepository) {
        this.rendimientoRepository = rendimientoRepository;
    }

    public List<ConsultaConsumoDto> obtenerTopConsumo() {
        return rendimientoRepository.consultarTopConsumo();
    }

    public List<TiempoPromedioConsultaDto> obtenerTiempoPromedio() {
        return rendimientoRepository.consultarTiempoPromedio();
    }

    public List<SesionActivaDto> obtenerSesionesActivas() {
        return rendimientoRepository.consultarSesionesActivas();
    }

    public ResumenBloqueosDto obtenerBloqueos() {
        return resumirBloqueos(rendimientoRepository.consultarSesionesBloqueadas());
    }

    /** @param orden "cpu" o "memoria" (sin importar mayúsculas) */
    public List<ProcesoConsumoDto> obtenerProcesos(String orden) {
        return rendimientoRepository.consultarProcesos(esOrdenPorMemoria(orden));
    }

    static boolean esOrdenPorMemoria(String orden) {
        String valor = orden == null ? ORDEN_CPU : orden.trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case ORDEN_CPU -> false;
            case ORDEN_MEMORIA -> true;
            default -> throw new IllegalArgumentException(
                    "El orden debe ser '" + ORDEN_CPU + "' o '" + ORDEN_MEMORIA + "'.");
        };
    }

    /**
     * Agrega estado e interpretación a cada sesión bloqueada y arma un mensaje general.
     * La sesión raíz es la que bloquea a otras sin estar bloqueada ella misma: es la que hay
     * que revisar (o terminar) para liberar toda la cadena.
     */
    static ResumenBloqueosDto resumirBloqueos(List<DatosSesionBloqueadaDto> filas) {
        if (filas.isEmpty()) {
            return new ResumenBloqueosDto(List.of(), 0,
                    "No hay sesiones bloqueadas en este momento.");
        }

        Set<Integer> bloqueadas = new HashSet<>();
        Set<Integer> bloqueadoras = new HashSet<>();
        for (DatosSesionBloqueadaDto fila : filas) {
            bloqueadas.add(fila.idSesionBloqueada());
            bloqueadoras.add(fila.idSesionBloqueadora());
        }
        Set<Integer> raices = new HashSet<>(bloqueadoras);
        raices.removeAll(bloqueadas);

        List<SesionBloqueadaDto> sesiones = filas.stream()
                .map(fila -> interpretarBloqueo(fila, raices.contains(fila.idSesionBloqueadora())))
                .toList();

        String mensaje = filas.size() == 1
                ? "Hay 1 sesión bloqueada."
                : "Hay " + filas.size() + " sesiones bloqueadas por " + bloqueadoras.size()
                        + (bloqueadoras.size() == 1 ? " sesión." : " sesiones.");
        if (!raices.isEmpty()) {
            mensaje += " Sesión raíz del bloqueo: " + unirIds(raices)
                    + ". Revisar si tiene una transacción abierta sin confirmar.";
        }
        return new ResumenBloqueosDto(sesiones, bloqueadoras.size(), mensaje);
    }

    static SesionBloqueadaDto interpretarBloqueo(DatosSesionBloqueadaDto datos, boolean bloqueadoraEsRaiz) {
        String espera = formatearSegundos(datos.tiempoEsperaMs());
        String estado;
        String interpretacion;
        if (datos.tiempoEsperaMs() >= UMBRAL_CRITICO_MS) {
            estado = ESTADO_CRITICO;
            interpretacion = "Lleva " + espera + " esperando a la sesión " + datos.idSesionBloqueadora()
                    + ". Una espera tan larga suele indicar una transacción que no se confirmó; "
                    + "considerar terminar la sesión bloqueadora (KILL " + datos.idSesionBloqueadora() + ").";
        } else {
            estado = ESTADO_ATENCION;
            interpretacion = "Lleva " + espera + " esperando a la sesión " + datos.idSesionBloqueadora()
                    + ". Puede ser una contención breve; vigilar si la espera sigue creciendo.";
        }
        if (!bloqueadoraEsRaiz) {
            interpretacion += " La sesión " + datos.idSesionBloqueadora()
                    + " también está bloqueada: el origen está más arriba en la cadena.";
        }

        return new SesionBloqueadaDto(
                datos.idSesionBloqueada(),
                datos.usuarioBloqueado(),
                datos.baseDatos(),
                datos.idSesionBloqueadora(),
                datos.usuarioBloqueador(),
                datos.equipoBloqueador(),
                datos.programaBloqueador(),
                datos.tipoEspera(),
                datos.tiempoEsperaMs(),
                datos.recursoEsperado(),
                datos.consultaBloqueada(),
                datos.consultaBloqueadora(),
                estado,
                interpretacion);
    }

    /** "4,6 s" o "2,5 min", con coma decimal como en la interfaz. */
    static String formatearSegundos(long milisegundos) {
        double segundos = milisegundos / 1000.0;
        if (segundos >= 60) {
            return String.format(ESPANOL, "%.1f min", segundos / 60);
        }
        return String.format(ESPANOL, "%.1f s", segundos);
    }

    private static String unirIds(Set<Integer> ids) {
        return String.join(", ", ids.stream().sorted().map(String::valueOf).toList());
    }
}
