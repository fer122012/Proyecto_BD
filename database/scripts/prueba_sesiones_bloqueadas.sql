/*
  Módulo 2 - Monitoreo de Rendimiento
  Script de prueba: provocar un bloqueo para validar
  database/queries/modulo2_rendimiento/04_sesiones_bloqueadas.sql

  Usa una tabla temporal en tempdb, no toca ninguna base del proyecto.
  Se ejecuta en SSMS con TRES ventanas de consulta (cada una es una sesión distinta).

  Resultado esperado en el paso 3:
    1 fila con la sesión del paso 2 bloqueada por la del paso 1,
    tipo_espera = LCK_M_S y las dos sentencias visibles.
  Después del paso 4 la consulta 04 debe devolver 0 filas.
*/

------------------------------------------------------------
-- PASO 0 (cualquier ventana): crear la tabla de prueba
------------------------------------------------------------
IF OBJECT_ID('tempdb.dbo.prueba_bloqueo') IS NULL
BEGIN
    CREATE TABLE tempdb.dbo.prueba_bloqueo (id INT PRIMARY KEY, valor INT);
    INSERT tempdb.dbo.prueba_bloqueo VALUES (1, 10);
END;

------------------------------------------------------------
-- PASO 1 (ventana 1): sesión bloqueadora
-- Deja la transacción abierta; NO ejecutar el ROLLBACK todavía.
------------------------------------------------------------
BEGIN TRAN;
UPDATE tempdb.dbo.prueba_bloqueo SET valor = valor + 1 WHERE id = 1;

------------------------------------------------------------
-- PASO 2 (ventana 2): sesión bloqueada
-- Se queda "Ejecutando consulta..." esperando a la ventana 1.
------------------------------------------------------------
SELECT * FROM tempdb.dbo.prueba_bloqueo WHERE id = 1;

------------------------------------------------------------
-- PASO 3 (ventana 3): ejecutar la consulta del módulo
--   database/queries/modulo2_rendimiento/04_sesiones_bloqueadas.sql
------------------------------------------------------------

------------------------------------------------------------
-- PASO 4 (ventana 1): liberar el bloqueo
------------------------------------------------------------
ROLLBACK;

------------------------------------------------------------
-- PASO 5 (cualquier ventana): limpiar
------------------------------------------------------------
DROP TABLE IF EXISTS tempdb.dbo.prueba_bloqueo;
