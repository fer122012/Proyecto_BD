/* Módulo 4 - Respaldo y Recuperación | Consulta: Historial de respaldos de una base

   Los últimos 50 respaldos de la base elegida, del más reciente al más antiguo, con su tipo,
   duración, tamaño, destino y las marcas que indican si el respaldo es confiable:

   - is_damaged:              1 si SQL Server detectó páginas dañadas al respaldar (se respaldó
                              con CONTINUE_AFTER_ERROR). El respaldo existe pero no es confiable.
   - has_incomplete_metadata: 1 si es un respaldo de la cola del log con metadatos incompletos.
   - has_backup_checksums:    1 si se hizo WITH CHECKSUM: SQL Server verificó cada página al
                              leerla, así que un archivo de respaldo corrupto se detecta al restaurar.
   - is_copy_only:            respaldo fuera de la cadena (no afecta diferenciales ni el log).

   Un respaldo que falló no aparece aquí: msdb solo registra los que terminaron.

   El nombre de la base llega como parámetro (?) porque es un valor del WHERE, no un nombre de
   objeto. El destino sale de backupmediafamily (un respaldo puede dividirse en varios archivos;
   se toma el primero, family_sequence_number = 1).

   Fuente: msdb.dbo.backupset + msdb.dbo.backupmediafamily
   Permiso: usuario en msdb con SELECT sobre esas dos tablas (04_permisos_respaldo.sql). */
SELECT TOP (50)
    b.backup_set_id                                          AS id_respaldo,
    b.type                                                   AS tipo,
    b.backup_start_date                                      AS inicio,
    b.backup_finish_date                                     AS fin,
    DATEDIFF(SECOND, b.backup_start_date, b.backup_finish_date) AS duracion_segundos,
    CAST(b.backup_size / 1048576.0 AS DECIMAL(18, 2))        AS tamano_mb,
    b.recovery_model                                         AS modelo_recuperacion,
    b.is_copy_only                                           AS es_copia,
    b.is_damaged                                             AS danado,
    b.has_incomplete_metadata                                AS metadatos_incompletos,
    b.has_backup_checksums                                   AS con_checksum,
    b.user_name                                              AS usuario,
    m.physical_device_name                                   AS destino
FROM msdb.dbo.backupset AS b
LEFT JOIN msdb.dbo.backupmediafamily AS m
       ON m.media_set_id = b.media_set_id
      AND m.family_sequence_number = 1
WHERE b.database_name = ?
-- backup_set_id desempata los respaldos que terminan en el mismo segundo.
ORDER BY b.backup_finish_date DESC, b.backup_set_id DESC;
