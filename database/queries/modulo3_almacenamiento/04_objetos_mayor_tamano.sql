/* Módulo 3 - Gestión del Almacenamiento | Consulta: Objetos con mayor consumo de almacenamiento

   Las 20 tablas e índices que más espacio reservan en la base elegida. Cada fila es un índice
   (o el montón, HEAP, de una tabla sin índice clúster), porque en SQL Server el espacio se
   asigna por índice: el índice clúster contiene los datos de la tabla y cada índice no clúster
   ocupa su propio espacio.

   - reservado_mb: páginas asignadas al objeto (lo que ocupa en el archivo).
   - usado_mb:     páginas que realmente contienen datos. reservado - usado es espacio asignado
                   pero vacío dentro del objeto.
   - filas:        filas del índice (suma de todas sus particiones).

   Las unidades de asignación de datos en fila y de desbordamiento (type 1 y 3) se relacionan
   con la partición por hobt_id, y las de datos LOB (type 2) por partition_id.
   Se excluyen los objetos del sistema (is_ms_shipped = 1).

   El esquema se obtiene uniendo {BASE}.sys.schemas, y no con SCHEMA_NAME(), porque esa función
   resuelve el nombre en la base actual de la conexión (master) y no en {BASE}.

   Fuente: {BASE}.sys.partitions + {BASE}.sys.allocation_units + {BASE}.sys.objects +
           {BASE}.sys.indexes + {BASE}.sys.schemas
   Permiso: VIEW ANY DEFINITION y un usuario en la base (03_permisos_almacenamiento.sql).
   {BASE} se reemplaza igual que en 02_espacio_filegroups.sql. */
WITH espacio AS (
    SELECT
        p.object_id,
        p.index_id,
        SUM(au.total_pages) AS paginas_reservadas,
        SUM(au.used_pages)  AS paginas_usadas
    FROM {BASE}.sys.partitions AS p
    JOIN {BASE}.sys.allocation_units AS au
        ON au.container_id = CASE WHEN au.type IN (1, 3) THEN p.hobt_id ELSE p.partition_id END
    GROUP BY p.object_id, p.index_id
),
filas AS (
    SELECT object_id, index_id, SUM(rows) AS filas
    FROM {BASE}.sys.partitions
    GROUP BY object_id, index_id
)
SELECT TOP (20)
    s.name                                                       AS esquema,
    o.name                                                       AS objeto,
    o.type_desc                                                  AS tipo_objeto,
    ISNULL(i.name, 'HEAP')                                       AS indice,
    i.type_desc                                                  AS tipo_indice,
    fl.filas                                                     AS filas,
    CAST(e.paginas_reservadas * 8 / 1024.0 AS DECIMAL(18, 2))    AS reservado_mb,
    CAST(e.paginas_usadas * 8 / 1024.0 AS DECIMAL(18, 2))        AS usado_mb
FROM espacio AS e
JOIN filas AS fl ON fl.object_id = e.object_id AND fl.index_id = e.index_id
JOIN {BASE}.sys.objects AS o ON o.object_id = e.object_id
JOIN {BASE}.sys.schemas AS s ON s.schema_id = o.schema_id
JOIN {BASE}.sys.indexes AS i ON i.object_id = e.object_id AND i.index_id = e.index_id
WHERE o.is_ms_shipped = 0
ORDER BY e.paginas_reservadas DESC, s.name, o.name, i.index_id;
