/* Módulo 4 - Respaldo y Recuperación | Consulta: Último respaldo de cada base de datos

   Una fila por base de datos con el último respaldo exitoso de cada tipo. SQL Server registra
   en msdb cada respaldo que TERMINA correctamente (BACKUP DATABASE / BACKUP LOG); un respaldo
   que falla no queda en msdb.dbo.backupset, solo en el log de errores de la instancia.

   Tipos de respaldo (columna type de backupset):
   - D = completo (FULL): copia toda la base. Es la base de cualquier restauración.
   - I = diferencial: solo lo que cambió desde el último completo.
   - L = de log: las transacciones desde el último respaldo de log. Solo existe en los modelos
     de recuperación FULL y BULK_LOGGED, y es lo que permite restaurar a un punto en el tiempo.

   Los respaldos COPY_ONLY se excluyen del cálculo del "último" porque no forman parte de la
   cadena de respaldos (no sirven como base de un diferencial ni cortan el log).

   - horas_desde_*: horas transcurridas desde el fin de ese respaldo, calculadas con la hora del
     servidor (GETDATE) para que no dependa del reloj del equipo donde corre la aplicación.
   - tempdb se excluye: se recrea en cada inicio de la instancia y no se puede respaldar.

   Fuente: sys.databases + msdb.dbo.backupset
   Permiso: usuario en msdb con SELECT sobre backupset (04_permisos_respaldo.sql). */
WITH ultimos AS (
    SELECT
        database_name,
        MAX(CASE WHEN type = 'D' THEN backup_finish_date END) AS ultimo_completo,
        MAX(CASE WHEN type = 'I' THEN backup_finish_date END) AS ultimo_diferencial,
        MAX(CASE WHEN type = 'L' THEN backup_finish_date END) AS ultimo_log,
        COUNT(*)                                             AS total_respaldos
    FROM msdb.dbo.backupset
    WHERE is_copy_only = 0
    GROUP BY database_name
)
SELECT
    d.name                                                      AS base_datos,
    d.state_desc                                                AS estado,
    d.recovery_model_desc                                       AS modelo_recuperacion,
    CAST(CASE WHEN d.database_id <= 4 THEN 1 ELSE 0 END AS BIT) AS es_sistema,
    u.ultimo_completo,
    u.ultimo_diferencial,
    u.ultimo_log,
    DATEDIFF(MINUTE, u.ultimo_completo, GETDATE()) / 60.0       AS horas_desde_completo,
    DATEDIFF(MINUTE, u.ultimo_log, GETDATE()) / 60.0            AS horas_desde_log,
    ISNULL(u.total_respaldos, 0)                                AS total_respaldos
FROM sys.databases AS d
LEFT JOIN ultimos AS u ON u.database_name = d.name
WHERE d.name <> N'tempdb'
ORDER BY es_sistema, d.name;
