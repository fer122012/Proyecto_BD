/*
  Módulo 2 - Monitoreo de Rendimiento
  Consulta: Sesiones bloqueadas (si existen)

  Fuente: sys.dm_exec_requests (+ sys.dm_exec_sessions y sys.dm_exec_sql_text)
  Una solicitud está bloqueada cuando blocking_session_id <> 0.
  Se muestra la sesión bloqueada, quién la bloquea y la sentencia de cada una.
  Si no hay bloqueos, la consulta no devuelve filas.

  Tiempos: wait_time viene en milisegundos.
  Permiso requerido: VIEW SERVER STATE
*/
SELECT
    r.session_id                         AS id_sesion_bloqueada,
    sb.login_name                        AS usuario_bloqueado,
    DB_NAME(r.database_id)               AS base_datos,
    r.blocking_session_id                AS id_sesion_bloqueadora,
    sk.login_name                        AS usuario_bloqueador,
    sk.host_name                         AS equipo_bloqueador,
    sk.program_name                      AS programa_bloqueador,
    r.wait_type                          AS tipo_espera,
    r.wait_time                          AS tiempo_espera_ms,
    r.wait_resource                      AS recurso_esperado,
    txt_bloqueado.text                   AS consulta_bloqueada,
    txt_bloqueador.text                  AS consulta_bloqueadora
FROM sys.dm_exec_requests AS r
JOIN sys.dm_exec_sessions AS sb
     ON sb.session_id = r.session_id
LEFT JOIN sys.dm_exec_sessions AS sk
     ON sk.session_id = r.blocking_session_id
LEFT JOIN sys.dm_exec_connections AS ck
     ON ck.session_id = r.blocking_session_id
OUTER APPLY sys.dm_exec_sql_text(r.sql_handle) AS txt_bloqueado
-- La sesión bloqueadora puede estar inactiva (transacción abierta sin solicitud en curso),
-- por eso su última sentencia se toma de la conexión y no de dm_exec_requests.
OUTER APPLY sys.dm_exec_sql_text(ck.most_recent_sql_handle) AS txt_bloqueador
WHERE r.blocking_session_id <> 0
ORDER BY r.wait_time DESC;
