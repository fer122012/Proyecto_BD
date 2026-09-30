/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Consultas SQL con mayor consumo de recursos

  Fuente: sys.dm_exec_query_stats + sys.dm_exec_sql_text
  Las estadísticas se acumulan desde que el plan entró en caché
  (se pierden al reiniciar la instancia o al limpiar la caché de planes).

  Tiempos: la DMV los entrega en microsegundos; aquí se convierten a milisegundos.
  Permiso requerido: VIEW SERVER STATE
*/
SELECT TOP (20)
    DB_NAME(st.dbid)                                   AS base_datos,
    SUBSTRING(st.text,
              (qs.statement_start_offset / 2) + 1,
              ((CASE qs.statement_end_offset
                    WHEN -1 THEN DATALENGTH(st.text)
                    ELSE qs.statement_end_offset
                END - qs.statement_start_offset) / 2) + 1) AS consulta,
    qs.execution_count                                 AS ejecuciones,
    qs.total_worker_time / 1000                        AS cpu_total_ms,
    qs.total_elapsed_time / 1000                       AS duracion_total_ms,
    qs.total_logical_reads                             AS lecturas_logicas_total,
    qs.total_logical_writes                            AS escrituras_logicas_total,
    qs.last_execution_time                             AS ultima_ejecucion
FROM sys.dm_exec_query_stats AS qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS st
ORDER BY qs.total_worker_time DESC;
