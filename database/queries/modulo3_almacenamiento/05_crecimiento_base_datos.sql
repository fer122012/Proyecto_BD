/* Módulo 3 - Gestión del Almacenamiento | Consulta: Historial de crecimiento de la base

   SQL Server no guarda un historial del tamaño de las bases, pero cada respaldo deja registrado
   en msdb el tamaño que tenía cada archivo en ese momento. Con los respaldos completos (FULL)
   se arma una serie real del tamaño de la base en el tiempo:

   - fecha:            momento en que terminó el respaldo completo.
   - tamano_datos_mb:  tamaño asignado de los archivos de datos en esa fecha.
   - tamano_log_mb:    tamaño asignado de los archivos de log en esa fecha.
   - usado_mb:         tamaño del respaldo (backup_size): las páginas con datos que tenía la base,
                       es decir, cuánto espacio estaba realmente ocupado.

   Se devuelven los últimos 30 respaldos completos, del más antiguo al más reciente, para que el
   backend calcule la variación entre uno y otro. Si la base nunca se respaldó, no hay historial
   (el Módulo 4 explica cómo hacer respaldos).

   El nombre de la base llega como parámetro (?) y no se inserta en el texto, porque aquí es un
   valor de la cláusula WHERE y no un nombre de objeto.

   Fuente: msdb.dbo.backupset + msdb.dbo.backupfile
   Permiso: un usuario en msdb con SELECT sobre esas dos tablas (03_permisos_almacenamiento.sql). */
SELECT fecha, tamano_datos_mb, tamano_log_mb, usado_mb
FROM (
    SELECT TOP (30)
        b.backup_finish_date                                                       AS fecha,
        CAST(SUM(CASE WHEN f.file_type = 'D' THEN f.file_size ELSE 0 END) / 1048576.0 AS DECIMAL(18, 2)) AS tamano_datos_mb,
        CAST(SUM(CASE WHEN f.file_type = 'L' THEN f.file_size ELSE 0 END) / 1048576.0 AS DECIMAL(18, 2)) AS tamano_log_mb,
        CAST(MAX(b.backup_size) / 1048576.0 AS DECIMAL(18, 2))                     AS usado_mb
    FROM msdb.dbo.backupset AS b
    JOIN msdb.dbo.backupfile AS f ON f.backup_set_id = b.backup_set_id
    WHERE b.database_name = ?
      AND b.type = 'D'
    GROUP BY b.backup_set_id, b.backup_finish_date
    ORDER BY b.backup_finish_date DESC
) AS ultimos
ORDER BY fecha ASC;
