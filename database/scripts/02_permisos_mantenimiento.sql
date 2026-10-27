/*
  Script 02 - Permisos para los Módulos 5 y 6 dentro de la base de práctica

  El login dba_monitor (creado en 01_crear_login_monitor.sql) solo tiene permisos de SERVIDOR
  de solo lectura: VIEW SERVER STATE y VIEW ANY DEFINITION. Dentro de cada base de práctica
  necesita además:

  - SELECT sobre sys.sql_expression_dependencies: la vista con la que los Módulos 5 y 6
    detectan objetos inválidos. Es una excepción entre las vistas de catálogo: por defecto
    solo la puede leer db_owner, y VIEW ANY DEFINITION no alcanza.
  - El rol db_ddladmin, porque el Módulo 6 EJECUTA dos acciones administrativas:
      - UPDATE STATISTICS (recalcular estadísticas)
      - sp_recompile (forzar la recompilación de un objeto)
    Ambas requieren el permiso ALTER sobre la tabla u objeto, dentro de la base de datos donde
    vive ese objeto. Los permisos de servidor no alcanzan.

  Usa la base de práctica del grupo, BASE_MONITOR (00_crear_base_monitor.sql). Para otra base,
  cambiar el USE y repetir el script. Ejecutar con un usuario que tenga permisos de
  administración (por ejemplo sysadmin).
*/
USE BASE_MONITOR;
GO

-- El login ya existe a nivel de servidor (01_crear_login_monitor.sql); aquí se crea su
-- usuario correspondiente dentro de esta base de datos específica.
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'dba_monitor')
BEGIN
    CREATE USER dba_monitor FOR LOGIN dba_monitor;
END
GO

-- Lectura del catálogo de dependencias (objetos inválidos, Módulos 5 y 6).
GRANT SELECT ON sys.sql_expression_dependencies TO dba_monitor;
GO

-- db_ddladmin: permite ejecutar DDL y comandos de mantenimiento (ALTER TABLE, UPDATE STATISTICS,
-- sp_recompile) sobre los objetos de esta base, SIN dar acceso a los datos que contienen
-- (a diferencia de db_owner, que sí tendría acceso total).
ALTER ROLE db_ddladmin ADD MEMBER dba_monitor;
GO
