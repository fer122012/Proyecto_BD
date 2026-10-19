/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Procesos que consumen mayor cantidad de CPU o memoria

  Fuente: sys.dm_exec_sessions
        + sys.dm_exec_requests            (lo que el proceso ejecuta en este momento)
        + sys.dm_exec_query_memory_grants (memoria concedida a la consulta en curso)
  Un "proceso" en SQL Server es una sesión (SPID). Se incluyen procesos de usuario
  y del sistema, porque un proceso interno también puede consumir recursos.

  - cpu_ms / lecturas_logicas: lo acumulado por la sesión MÁS lo de la solicitud en curso.
    dm_exec_sessions solo suma el CPU de una solicitud cuando esta termina, así que sin
    dm_exec_requests un proceso que está consumiendo ahora mismo aparecería con 0 ms.
  - memoria_sesion_kb: memoria propia de la sesión (memory_usage viene en páginas de 8 KB).
  - memoria_concedida_kb: memoria de trabajo (sorts, hashes) de la consulta que corre ahora;
    NULL si el proceso no está ejecutando nada.
  - porcentaje_cpu: participación del proceso en el CPU de todos los procesos listados.

  Para ordenar por memoria en lugar de CPU, cambiar el ORDER BY (ver al final).
  Permiso requerido: VIEW SERVER STATE
*/
WITH procesos AS (
    SELECT
        s.session_id                                  AS id_proceso,
        CASE WHEN s.is_user_process = 1
             THEN 'Usuario' ELSE 'Sistema' END        AS tipo_proceso,
        s.login_name                                  AS usuario,
        s.program_name                                AS programa,
        DB_NAME(s.database_id)                        AS base_datos,
        s.status                                      AS estado,
        s.cpu_time + ISNULL(r.cpu_time, 0)            AS cpu_ms,
        s.memory_usage * 8                            AS memoria_sesion_kb,
        mg.granted_memory_kb                          AS memoria_concedida_kb,
        s.logical_reads + ISNULL(r.logical_reads, 0)  AS lecturas_logicas,
        r.command                                     AS comando_actual,
        txt.text                                      AS consulta_actual
    FROM sys.dm_exec_sessions AS s
    LEFT JOIN sys.dm_exec_requests AS r
           ON r.session_id = s.session_id
    LEFT JOIN sys.dm_exec_query_memory_grants AS mg
           ON mg.session_id = r.session_id
          AND mg.request_id = r.request_id
    OUTER APPLY sys.dm_exec_sql_text(r.sql_handle) AS txt
    WHERE s.session_id <> @@SPID
      -- Se omiten procesos sin consumo que no están ejecutando nada (ruido en el ranking).
      -- Las tareas internas siempre tienen una solicitud en estado 'background'/'sleeping',
      -- por eso solo cuentan las solicitudes que realmente están corriendo o esperando.
      AND (s.cpu_time > 0
           OR s.memory_usage > 0
           OR r.status IN ('running', 'runnable', 'suspended'))
)
SELECT TOP (20)
    id_proceso,
    tipo_proceso,
    usuario,
    programa,
    base_datos,
    estado,
    cpu_ms,
    CAST(100.0 * cpu_ms / NULLIF(SUM(cpu_ms) OVER (), 0)
         AS DECIMAL(5, 2))                            AS porcentaje_cpu,
    memoria_sesion_kb,
    memoria_concedida_kb,
    lecturas_logicas,
    comando_actual,
    consulta_actual
FROM procesos
ORDER BY cpu_ms DESC;
-- Por memoria:
-- ORDER BY ISNULL(memoria_concedida_kb, 0) + memoria_sesion_kb DESC;
