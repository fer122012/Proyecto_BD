/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Consultas SQL con mayor consumo de recursos

  Fuente: sys.dm_exec_query_stats + sys.dm_exec_sql_text + sys.dm_exec_plan_attributes
  Las estadísticas se acumulan desde que el plan entró en caché
  (se pierden al reiniciar la instancia o al limpiar la caché de planes).

  Base de datos: dm_exec_sql_text.dbid solo viene lleno para procedimientos y funciones; para
  las sentencias que envían las aplicaciones (ad hoc o con sp_executesql) es NULL. En ese caso
  se toma el atributo dbid del plan, que es la base en la que se compiló.
  Primero se eligen las 20 sentencias y después se obtiene su texto y su base, para no
  consultar esas funciones por cada plan de la caché.

  Tiempos: la DMV los entrega en microsegundos; aquí se convierten a milisegundos.
  Permiso requerido: VIEW SERVER STATE
*/
WITH mayor_consumo AS (
    SELECT TOP (20)
        sql_handle,
        plan_handle,
        statement_start_offset,
        statement_end_offset,
        execution_count,
        total_worker_time,
        total_elapsed_time,
        total_logical_reads,
        total_logical_writes,
        last_execution_time
    FROM sys.dm_exec_query_stats
    ORDER BY total_worker_time DESC
)
SELECT
    DB_NAME(COALESCE(st.dbid, pa.dbid))                AS base_datos,
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
FROM mayor_consumo AS qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS st
OUTER APPLY (SELECT CONVERT(INT, value) AS dbid
             FROM sys.dm_exec_plan_attributes(qs.plan_handle)
             WHERE attribute = N'dbid') AS pa
ORDER BY qs.total_worker_time DESC;
