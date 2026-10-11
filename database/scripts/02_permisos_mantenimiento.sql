/*
  Script 02 - Permisos para el Módulo 6 (Mantenimiento preventivo)

  El login dba_monitor (creado en 01_crear_login_monitor.sql) solo tiene permisos de SERVIDOR
  de solo lectura: VIEW SERVER STATE y VIEW ANY DEFINITION. Esos permisos alcanzan para TODOS
  los módulos de consulta (1 a 5), pero el Módulo 6 además EJECUTA dos acciones administrativas:
    - UPDATE STATISTICS (recalcular estadísticas)
    - sp_recompile (forzar la recompilación de un objeto)
  Ambas requieren el permiso ALTER sobre la tabla u objeto, dentro de la base de datos donde
  vive ese objeto. Los permisos de servidor no alcanzan: hay que concederlo en CADA BASE DE
  DATOS de práctica que el grupo use para la demostración (por ejemplo AdventureWorks).

  Reemplazar <BASE_DE_PRACTICA> por el nombre real antes de ejecutar, y ejecutar con un usuario
  que tenga permisos de administración (por ejemplo sysadmin). Repetir este script por cada
  base de datos adicional que se quiera usar con el Módulo 6.
*/
USE <BASE_DE_PRACTICA>;
GO

-- El login ya existe a nivel de servidor (01_crear_login_monitor.sql); aquí se crea su
-- usuario correspondiente dentro de esta base de datos específica.
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE USER dba_monitor FOR LOGIN dba_monitor;
END
GO

-- db_ddladmin: permite ejecutar DDL y comandos de mantenimiento (ALTER TABLE, UPDATE STATISTICS,
-- sp_recompile) sobre los objetos de esta base, SIN dar acceso a los datos que contienen
-- (a diferencia de db_owner, que sí tendría acceso total).
ALTER ROLE db_ddladmin ADD MEMBER dba_monitor;
GO
