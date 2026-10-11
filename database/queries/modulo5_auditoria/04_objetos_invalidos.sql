/* Módulo 5 - Auditoría | Consulta: Objetos con dependencias no resueltas (errores de compilación)

   SQL Server no tiene el concepto de "objeto inválido" de Oracle porque usa resolución de
   nombres diferida: al crear una vista, procedimiento o función, NO valida que las tablas,
   columnas u otros objetos que referencia existan de verdad. El error solo aparece cuando el
   objeto se ejecuta. Esta consulta encuentra esos objetos ANTES de ejecutarlos, revisando el
   catálogo de dependencias: si SQL Server no pudo resolver a qué objeto apunta una referencia
   (referenced_id IS NULL), es porque ese objeto no existe o fue eliminado/renombrado después.

   Fuente: sys.sql_expression_dependencies
   Permiso: VIEW ANY DEFINITION (ya concedido)

   Nota: is_ambiguous = 0 descarta los casos en que SQL Server no pudo decidir a cuál de varios
   objetos candidatos se refiere (por ejemplo por falta de prefijo de esquema), que no son
   necesariamente un error sino una referencia ambigua que igual se resuelve en tiempo de
   ejecución según el esquema del usuario. */
SELECT
    SCHEMA_NAME(o.schema_id)   AS esquema,
    o.name                     AS objeto,
    o.type_desc                AS tipo,
    dep.referenced_schema_name AS esquema_referenciado,
    dep.referenced_entity_name AS objeto_referenciado
FROM sys.sql_expression_dependencies dep
JOIN sys.objects o ON o.object_id = dep.referencing_id
WHERE dep.referenced_id IS NULL
  AND dep.is_ambiguous = 0
ORDER BY esquema, o.name;
