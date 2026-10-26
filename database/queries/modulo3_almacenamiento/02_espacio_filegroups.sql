/* Módulo 3 - Gestión del Almacenamiento | Consulta: Espacio utilizado y disponible por filegroup

   En SQL Server los datos se guardan en filegroups (el equivalente a los tablespaces de Oracle).
   Cada filegroup agrupa uno o varios archivos de datos, y cada tabla o índice se asigna a un
   filegroup. Esta consulta muestra, por cada filegroup de datos de la base elegida:

   - tamano_mb:     espacio asignado (suma del tamaño de sus archivos).
   - usado_mb:      espacio reservado por tablas e índices (páginas asignadas a sus unidades
                    de asignación). Es el espacio que ya no está disponible para datos nuevos.
   - archivos:      cuántos archivos de datos tiene el filegroup.
   - archivos_con_crecimiento: cuántos de esos archivos todavía pueden crecer automáticamente
                    (autogrowth activo y sin haber llegado a su tamaño máximo). Si es 0 y el
                    filegroup se llena, las inserciones fallan con el error 1105.

   El espacio disponible y el porcentaje de uso los calcula el backend a partir de estas columnas.
   Solo se consideran los filegroups de filas (type = 'FG'); el log de transacciones no pertenece
   a ningún filegroup y se ve en la consulta de archivos.

   Fuente: {BASE}.sys.filegroups + {BASE}.sys.database_files + {BASE}.sys.allocation_units
   Permiso: VIEW ANY DEFINITION y un usuario en la base (03_permisos_almacenamiento.sql).

   {BASE} es la base de datos elegida por el usuario. El backend la reemplaza ya validada y
   escrita entre corchetes (ver SqlIdentificadores). Se usan nombres de tres partes porque la
   conexión del backend sigue apuntando a master. */
WITH archivos AS (
    SELECT
        data_space_id,
        SUM(CAST(size AS BIGINT)) AS paginas,
        COUNT(*)                  AS archivos,
        SUM(CASE WHEN growth > 0 AND (max_size = -1 OR size < max_size) THEN 1 ELSE 0 END) AS archivos_con_crecimiento
    FROM {BASE}.sys.database_files
    WHERE type = 0
    GROUP BY data_space_id
),
reservado AS (
    SELECT data_space_id, SUM(total_pages) AS paginas
    FROM {BASE}.sys.allocation_units
    GROUP BY data_space_id
)
SELECT
    fg.name                                                      AS filegroup,
    CAST(fg.is_default AS BIT)                                   AS es_predeterminado,
    CAST(fg.is_read_only AS BIT)                                 AS solo_lectura,
    CAST(a.paginas * 8 / 1024.0 AS DECIMAL(18, 2))               AS tamano_mb,
    CAST(ISNULL(r.paginas, 0) * 8 / 1024.0 AS DECIMAL(18, 2))    AS usado_mb,
    a.archivos                                                   AS archivos,
    a.archivos_con_crecimiento                                   AS archivos_con_crecimiento
FROM {BASE}.sys.filegroups AS fg
JOIN archivos AS a ON a.data_space_id = fg.data_space_id
LEFT JOIN reservado AS r ON r.data_space_id = fg.data_space_id
WHERE fg.type = 'FG'
ORDER BY fg.data_space_id;
