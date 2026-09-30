/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Tiempo promedio de ejecución de consultas

  Fuente: sys.dm_exec_query_stats + sys.dm_exec_sql_text
  Promedio = total / execution_count (convertido de microsegundos a milisegundos).
  Se ordena por duración promedio para resaltar las consultas más lentas.
  Permiso requerido: VIEW SERVER STATE
*/
SELECT TOP (20)
    DB_NAME(st.dbid)                                              AS base_datos,
    SUBSTRING(st.text,
              (qs.statement_start_offset / 2) + 1,
              ((CASE qs.statement_end_offset
                    WHEN -1 THEN DATALENGTH(st.text)
                    ELSE qs.statement_end_offset
                END - qs.statement_start_offset) / 2) + 1)        AS consulta,
    qs.execution_count                                            AS ejecuciones,
    CAST(qs.total_elapsed_time / 1000.0 / qs.execution_count
         AS DECIMAL(18, 2))                                       AS duracion_promedio_ms,
    CAST(qs.total_worker_time / 1000.0 / qs.execution_count
         AS DECIMAL(18, 2))                                       AS cpu_promedio_ms,
    qs.total_logical_reads / qs.execution_count                   AS lecturas_promedio,
    qs.min_elapsed_time / 1000                                    AS duracion_min_ms,
    qs.max_elapsed_time / 1000                                    AS duracion_max_ms
FROM sys.dm_exec_query_stats AS qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS st
WHERE qs.execution_count > 0
ORDER BY duracion_promedio_ms DESC;
