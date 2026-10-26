/*
  Script 04 - Permisos para el Módulo 4 (Respaldo y recuperación)

  El historial de respaldos de TODAS las bases de la instancia está en msdb. El login
  dba_monitor necesita un usuario en msdb con permiso de lectura solo sobre las tablas que usa
  el módulo; no se le da ningún rol de msdb (por ejemplo db_datareader daría acceso también a
  los trabajos, alertas y operadores, que no necesita).

  - backupset:         un registro por respaldo terminado (tipo, fechas, tamaño, marcas de daño).
  - backupmediafamily: el archivo o dispositivo de destino de cada respaldo.

  El estado de cada base (sys.databases) y los respaldos en curso (sys.dm_exec_requests) ya se
  leen con los permisos de servidor de 01_crear_login_monitor.sql.

  Si ya se ejecutó 03_permisos_almacenamiento.sql, el usuario en msdb ya existe y el GRANT sobre
  backupset se repite sin efecto. Ejecutar con un usuario administrador (sysadmin).
*/
USE msdb;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE USER dba_monitor FOR LOGIN dba_monitor;
END
GO

GRANT SELECT ON dbo.backupset TO dba_monitor;
GRANT SELECT ON dbo.backupmediafamily TO dba_monitor;
GO
