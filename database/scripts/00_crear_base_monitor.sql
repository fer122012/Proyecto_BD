/*
  Script 00 - Base de datos de práctica BASE_MONITOR

  Crea la base común del grupo para probar y demostrar los seis módulos con información real
  del SGBD. Simula los sistemas de la empresa del caso de estudio (DataSolutions Costa Rica S.A.):
  recursos humanos, ventas e inventario.

  Qué deja preparado para cada módulo:
  - Módulo 1: una base de usuario más en la lista de la instancia.
  - Módulo 3: tres filegroups. FG_DATOS crece de forma automática; FG_HISTORICO tiene un archivo
    de tamaño fijo (sin autocrecimiento) que queda casi lleno, para ver el estado CRITICO.
  - Módulo 4: modelo de recuperación FULL, necesario para los respaldos de log
    (ver 05_generar_historial_respaldos.sql).
  - Módulo 5 y 6: objetos con dependencias rotas (una vista y dos procedimientos) y roles de base.
  - Módulo 6: estadísticas desactualizadas en tablas de esquemas distintos de dbo.

  IMPORTANTE:
  - Ejecutar con un usuario administrador (sysadmin), por ejemplo:
      sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\00_crear_base_monitor.sql
    (-f 65001 hace que sqlcmd lea el archivo como UTF-8; sin eso las tildes quedan mal guardadas).
  - Si BASE_MONITOR ya existe, el script la ELIMINA y la vuelve a crear, junto con su historial
    de respaldos en msdb. Así todos los integrantes parten de la misma base.
  - No contiene contraseñas: los usuarios de base de datos se crean WITHOUT LOGIN.
  - Los archivos se crean en la carpeta de datos predeterminada de la instancia.
*/
USE master;
GO

IF DB_ID(N'BASE_MONITOR') IS NOT NULL
BEGIN
    ALTER DATABASE BASE_MONITOR SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    EXEC msdb.dbo.sp_delete_database_backuphistory @database_name = N'BASE_MONITOR';
    DROP DATABASE BASE_MONITOR;
END
GO

/* La ruta de datos cambia según la instalación, por eso el CREATE DATABASE se arma con SQL
   dinámico a partir de la carpeta predeterminada de la instancia. */
DECLARE @ruta NVARCHAR(400) = CAST(SERVERPROPERTY('InstanceDefaultDataPath') AS NVARCHAR(400));
DECLARE @sql NVARCHAR(MAX) = N'
CREATE DATABASE BASE_MONITOR
ON PRIMARY
    (NAME = BASE_MONITOR_primario,  FILENAME = ''{RUTA}BASE_MONITOR_primario.mdf'',
           SIZE = 16MB, FILEGROWTH = 16MB),
FILEGROUP FG_DATOS DEFAULT
    (NAME = BASE_MONITOR_datos,     FILENAME = ''{RUTA}BASE_MONITOR_datos.ndf'',
           SIZE = 32MB, MAXSIZE = 2GB, FILEGROWTH = 32MB),
FILEGROUP FG_HISTORICO
    -- Tamaño fijo a propósito (FILEGROWTH = 0): demuestra el estado CRITICO del Módulo 3.
    (NAME = BASE_MONITOR_historico, FILENAME = ''{RUTA}BASE_MONITOR_historico.ndf'',
           SIZE = 8MB, FILEGROWTH = 0)
LOG ON
    (NAME = BASE_MONITOR_log,       FILENAME = ''{RUTA}BASE_MONITOR_log.ldf'',
           SIZE = 16MB, FILEGROWTH = 16MB);';
SET @sql = REPLACE(@sql, N'{RUTA}', @ruta);
EXEC (@sql);
GO

ALTER DATABASE BASE_MONITOR SET RECOVERY FULL;
GO

USE BASE_MONITOR;
GO

/* ============================================================================================
   Esquemas y tablas
   ============================================================================================ */
CREATE SCHEMA rrhh;
GO
CREATE SCHEMA ventas;
GO
CREATE SCHEMA inventario;
GO

CREATE TABLE rrhh.Departamento (
    DepartamentoId INT IDENTITY PRIMARY KEY,
    Nombre         NVARCHAR(60) NOT NULL
);

CREATE TABLE rrhh.Empleado (
    EmpleadoId     INT IDENTITY PRIMARY KEY,
    DepartamentoId INT NOT NULL REFERENCES rrhh.Departamento (DepartamentoId),
    Nombre         NVARCHAR(80) NOT NULL,
    FechaIngreso   DATE NOT NULL,
    Salario        DECIMAL(12, 2) NOT NULL
);

CREATE TABLE inventario.Producto (
    ProductoId INT IDENTITY PRIMARY KEY,
    Nombre     NVARCHAR(80) NOT NULL,
    Categoria  NVARCHAR(40) NOT NULL,
    Precio     DECIMAL(12, 2) NOT NULL,
    Existencia INT NOT NULL
);

CREATE TABLE ventas.Cliente (
    ClienteId INT IDENTITY PRIMARY KEY,
    Nombre    NVARCHAR(80) NOT NULL,
    Provincia NVARCHAR(30) NOT NULL,
    Correo    NVARCHAR(120) NOT NULL
);

CREATE TABLE ventas.Factura (
    FacturaId    INT IDENTITY PRIMARY KEY,
    ClienteId    INT NOT NULL REFERENCES ventas.Cliente (ClienteId),
    EmpleadoId   INT NOT NULL REFERENCES rrhh.Empleado (EmpleadoId),
    FechaFactura DATETIME2(0) NOT NULL,
    Total        DECIMAL(14, 2) NOT NULL DEFAULT 0
);

CREATE TABLE ventas.DetalleFactura (
    DetalleId      INT IDENTITY PRIMARY KEY,
    FacturaId      INT NOT NULL REFERENCES ventas.Factura (FacturaId),
    ProductoId     INT NOT NULL REFERENCES inventario.Producto (ProductoId),
    Cantidad       INT NOT NULL,
    PrecioUnitario DECIMAL(12, 2) NOT NULL
);

-- Facturas de años anteriores, en el filegroup de tamaño fijo.
CREATE TABLE ventas.FacturaHistorica (
    FacturaHistoricaId INT IDENTITY PRIMARY KEY,
    ClienteId          INT NOT NULL,
    FechaFactura       DATETIME2(0) NOT NULL,
    Total              DECIMAL(14, 2) NOT NULL,
    Observaciones      NCHAR(200) NOT NULL   -- ancho fijo: ocupa espacio de forma predecible
) ON FG_HISTORICO;
GO

/* ============================================================================================
   Datos de ejemplo (generados por conjuntos a partir de una tabla de números)
   ============================================================================================ */
SET NOCOUNT ON;

INSERT INTO rrhh.Departamento (Nombre)
VALUES (N'Ventas'), (N'Finanzas'), (N'Tecnología'), (N'Recursos Humanos'), (N'Bodega');

WITH numeros AS (
    SELECT TOP (60000) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_columns AS a CROSS JOIN sys.all_columns AS b
)
SELECT n INTO #numeros FROM numeros;

INSERT INTO rrhh.Empleado (DepartamentoId, Nombre, FechaIngreso, Salario)
SELECT 1 + n % 5,
       CONCAT(N'Empleado ', n),
       DATEADD(DAY, -(n * 37 % 3650), CAST(GETDATE() AS DATE)),
       450000 + (n * 7919 % 1500000)
FROM #numeros WHERE n <= 200;

INSERT INTO inventario.Producto (Nombre, Categoria, Precio, Existencia)
SELECT CONCAT(N'Producto ', n),
       CHOOSE(1 + n % 5, N'Cómputo', N'Oficina', N'Redes', N'Software', N'Mobiliario'),
       1000 + (n * 104729 % 500000) / 100.0,
       n * 13 % 500
FROM #numeros WHERE n <= 1000;

INSERT INTO ventas.Cliente (Nombre, Provincia, Correo)
SELECT CONCAT(N'Cliente ', n),
       CHOOSE(1 + n % 7, N'San José', N'Alajuela', N'Cartago', N'Heredia', N'Guanacaste', N'Puntarenas', N'Limón'),
       CONCAT(N'cliente', n, N'@ejemplo.cr')
FROM #numeros WHERE n <= 5000;

INSERT INTO ventas.Factura (ClienteId, EmpleadoId, FechaFactura)
SELECT 1 + n % 5000,
       1 + n % 200,
       DATEADD(MINUTE, -(n * 53 % 525600), SYSDATETIME())
FROM #numeros WHERE n <= 40000;

INSERT INTO ventas.DetalleFactura (FacturaId, ProductoId, Cantidad, PrecioUnitario)
SELECT f.FacturaId,
       1 + (f.FacturaId * 31 + l.linea * 17) % 1000,
       1 + (f.FacturaId + l.linea) % 10,
       1000 + ((f.FacturaId * 31 + l.linea * 17) % 1000) * 3.5
FROM ventas.Factura AS f
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS l (linea);

UPDATE f
SET Total = d.Total
FROM ventas.Factura AS f
JOIN (SELECT FacturaId, SUM(Cantidad * PrecioUnitario) AS Total
      FROM ventas.DetalleFactura GROUP BY FacturaId) AS d ON d.FacturaId = f.FacturaId;

/* Llena FG_HISTORICO hasta ~90 % de su tamaño fijo. Se inserta por lotes y se mide el espacio
   ocupado del archivo con FILEPROPERTY (en páginas de 8 KB), en vez de adivinar cuántas filas
   caben. */
DECLARE @paginasArchivo INT = (SELECT size FROM sys.database_files WHERE name = N'BASE_MONITOR_historico');
WHILE FILEPROPERTY(N'BASE_MONITOR_historico', 'SpaceUsed') < @paginasArchivo * 0.90
BEGIN
    BEGIN TRY
        INSERT INTO ventas.FacturaHistorica (ClienteId, FechaFactura, Total, Observaciones)
        SELECT TOP (500) 1 + n % 5000,
               DATEADD(DAY, -(365 + n % 1825), SYSDATETIME()),
               5000 + n % 900000,
               N'Factura migrada del sistema anterior'
        FROM #numeros;
    END TRY
    BEGIN CATCH
        BREAK;   -- error 1105: el filegroup se llenó antes de llegar al 90 %
    END CATCH
END;

DROP TABLE #numeros;
GO

/* ============================================================================================
   Índices (cada índice crea su estadística)
   ============================================================================================ */
CREATE INDEX IX_Factura_Fecha         ON ventas.Factura (FechaFactura);
CREATE INDEX IX_Factura_Cliente       ON ventas.Factura (ClienteId);
CREATE INDEX IX_DetalleFactura_Producto ON ventas.DetalleFactura (ProductoId) INCLUDE (Cantidad, PrecioUnitario);
CREATE INDEX IX_Producto_Categoria    ON inventario.Producto (Categoria);
CREATE INDEX IX_Empleado_Departamento ON rrhh.Empleado (DepartamentoId);
CREATE INDEX IX_Cliente_Provincia     ON ventas.Cliente (Provincia);
-- Estadística sin índice: el optimizador la usa para estimar filtros por precio.
CREATE STATISTICS ST_Producto_Precio  ON inventario.Producto (Precio);
GO

/* ============================================================================================
   Vistas y procedimientos válidos
   ============================================================================================ */
CREATE VIEW ventas.vw_VentasPorProvincia
AS
SELECT c.Provincia, COUNT(*) AS Facturas, SUM(f.Total) AS Total
FROM ventas.Factura AS f
JOIN ventas.Cliente AS c ON c.ClienteId = f.ClienteId
GROUP BY c.Provincia;
GO

CREATE PROCEDURE ventas.usp_FacturasDeCliente @ClienteId INT
AS
SELECT FacturaId, FechaFactura, Total
FROM ventas.Factura
WHERE ClienteId = @ClienteId
ORDER BY FechaFactura DESC;
GO

/* ============================================================================================
   Objetos inválidos (Módulos 5 y 6)
   SQL Server usa resolución de nombres diferida: deja crear un objeto que referencia algo que
   no existe, y el error solo aparece al ejecutarlo. Aquí se provocan los dos casos típicos.
   ============================================================================================ */

-- Caso 1: la tabla existía cuando se crearon la vista y el procedimiento, y luego se eliminó.
CREATE TABLE ventas.Descuento (
    DescuentoId INT IDENTITY PRIMARY KEY,
    ClienteId   INT NOT NULL,
    Porcentaje  DECIMAL(5, 2) NOT NULL
);
GO

CREATE VIEW ventas.vw_FacturasConDescuento
AS
SELECT f.FacturaId, f.Total, d.Porcentaje, f.Total * (1 - d.Porcentaje / 100) AS TotalConDescuento
FROM ventas.Factura AS f
JOIN ventas.Descuento AS d ON d.ClienteId = f.ClienteId;
GO

CREATE PROCEDURE ventas.usp_AplicarDescuento @ClienteId INT, @Porcentaje DECIMAL(5, 2)
AS
INSERT INTO ventas.Descuento (ClienteId, Porcentaje) VALUES (@ClienteId, @Porcentaje);
GO

DROP TABLE ventas.Descuento;
GO

-- Caso 2: el procedimiento se creó referenciando una tabla que nunca existió.
CREATE PROCEDURE rrhh.usp_CalcularBonos @Anio INT
AS
SELECT e.EmpleadoId, e.Nombre, b.Monto
FROM rrhh.Empleado AS e
JOIN rrhh.Bono AS b ON b.EmpleadoId = e.EmpleadoId
WHERE b.Anio = @Anio;
GO

/* ============================================================================================
   Estadísticas desactualizadas (Módulo 6)
   Se modifican muchas filas DESPUÉS de crear los índices. El contador modification_counter de
   cada estadística sube y se mantiene hasta que alguien las recalcula (o hasta que una consulta
   que las necesite dispare la actualización automática). El contador es por columna: solo
   cuenta para las estadísticas cuya primera columna se modificó.
   ============================================================================================ */
UPDATE inventario.Producto SET Precio = Precio * 1.13;                          -- 100 % de las filas
UPDATE ventas.Cliente SET Provincia = N'San José' WHERE ClienteId % 5 < 2;      -- 40 % de las filas
UPDATE ventas.Factura SET FechaFactura = DATEADD(DAY, 1, FechaFactura)
WHERE FacturaId % 2 = 0;                                                         -- 50 % de las filas
UPDATE rrhh.Empleado SET DepartamentoId = 1 + (EmpleadoId + 1) % 5
WHERE EmpleadoId % 3 = 0;                                                        -- 33 % de las filas
GO

/* ============================================================================================
   Roles y usuarios de base de datos (Módulo 5)
   Usuarios WITHOUT LOGIN: existen solo dentro de la base, no tienen contraseña y no pueden
   conectarse; sirven para mostrar roles y privilegios sin crear credenciales.
   ============================================================================================ */
CREATE ROLE rol_ventas;
GRANT SELECT, INSERT, UPDATE ON SCHEMA::ventas TO rol_ventas;
GRANT SELECT ON SCHEMA::inventario TO rol_ventas;

CREATE ROLE rol_rrhh;
GRANT SELECT, INSERT, UPDATE ON SCHEMA::rrhh TO rol_rrhh;

CREATE ROLE rol_consulta;
GRANT SELECT ON SCHEMA::ventas TO rol_consulta;
GRANT SELECT ON SCHEMA::inventario TO rol_consulta;
DENY SELECT ON rrhh.Empleado TO rol_consulta;   -- los salarios no son públicos

CREATE USER usr_cajero      WITHOUT LOGIN;
CREATE USER usr_planillas   WITHOUT LOGIN;
CREATE USER usr_reportes    WITHOUT LOGIN;
ALTER ROLE rol_ventas   ADD MEMBER usr_cajero;
ALTER ROLE rol_rrhh     ADD MEMBER usr_planillas;
ALTER ROLE rol_consulta ADD MEMBER usr_reportes;
GO

PRINT 'BASE_MONITOR creada. Siguientes pasos: scripts de permisos (02 y 03) y 05 (historial de respaldos).';
GO
