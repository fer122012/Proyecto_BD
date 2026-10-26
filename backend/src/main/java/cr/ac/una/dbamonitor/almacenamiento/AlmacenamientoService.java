package cr.ac.una.dbamonitor.almacenamiento;

import cr.ac.una.dbamonitor.almacenamiento.dto.ArchivoDatosDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.CrecimientoBaseDatosDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.DatosArchivoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.DatosFilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.FilegroupDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.ObjetoAlmacenamientoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.PuntoCrecimientoDto;
import cr.ac.una.dbamonitor.almacenamiento.dto.TamanoBaseDatosDto;
import cr.ac.una.dbamonitor.common.SqlIdentificadores;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lógica del Módulo 3: calcula el espacio disponible y el porcentaje de uso de cada filegroup,
 * le asigna un estado con su interpretación, traduce el crecimiento automático de los archivos
 * a texto legible y resume el historial de crecimiento de la base.
 * <p>
 * El nombre de la base que elige el usuario se valida con {@link SqlIdentificadores} antes de
 * insertarlo en las consultas, para evitar inyección SQL.
 */
@Service
public class AlmacenamientoService {

    static final String ESTADO_NORMAL = "NORMAL";
    static final String ESTADO_ATENCION = "ATENCION";
    static final String ESTADO_CRITICO = "CRITICO";

    /** A partir de este porcentaje de uso un filegroup requiere atención. */
    static final double UMBRAL_ATENCION = 85.0;

    private static final double HORAS_POR_DIA = 24.0;
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    private final AlmacenamientoRepository almacenamientoRepository;

    public AlmacenamientoService(AlmacenamientoRepository almacenamientoRepository) {
        this.almacenamientoRepository = almacenamientoRepository;
    }

    public List<TamanoBaseDatosDto> obtenerTamanoBases() {
        return almacenamientoRepository.consultarTamanoBases();
    }

    public List<FilegroupDto> obtenerFilegroups(String baseDatos) {
        return almacenamientoRepository.consultarFilegroups(validarBase(baseDatos)).stream()
                .map(AlmacenamientoService::interpretarFilegroup)
                .toList();
    }

    public List<ArchivoDatosDto> obtenerArchivos(String baseDatos) {
        return almacenamientoRepository.consultarArchivos(validarBase(baseDatos)).stream()
                .map(AlmacenamientoService::describirArchivo)
                .toList();
    }

    public List<ObjetoAlmacenamientoDto> obtenerObjetosMayorTamano(String baseDatos) {
        return almacenamientoRepository.consultarObjetosMayorTamano(validarBase(baseDatos));
    }

    public CrecimientoBaseDatosDto obtenerCrecimiento(String baseDatos) {
        String base = validarBase(baseDatos);
        return resumirCrecimiento(base, almacenamientoRepository.consultarCrecimiento(base));
    }

    /**
     * NORMAL por debajo del umbral. Por encima: ATENCION si algún archivo todavía puede crecer
     * solo, y CRITICO si ninguno puede, porque al llenarse las inserciones fallan (error 1105).
     */
    static FilegroupDto interpretarFilegroup(DatosFilegroupDto datos) {
        double disponibleMb = Math.max(datos.tamanoMb() - datos.usadoMb(), 0);
        double porcentajeUso = datos.tamanoMb() > 0 ? redondear(datos.usadoMb() * 100 / datos.tamanoMb()) : 0;

        String estado;
        String interpretacion;
        if (datos.soloLectura()) {
            estado = ESTADO_NORMAL;
            interpretacion = "Filegroup de solo lectura: su contenido no cambia, así que no crece.";
        } else if (porcentajeUso < UMBRAL_ATENCION) {
            estado = ESTADO_NORMAL;
            interpretacion = "Espacio suficiente: quedan " + formatearMb(disponibleMb) + " libres.";
        } else if (datos.archivosConCrecimiento() > 0) {
            estado = ESTADO_ATENCION;
            interpretacion = "Casi lleno, pero puede crecer automáticamente. Vigilar el espacio libre en el disco "
                    + "y considerar ampliarlo de forma planificada para evitar crecimientos frecuentes.";
        } else {
            estado = ESTADO_CRITICO;
            interpretacion = "Casi lleno y ningún archivo puede crecer: cuando se llene, las inserciones fallarán "
                    + "(error 1105). Ampliar un archivo, subir su tamaño máximo o agregar otro archivo al filegroup.";
        }

        return new FilegroupDto(
                datos.filegroup(),
                datos.esPredeterminado(),
                datos.soloLectura(),
                datos.tamanoMb(),
                datos.usadoMb(),
                redondear(disponibleMb),
                porcentajeUso,
                datos.archivos(),
                datos.archivosConCrecimiento(),
                estado,
                interpretacion);
    }

    static ArchivoDatosDto describirArchivo(DatosArchivoDto datos) {
        return new ArchivoDatosDto(
                datos.nombreLogico(),
                datos.tipo(),
                datos.filegroup(),
                datos.rutaFisica(),
                datos.estado(),
                datos.tamanoMb(),
                datos.tamanoMaximoMb(),
                textoTamanoMaximo(datos),
                textoCrecimiento(datos.crecimiento(), datos.crecimientoEnPorcentaje()));
    }

    /**
     * Sin crecimiento automático el máximo es el tamaño actual, aunque SQL Server informe
     * max_size = -1 ("ilimitado"), porque ese límite solo aplica cuando el archivo puede crecer.
     */
    static String textoTamanoMaximo(DatosArchivoDto datos) {
        if (datos.crecimiento() == 0) {
            return "Tamaño fijo";
        }
        return datos.tamanoMaximoMb() == null ? "Ilimitado" : formatearMb(datos.tamanoMaximoMb());
    }

    /** El crecimiento llega en porcentaje o en páginas de 8 KB (128 páginas = 1 MB). */
    static String textoCrecimiento(int crecimiento, boolean enPorcentaje) {
        if (crecimiento == 0) {
            return "Sin crecimiento automático";
        }
        if (enPorcentaje) {
            return crecimiento + " %";
        }
        return formatearMb(crecimiento / 128.0);
    }

    /**
     * Calcula la variación de cada respaldo respecto al anterior y el crecimiento total del
     * espacio usado entre el primero y el último.
     */
    static CrecimientoBaseDatosDto resumirCrecimiento(String baseDatos, List<PuntoCrecimientoDto> respaldos) {
        List<PuntoCrecimientoDto> puntos = new ArrayList<>();
        PuntoCrecimientoDto anterior = null;
        for (PuntoCrecimientoDto respaldo : respaldos) {
            Double variacion = anterior == null ? null : redondear(respaldo.usadoMb() - anterior.usadoMb());
            puntos.add(new PuntoCrecimientoDto(
                    respaldo.fecha(), respaldo.tamanoDatosMb(), respaldo.tamanoLogMb(), respaldo.usadoMb(), variacion));
            anterior = respaldo;
        }

        if (puntos.isEmpty()) {
            return new CrecimientoBaseDatosDto(baseDatos, puntos, null, null, null, null,
                    "No hay respaldos completos registrados de esta base en msdb, así que no hay historial de tamaño. "
                            + "Cada respaldo completo (BACKUP DATABASE) agrega un punto a este historial.");
        }
        if (puntos.size() == 1) {
            return new CrecimientoBaseDatosDto(baseDatos, puntos, null, null, null, null,
                    "Solo hay un respaldo completo registrado. Se necesitan al menos dos para comparar el crecimiento.");
        }

        PuntoCrecimientoDto primero = puntos.get(0);
        PuntoCrecimientoDto ultimo = puntos.get(puntos.size() - 1);
        double crecimientoUsado = redondear(ultimo.usadoMb() - primero.usadoMb());
        double crecimientoDatos = redondear(ultimo.tamanoDatosMb() - primero.tamanoDatosMb());
        double dias = Duration.between(primero.fecha(), ultimo.fecha()).toMinutes() / 60.0 / HORAS_POR_DIA;
        Double promedioDiario = dias >= 1 ? redondear(crecimientoUsado / dias) : null;

        return new CrecimientoBaseDatosDto(
                baseDatos,
                puntos,
                crecimientoUsado,
                crecimientoDatos,
                redondear(dias),
                promedioDiario,
                interpretarCrecimiento(crecimientoUsado, promedioDiario, puntos.size()));
    }

    private static String interpretarCrecimiento(double crecimientoUsado, Double promedioDiario, int respaldos) {
        String periodo = "entre los " + respaldos + " respaldos completos registrados";
        if (crecimientoUsado < 0) {
            return "El espacio usado disminuyó " + formatearMb(-crecimientoUsado) + " " + periodo
                    + " (se eliminaron datos o se reorganizaron índices).";
        }
        if (crecimientoUsado == 0) {
            return "El espacio usado no cambió " + periodo + ".";
        }
        String texto = "El espacio usado creció " + formatearMb(crecimientoUsado) + " " + periodo;
        return promedioDiario == null
                ? texto + ". El periodo es menor a un día, así que no se calcula un promedio diario."
                : texto + ", en promedio " + formatearMb(promedioDiario) + " por día.";
    }

    private static String validarBase(String baseDatos) {
        return SqlIdentificadores.validar(baseDatos, "La base de datos");
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    /** "512 MB", "2,12 MB" o "1,5 GB" según el tamaño, con coma decimal como en la interfaz. */
    static String formatearMb(double mb) {
        if (mb >= 1024) {
            return String.format(ESPANOL, "%.1f GB", mb / 1024);
        }
        return (mb == Math.rint(mb) ? String.valueOf((long) mb) : String.format(ESPANOL, "%.2f", mb)) + " MB";
    }
}
