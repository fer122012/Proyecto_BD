/*
  Script 03 - Permisos para el Módulo 3 (Gestión del almacenamiento)

  El tamaño de cada base (sys.master_files) se lee con los permisos de servidor que ya otorga
  01_crear_login_monitor.sql. Pero el espacio por filegroup, los archivos y los objetos más
  grandes se consultan con vistas de catálogo DE CADA BASE (por ejemplo AdventureWorks.sys.
  allocation_units), y para leerlas el login necesita un usuario dentro de esa base. No se le da
  ningún rol: VIEW ANY DEFINITION ya le permite ver los metadatos, y no necesita leer los datos.

  El historial de crecimiento sale de los respaldos registrados en msdb, así que también se crea
  el usuario en msdb con permiso de lectura SOLO sobre las dos tablas de historial que se usan.

  Usa la base de práctica del grupo, BASE_MONITOR (00_crear_base_monitor.sql). Para otra base,
  cambiar el USE. Ejecutar con un usuario
  que tenga permisos de administración (por ejemplo sysadmin). Si la base ya tiene el usuario
  dba_monitor por el script 02 (Módulo 6), la primera parte no hace nada.
*/
USE BASE_MONITOR;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE USER dba_monitor FOR LOGIN dba_monitor;
END
GO

USE msdb;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE USER dba_monitor FOR LOGIN dba_monitor;
END
GO

-- Solo lectura del historial de respaldos: tamaño de la base en cada respaldo completo.
GRANT SELECT ON dbo.backupset TO dba_monitor;
GRANT SELECT ON dbo.backupfile TO dba_monitor;
GO
