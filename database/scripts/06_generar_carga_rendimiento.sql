/*
  Script 06 - Generar carga para el Módulo 2 (Monitoreo de rendimiento)

  Las consultas del Módulo 2 leen DMVs que solo tienen datos cuando la instancia tiene actividad:
  en una instancia de práctica recién iniciada la caché de planes está casi vacía y no hay sesiones
  trabajando. Este script ejecuta sobre BASE_MONITOR un conjunto de consultas típicas de los
  sistemas del caso de estudio (reportes de ventas, búsquedas, actualizaciones de inventario),
  algunas escritas a propósito de forma ineficiente, para que la herramienta muestre:

  - Consultas con mayor consumo (01) y tiempo promedio (02): cada sentencia se ejecuta varias veces
    con sp_executesql, así reutiliza su plan y acumula ejecuciones en sys.dm_exec_query_stats.
  - Sesiones activas (03) y procesos por CPU/memoria (05): mientras el script corre, su sesión
    aparece consumiendo CPU y con memoria concedida para los ordenamientos.
  - Sesiones bloqueadas (04): usar database/scripts/prueba_sesiones_bloqueadas.sql.

  Requisitos:
  - BASE_MONITOR creada (00_crear_base_monitor.sql).
  - Solo lee y actualiza datos de práctica: las actualizaciones de existencias se revierten
    al final de cada vuelta, así que la base queda igual.

  Ejecución (una sesión):
      sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\06_generar_carga_rendimiento.sql -o NUL
  (-o NUL descarta los resultados de las consultas, que no interesan; -f 65001 lee las tildes bien).
  En SSMS: Consulta > Opciones de consulta > Resultados > "Descartar resultados después de la ejecución".
  Para ver varias sesiones a la vez, ejecutar el mismo comando en 2 o 3 terminales.
  Con @vueltas = 20 tarda alrededor de un minuto; subirlo para una demostración más larga.
*/
/* SQL Server Express crea las bases con AUTO_CLOSE activado: cuando la última sesión se
   desconecta, la base se cierra y SQL Server vacía TODA la caché de planes de la instancia.
   Sin esto, al terminar la carga las consultas 01 y 02 del Módulo 2 quedarían vacías. */
IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'BASE_MONITOR' AND is_auto_close_on = 1)
    ALTER DATABASE BASE_MONITOR SET AUTO_CLOSE OFF;
GO

USE BASE_MONITOR;
GO

SET NOCOUNT ON;

DECLARE @vueltas INT = 20;
DECLARE @vuelta INT = 1;
DECLARE @inicio DATETIME2 = SYSDATETIME();

-- Las sentencias se guardan en variables y se ejecutan con sp_executesql para que SQL Server
-- reutilice un mismo plan con distintos parámetros, como lo haría una aplicación real.

-- Reporte de ventas por provincia y mes (join + agregación sobre 200 mil detalles).
DECLARE @ventasPorProvincia NVARCHAR(MAX) = N'
SELECT c.Provincia,
       DATEFROMPARTS(YEAR(f.FechaFactura), MONTH(f.FechaFactura), 1) AS Mes,
       COUNT(DISTINCT f.FacturaId) AS Facturas,
       SUM(d.Cantidad * d.PrecioUnitario) AS Monto
FROM ventas.Factura AS f
JOIN ventas.Cliente AS c ON c.ClienteId = f.ClienteId
JOIN ventas.DetalleFactura AS d ON d.FacturaId = f.FacturaId
WHERE f.FechaFactura >= @desde
GROUP BY c.Provincia, DATEFROMPARTS(YEAR(f.FechaFactura), MONTH(f.FechaFactura), 1)
ORDER BY Monto DESC;';

-- Productos más vendidos de una categoría: ordenamiento grande (pide memoria concedida).
DECLARE @productosMasVendidos NVARCHAR(MAX) = N'
SELECT TOP (50) p.Nombre, SUM(d.Cantidad) AS Unidades, SUM(d.Cantidad * d.PrecioUnitario) AS Monto
FROM ventas.DetalleFactura AS d
JOIN inventario.Producto AS p ON p.ProductoId = d.ProductoId
WHERE p.Categoria = @categoria
GROUP BY p.Nombre
ORDER BY Unidades DESC;';

-- Búsqueda de clientes con comodín al inicio: no puede usar un índice, recorre toda la tabla.
DECLARE @buscarCliente NVARCHAR(MAX) = N'
SELECT c.ClienteId, c.Nombre, c.Correo, COUNT(f.FacturaId) AS Facturas
FROM ventas.Cliente AS c
LEFT JOIN ventas.Factura AS f ON f.ClienteId = c.ClienteId
WHERE c.Correo LIKE @patron
GROUP BY c.ClienteId, c.Nombre, c.Correo;';

-- Filtro no SARGable: YEAR() sobre la columna impide buscar por rango y obliga a recorrer la tabla.
DECLARE @facturasDelAnio NVARCHAR(MAX) = N'
SELECT COUNT(*) AS Facturas, SUM(Total) AS Monto
FROM ventas.Factura
WHERE YEAR(FechaFactura) = @anio;';

-- Ranking de empleados por ventas (función de ventana sobre todas las facturas).
DECLARE @rankingEmpleados NVARCHAR(MAX) = N'
SELECT TOP (20) e.Nombre, d.Nombre AS Departamento, v.Monto,
       RANK() OVER (ORDER BY v.Monto DESC) AS Posicion
FROM (SELECT EmpleadoId, SUM(Total) AS Monto FROM ventas.Factura GROUP BY EmpleadoId) AS v
JOIN rrhh.Empleado AS e ON e.EmpleadoId = v.EmpleadoId
JOIN rrhh.Departamento AS d ON d.DepartamentoId = e.DepartamentoId;';

-- Consulta liviana y frecuente: detalle de una factura por su llave primaria.
DECLARE @detalleFactura NVARCHAR(MAX) = N'
SELECT d.ProductoId, d.Cantidad, d.PrecioUnitario
FROM ventas.DetalleFactura AS d
WHERE d.FacturaId = @factura;';

-- Actualización de existencias (escrituras lógicas). Se revierte al final de cada vuelta.
DECLARE @actualizarExistencias NVARCHAR(MAX) = N'
UPDATE inventario.Producto
SET Existencia = Existencia - 1
WHERE Categoria = @categoria;';

WHILE @vuelta <= @vueltas
BEGIN
    DECLARE @categoria NVARCHAR(40) = CHOOSE(1 + @vuelta % 5, N'Cómputo', N'Oficina', N'Redes', N'Software', N'Mobiliario');

    EXEC sp_executesql @ventasPorProvincia, N'@desde DATETIME2', @desde = '2000-01-01';
    EXEC sp_executesql @productosMasVendidos, N'@categoria NVARCHAR(40)', @categoria = @categoria;
    EXEC sp_executesql @buscarCliente, N'@patron NVARCHAR(120)', @patron = N'%5@ejemplo.cr';
    EXEC sp_executesql @facturasDelAnio, N'@anio INT', @anio = 2026;
    EXEC sp_executesql @rankingEmpleados;

    DECLARE @i INT = 1;
    WHILE @i <= 50
    BEGIN
        DECLARE @factura INT = 1 + (@vuelta * 997 + @i * 131) % 40000;
        EXEC sp_executesql @detalleFactura, N'@factura INT', @factura = @factura;
        SET @i += 1;
    END;

    BEGIN TRAN;
        EXEC sp_executesql @actualizarExistencias, N'@categoria NVARCHAR(40)', @categoria = @categoria;
    ROLLBACK;

    SET @vuelta += 1;
END;

PRINT CONCAT('Carga terminada: ', @vueltas, ' vueltas en ',
             DATEDIFF(SECOND, @inicio, SYSDATETIME()), ' s.');
GO
