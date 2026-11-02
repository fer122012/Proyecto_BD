/* Módulo 6 - Mantenimiento Preventivo | Acción: Validar los objetos de una base

   El catálogo de dependencias (02_objetos_invalidos.sql) detecta tablas u objetos que faltan, pero
   no columnas eliminadas: la tabla sigue existiendo, así que la referencia aparece resuelta. Para
   encontrarlas, esta acción ejecuta sp_refreshsqlmodule sobre cada vista, procedimiento y función
   y guarda el error que devuelva, por ejemplo "Invalid column name 'x'" (el texto lo da SQL Server).
   Las dos revisiones se complementan: a un procedimiento le puede faltar una tabla sin que
   sp_refreshsqlmodule falle, porque la resolución de nombres diferida lo permite.

   Cada intento va en su propia transacción y siempre se revierte, para que la validación no cambie
   los metadatos de los objetos. Es más costosa que leer el catálogo porque analiza cada objeto; por
   eso se ejecuta solo cuando el usuario la pide y no al abrir la pantalla.
   Se omiten los objetos con SCHEMABINDING: SQL Server impide eliminar lo que ellos usan.
   El mensaje se guarda después del ROLLBACK porque el error puede dejar la transacción sin
   posibilidad de escribir hasta revertirla.

   El lote corre en la base elegida a través de {BASE}.sys.sp_executesql, porque sp_refreshsqlmodule
   y las vistas del lote trabajan sobre la base actual. {BASE} se reemplaza por la base ya validada
   y entre corchetes (ver SqlIdentificadores).

   Fuente: sys.sql_modules + sys.objects + sys.schemas + sp_refreshsqlmodule
   Permiso requerido: ALTER sobre los objetos, mediante el rol db_ddladmin
   (ver database/scripts/02_permisos_mantenimiento.sql). */
SET NOCOUNT ON;
EXEC {BASE}.sys.sp_executesql N'
SET NOCOUNT ON;
DECLARE @errores TABLE (esquema SYSNAME, objeto SYSNAME, tipo NVARCHAR(60), error NVARCHAR(2048));
DECLARE @esquema SYSNAME, @objeto SYSNAME, @tipo NVARCHAR(60), @nombre NVARCHAR(600), @error NVARCHAR(2048);

DECLARE modulos CURSOR LOCAL FAST_FORWARD FOR
    SELECT s.name, o.name, o.type_desc
    FROM sys.sql_modules AS m
    JOIN sys.objects AS o ON o.object_id = m.object_id
    JOIN sys.schemas AS s ON s.schema_id = o.schema_id
    WHERE o.type IN (''V'', ''P'', ''FN'', ''IF'', ''TF'')
      AND o.is_ms_shipped = 0
      AND m.is_schema_bound = 0;

OPEN modulos;
FETCH NEXT FROM modulos INTO @esquema, @objeto, @tipo;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @nombre = QUOTENAME(@esquema) + N''.'' + QUOTENAME(@objeto);
    SET @error = NULL;
    BEGIN TRANSACTION;
    BEGIN TRY
        EXEC sys.sp_refreshsqlmodule @name = @nombre;
    END TRY
    BEGIN CATCH
        SET @error = ERROR_MESSAGE();
    END CATCH;
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    IF @error IS NOT NULL
        INSERT INTO @errores VALUES (@esquema, @objeto, @tipo, @error);
    FETCH NEXT FROM modulos INTO @esquema, @objeto, @tipo;
END;
CLOSE modulos;
DEALLOCATE modulos;

SELECT esquema, objeto, tipo, error FROM @errores ORDER BY esquema, objeto;
';
