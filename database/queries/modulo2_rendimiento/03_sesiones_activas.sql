/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Sesiones activas

  Fuente: sys.dm_exec_sessions (+ sys.dm_exec_requests para lo que se está ejecutando ahora)
  Solo sesiones de usuario (is_user_process = 1); se excluye la sesión que ejecuta esta consulta.
  Permiso requerido: VIEW SERVER STATE
*/
SELECT
    s.session_id                         AS id_sesion,
    s.login_name                         AS usuario,
    s.host_name                          AS equipo,
    s.program_name                       AS programa,
    DB_NAME(s.database_id)               AS base_datos,
    s.status                             AS estado_sesion,
    r.status                             AS estado_solicitud,
    r.command                            AS comando,
    r.wait_type                          AS tipo_espera,
    s.cpu_time                           AS cpu_ms,
    s.memory_usage * 8                   AS memoria_kb,   -- memory_usage viene en páginas de 8 KB
    s.login_time                         AS inicio_sesion,
    s.last_request_start_time            AS ultima_solicitud
FROM sys.dm_exec_sessions AS s
LEFT JOIN sys.dm_exec_requests AS r
       ON r.session_id = s.session_id
WHERE s.is_user_process = 1
  AND s.session_id <> @@SPID
ORDER BY s.cpu_time DESC;
