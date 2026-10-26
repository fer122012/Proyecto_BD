/* Módulo 6 - Mantenimiento Preventivo | Consulta: Estado de las estadísticas de las tablas

   El optimizador de consultas de SQL Server usa estadísticas (histogramas de distribución de
   valores por columna) para decidir el plan de ejecución. Si una tabla cambia mucho y sus
   estadísticas no se actualizan, el optimizador puede elegir planes malos. Esta consulta
   muestra, para cada estadística de cada tabla de usuario, cuándo se actualizó por última vez
   y cuántas filas cambiaron desde entonces, para priorizar sobre cuáles conviene recalcular.

   Fuente: sys.tables + sys.stats + sys.dm_db_stats_properties
   Permiso: VIEW SERVER STATE (para leer sys.dm_db_stats_properties)

   {BASE} es la base de datos de práctica elegida por el usuario (por ejemplo AdventureWorks).
   El backend la reemplaza ya validada y escrita entre corchetes (ver SqlIdentificadores),
   porque esta vista de administración dinámica solo informa sobre la base de datos actual y
   aquí se consulta sin cambiar el contexto de conexión (que sigue siendo master).

   El esquema se toma de {BASE}.sys.schemas y no con SCHEMA_NAME(): esa función busca el
   schema_id en la base ACTUAL (master), así que para esquemas distintos de dbo devolvía NULL.

   ultima_actualizacion es NULL si SQL Server nunca calculó la estadística (por ejemplo, un
   índice creado sobre una tabla vacía que luego se llenó y nadie ha consultado). Esas van
   primero, porque el optimizador no tiene ninguna información sobre esos datos. */
SELECT TOP (50)
    sch.name                 AS esquema,
    t.name                   AS tabla,
    s.name                   AS estadistica,
    sp.last_updated          AS ultima_actualizacion,
    sp.rows                  AS filas,
    sp.modification_counter  AS filas_modificadas
FROM {BASE}.sys.tables AS t
JOIN {BASE}.sys.schemas AS sch ON sch.schema_id = t.schema_id
JOIN {BASE}.sys.stats AS s ON s.object_id = t.object_id
CROSS APPLY {BASE}.sys.dm_db_stats_properties(t.object_id, s.stats_id) AS sp
WHERE t.is_ms_shipped = 0
ORDER BY CASE WHEN sp.last_updated IS NULL THEN 0 ELSE 1 END,
         sp.modification_counter DESC, sp.last_updated ASC;
