/* Módulo 3 - Gestión del Almacenamiento | Consulta: Tamaño actual de cada base de datos

   Devuelve una fila por base de datos con el espacio que tiene asignado en disco, separado en
   archivos de datos (ROWS) y de log (LOG). Es el "tamaño actual de la base de datos" que pide
   el enunciado: lo que ocupan sus archivos, haya o no datos dentro.

   - tamano_datos_mb: suma de los archivos de datos (.mdf / .ndf).
   - tamano_log_mb:   suma de los archivos de log de transacciones (.ldf).
   - tamano_total_mb: datos + log.
   - cantidad_archivos: número de archivos físicos de la base.

   La columna size está en páginas de 8 KB, por eso se multiplica por 8 y se divide entre 1024
   para obtener MB. Los archivos FILESTREAM y de OLTP en memoria (tipos 2 y 3) no se cuentan
   porque su tamaño no se expresa en páginas.

   Fuente: sys.master_files (catálogo del servidor, una fila por archivo de cada base).
   Permiso: VIEW ANY DEFINITION (lo otorga 01_crear_login_monitor.sql). No hace falta tener
   usuario dentro de cada base, porque la vista vive en master. */
SELECT
    d.name                                                               AS nombre,
    d.state_desc                                                         AS estado,
    CAST(CASE WHEN d.database_id <= 4 THEN 1 ELSE 0 END AS BIT)          AS es_sistema,
    CAST(SUM(CASE WHEN f.type = 0 THEN f.size ELSE 0 END) * 8 / 1024.0 AS DECIMAL(18, 2)) AS tamano_datos_mb,
    CAST(SUM(CASE WHEN f.type = 1 THEN f.size ELSE 0 END) * 8 / 1024.0 AS DECIMAL(18, 2)) AS tamano_log_mb,
    CAST(SUM(f.size) * 8 / 1024.0 AS DECIMAL(18, 2))                     AS tamano_total_mb,
    COUNT(*)                                                             AS cantidad_archivos
FROM sys.databases AS d
JOIN sys.master_files AS f ON f.database_id = d.database_id
WHERE f.type IN (0, 1)
GROUP BY d.name, d.state_desc, d.database_id
ORDER BY tamano_total_mb DESC;
