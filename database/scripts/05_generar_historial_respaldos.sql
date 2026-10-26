/*
  Script 05 - Historial de respaldos de BASE_MONITOR (Módulos 3 y 4)

  Ejecuta respaldos REALES de BASE_MONITOR siguiendo una estrategia típica (completo,
  diferencial y de log) y agrega ventas entre un respaldo y otro, para que:
  - el Módulo 4 muestre un historial con los tres tipos de respaldo, y
  - el Módulo 3 tenga varios respaldos completos con tamaños distintos para graficar el
    crecimiento de la base.

  Secuencia: COMPLETO → ventas → LOG → DIFERENCIAL → ventas → LOG → COMPLETO → ventas → LOG
             → COMPLETO

  Los archivos se guardan en la carpeta de respaldos predeterminada de la instancia
  (SERVERPROPERTY('InstanceDefaultBackupPath')), con la fecha y hora en el nombre. Cada ejecución
  ocupa alrededor de 250 MB; se pueden borrar sin afectar la base (pero el historial en msdb
  seguirá apuntando a ellos).

  Se puede ejecutar varias veces, idealmente en días distintos: cada ejecución agrega puntos al
  historial y la base crece un poco. Ejecutar con un usuario administrador (sysadmin) DESPUÉS de
  00_crear_base_monitor.sql:
      sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\05_generar_historial_respaldos.sql

  WITH CHECKSUM verifica cada página al leerla, y STATS = 25 muestra el avance cada 25 %.
  No se usa compresión porque SQL Server Express no la soporta.
*/
USE master;
GO

IF DB_ID(N'BASE_MONITOR') IS NULL
BEGIN
    RAISERROR(N'BASE_MONITOR no existe. Ejecute primero 00_crear_base_monitor.sql.', 16, 1);
    SET NOEXEC ON;
END
GO

/* Procedimiento temporal: respalda BASE_MONITOR con el tipo indicado en un archivo nuevo. */
CREATE OR ALTER PROCEDURE #respaldar @tipo CHAR(1)
AS
BEGIN
    DECLARE @ruta NVARCHAR(400) = CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS NVARCHAR(400));
    IF RIGHT(@ruta, 1) <> N'\' SET @ruta += N'\';
    DECLARE @marca NVARCHAR(20) = FORMAT(SYSDATETIME(), N'yyyyMMdd_HHmmss_fff');

    IF @tipo = 'D'
    BEGIN
        SET @ruta += N'BASE_MONITOR_COMPLETO_' + @marca + N'.bak';
        BACKUP DATABASE BASE_MONITOR TO DISK = @ruta
            WITH INIT, CHECKSUM, STATS = 25, NAME = N'BASE_MONITOR - respaldo completo';
    END
    ELSE IF @tipo = 'I'
    BEGIN
        SET @ruta += N'BASE_MONITOR_DIFERENCIAL_' + @marca + N'.bak';
        BACKUP DATABASE BASE_MONITOR TO DISK = @ruta
            WITH DIFFERENTIAL, INIT, CHECKSUM, STATS = 25, NAME = N'BASE_MONITOR - respaldo diferencial';
    END
    ELSE
    BEGIN
        SET @ruta += N'BASE_MONITOR_LOG_' + @marca + N'.trn';
        BACKUP LOG BASE_MONITOR TO DISK = @ruta
            WITH INIT, CHECKSUM, STATS = 25, NAME = N'BASE_MONITOR - respaldo de log';
    END
END
GO

/* Procedimiento temporal: registra @facturas ventas nuevas con 5 líneas cada una. */
CREATE OR ALTER PROCEDURE #registrarVentas @facturas INT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @nuevas TABLE (FacturaId INT PRIMARY KEY);

    INSERT INTO BASE_MONITOR.ventas.Factura (ClienteId, EmpleadoId, FechaFactura)
    OUTPUT inserted.FacturaId INTO @nuevas
    SELECT TOP (@facturas)
           1 + ABS(CHECKSUM(NEWID())) % 5000,
           1 + ABS(CHECKSUM(NEWID())) % 200,
           SYSDATETIME()
    FROM sys.all_columns AS a CROSS JOIN sys.all_columns AS b;

    INSERT INTO BASE_MONITOR.ventas.DetalleFactura (FacturaId, ProductoId, Cantidad, PrecioUnitario)
    SELECT n.FacturaId, p.ProductoId, 1 + ABS(CHECKSUM(NEWID())) % 10, p.Precio
    FROM @nuevas AS n
    CROSS APPLY (SELECT TOP (5) ProductoId, Precio
                 FROM BASE_MONITOR.inventario.Producto
                 WHERE ProductoId % 97 = n.FacturaId % 97) AS p;

    UPDATE f
    SET Total = d.Total
    FROM BASE_MONITOR.ventas.Factura AS f
    JOIN (SELECT FacturaId, SUM(Cantidad * PrecioUnitario) AS Total
          FROM BASE_MONITOR.ventas.DetalleFactura
          WHERE FacturaId IN (SELECT FacturaId FROM @nuevas)
          GROUP BY FacturaId) AS d ON d.FacturaId = f.FacturaId;
END
GO

EXEC #respaldar 'D';
EXEC #registrarVentas 15000;
EXEC #respaldar 'L';
EXEC #respaldar 'I';
EXEC #registrarVentas 20000;
EXEC #respaldar 'L';
EXEC #respaldar 'D';
EXEC #registrarVentas 25000;
EXEC #respaldar 'L';
EXEC #respaldar 'D';
GO

SET NOEXEC OFF;
GO

SELECT
    CASE type WHEN 'D' THEN 'Completo' WHEN 'I' THEN 'Diferencial' WHEN 'L' THEN 'Log' END AS tipo,
    backup_finish_date                                AS fin,
    CAST(backup_size / 1048576.0 AS DECIMAL(10, 2))   AS tamano_mb
FROM msdb.dbo.backupset
WHERE database_name = N'BASE_MONITOR'
ORDER BY backup_finish_date;
GO
