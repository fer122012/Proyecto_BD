/*
  Script 01 - Crear login de monitoreo
  Crea el login dba_monitor que usa la aplicación para conectarse a SQL Server
  y le otorga los permisos de servidor que necesitan las consultas de monitoreo.

  IMPORTANTE:
  - Reemplazar '<CAMBIAR_CONTRASENA>' por una contraseña segura SOLO al ejecutar el script.
    Nunca guardar la contraseña real en este archivo ni subirla al repositorio.
  - La contraseña real se configura en application-local.properties (ignorado por Git).
  - Ejecutar con un usuario que tenga permisos de administración (por ejemplo sysadmin).
*/
USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE LOGIN dba_monitor
        WITH PASSWORD = N'<CAMBIAR_CONTRASENA>',
             DEFAULT_DATABASE = master,
             CHECK_POLICY = ON;   -- aplica la política de contraseñas de Windows
END
GO

-- VIEW SERVER STATE: permite leer las vistas de administración dinámica (DMVs) de todo el
-- servidor, por ejemplo sys.dm_exec_query_stats, sys.dm_exec_sessions y sys.dm_exec_requests.
-- Lo necesitan las consultas de estado de la instancia y de rendimiento.
GRANT VIEW SERVER STATE TO dba_monitor;
GO

-- VIEW ANY DEFINITION: permite ver los metadatos (definiciones) de todos los objetos del
-- servidor y de las bases de datos, como tablas, índices, archivos y procedimientos, sin
-- dar acceso a los datos que contienen.
GRANT VIEW ANY DEFINITION TO dba_monitor;
GO

/*
  Nota: otros módulos (por ejemplo respaldo y recuperación, auditoría o mantenimiento
  preventivo) pueden requerir permisos adicionales. Esos permisos se agregarán en scripts
  numerados posteriores (02_..., 03_...) dentro de database/scripts/.
*/
