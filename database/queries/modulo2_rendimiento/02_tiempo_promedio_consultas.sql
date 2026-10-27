/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Tiempo promedio de ejecución de consultas

  Fuente: sys.dm_exec_query_stats + sys.dm_exec_sql_text + sys.dm_exec_plan_attributes
  Promedio = total / execution_count (convertido de microsegundos a milisegundos).
  Se ordena por duración promedio para resaltar las consultas más lentas.

  Base de datos: igual que en 01_top_consultas_consumo.sql, si dm_exec_sql_text no trae el
  dbid (sentencias ad hoc o con sp_executesql) se toma el del plan.
  Permiso requerido: VIEW SERVER STATE
*/
WITH mas_lentas AS (
    SELECT TOP (20)
        sql_handle,
        plan_handle,
        statement_start_offset,
        statement_end_offset,
        execution_count,
        total_elapsed_time,
        total_worker_time,
        total_logical_reads,
        min_elapsed_time,
        max_elapsed_time
    FROM sys.dm_exec_query_stats
    WHERE execution_count > 0
    ORDER BY total_elapsed_time / execution_count DESC
)
SELECT
    DB_NAME(COALESCE(st.dbid, pa.dbid))                           AS base_datos,
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
FROM mas_lentas AS qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS st
OUTER APPLY (SELECT CONVERT(INT, value) AS dbid
             FROM sys.dm_exec_plan_attributes(qs.plan_handle)
             WHERE attribute = N'dbid') AS pa
ORDER BY duracion_promedio_ms DESC;
