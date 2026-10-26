/* Módulo 4 - Respaldo y Recuperación | Consulta: Respaldos y restauraciones en curso

   msdb solo registra un respaldo cuando termina. Para saber si hay uno ejecutándose ahora (por
   ejemplo uno grande que tarda varios minutos) se consulta la solicitud activa: SQL Server
   informa el avance en percent_complete y estima el tiempo restante en estimated_completion_time
   (milisegundos). Si no hay respaldos en curso, la consulta no devuelve filas.

   Fuente: sys.dm_exec_requests
   Permiso: VIEW SERVER STATE (ya concedido en 01_crear_login_monitor.sql) */
SELECT
    r.session_id                                         AS id_sesion,
    DB_NAME(r.database_id)                               AS base_datos,
    r.command                                            AS comando,
    r.start_time                                         AS inicio,
    CAST(r.percent_complete AS DECIMAL(5, 2))            AS porcentaje_avance,
    r.estimated_completion_time / 1000                   AS segundos_restantes
FROM sys.dm_exec_requests AS r
WHERE r.command LIKE N'BACKUP%'
   OR r.command LIKE N'RESTORE%'
ORDER BY r.start_time;
