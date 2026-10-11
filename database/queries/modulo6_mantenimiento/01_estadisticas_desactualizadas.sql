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
   aquí se consulta sin cambiar el contexto de conexión (que sigue siendo master). */
SELECT TOP (50)
    SCHEMA_NAME(t.schema_id) AS esquema,
    t.name                   AS tabla,
    s.name                   AS estadistica,
    sp.last_updated          AS ultima_actualizacion,
    sp.rows                  AS filas,
    sp.modification_counter  AS filas_modificadas
FROM {BASE}.sys.tables AS t
JOIN {BASE}.sys.stats AS s ON s.object_id = t.object_id
CROSS APPLY {BASE}.sys.dm_db_stats_properties(t.object_id, s.stats_id) AS sp
WHERE t.is_ms_shipped = 0
ORDER BY sp.modification_counter DESC, sp.last_updated ASC;
