package cr.ac.una.dbamonitor.common;

import java.util.regex.Pattern;

/**
 * Valida y escapa identificadores (nombre de base de datos, esquema, tabla u objeto) que el
 * usuario elige y que el backend debe insertar en SQL dinámico. Esto es necesario para las
 * acciones administrativas del Módulo 6 (recalcular estadísticas, recompilar un objeto), porque
 * T-SQL no permite parametrizar nombres de objetos con parámetros de consulta ("?"), a diferencia
 * de los valores de una cláusula WHERE.
 * <p>
 * Solo se aceptan los caracteres válidos para un identificador de SQL Server sin comillas
 * (letras, dígitos, guion bajo, # y $); cualquier otro caracter se rechaza. Esto evita por
 * completo la inyección SQL en esa posición, sin necesidad de intentar "escapar" comillas.
 */
public final class SqlIdentificadores {

    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("^[A-Za-z0-9_#$]+$");

    private SqlIdentificadores() {
    }

    /**
     * @param descripcion texto en español para identificar el campo en el mensaje de error,
     *                     por ejemplo "La base de datos" o "El esquema"
     * @return el mismo nombre, ya validado
     * @throws IllegalArgumentException si el nombre es nulo o contiene caracteres no permitidos
     */
    public static String validar(String nombre, String descripcion) {
        if (nombre == null || !IDENTIFICADOR_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    descripcion + " \"" + nombre + "\" no es un identificador válido. "
                            + "Solo se permiten letras, números, guion bajo, # y $.");
        }
        return nombre;
    }

    /** Envuelve un identificador ya validado entre corchetes, igual que QUOTENAME de SQL Server. */
    public static String entreCorchetes(String identificadorValidado) {
        return "[" + identificadorValidado + "]";
    }
}
