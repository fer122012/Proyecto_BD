/* Módulo 3 - Gestión del Almacenamiento | Consulta: Archivos de datos y de log de la base

   Una fila por archivo físico de la base elegida:
   - nombre_logico:   nombre con el que SQL Server identifica el archivo (se usa en ALTER DATABASE).
   - tipo:            ROWS (datos, .mdf/.ndf) o LOG (log de transacciones, .ldf).
   - filegroup:       filegroup al que pertenece. NULL para el log, que no pertenece a ninguno.
   - ruta_fisica:     ubicación del archivo en el disco del servidor.
   - estado:          ONLINE, OFFLINE, etc.
   - tamano_mb:       tamaño actual asignado en disco.
   - tamano_maximo_mb: límite de crecimiento. NULL = ilimitado (max_size = -1, o el máximo de
                      2 TB que SQL Server usa para los logs "ilimitados").
   - crecimiento:     cuánto crece el archivo cuando se llena (autogrowth). 0 = no crece.
   - crecimiento_en_porcentaje: 1 si "crecimiento" es un porcentaje, 0 si son páginas de 8 KB
                      (el backend lo convierte a MB).

   Fuente: {BASE}.sys.database_files + {BASE}.sys.filegroups
   Permiso: VIEW ANY DEFINITION y un usuario en la base (03_permisos_almacenamiento.sql).
   {BASE} se reemplaza igual que en 02_espacio_filegroups.sql. */
SELECT
    f.name                                                       AS nombre_logico,
    f.type_desc                                                  AS tipo,
    fg.name                                                      AS filegroup,
    f.physical_name                                              AS ruta_fisica,
    f.state_desc                                                 AS estado,
    CAST(f.size * 8 / 1024.0 AS DECIMAL(18, 2))                  AS tamano_mb,
    CAST(CASE WHEN f.max_size IN (-1, 268435456) THEN NULL
              ELSE CAST(f.max_size AS BIGINT) * 8 / 1024.0 END AS DECIMAL(18, 2)) AS tamano_maximo_mb,
    f.growth                                                     AS crecimiento,
    CAST(f.is_percent_growth AS BIT)                             AS crecimiento_en_porcentaje
FROM {BASE}.sys.database_files AS f
LEFT JOIN {BASE}.sys.filegroups AS fg ON fg.data_space_id = f.data_space_id
WHERE f.type IN (0, 1)
ORDER BY f.type, f.file_id;
