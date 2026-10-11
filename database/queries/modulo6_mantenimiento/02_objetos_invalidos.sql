/* Módulo 6 - Mantenimiento Preventivo | Consulta: Objetos candidatos a recompilar

   Mismo criterio que el Módulo 5 (ver database/queries/modulo5_auditoria/04_objetos_invalidos.sql):
   objetos cuyas referencias a otras tablas, vistas o columnas no se pudieron resolver en el
   catálogo. Aquí se consultan dentro de la base de práctica {BASE} elegida por el usuario, para
   ofrecerlos directamente como candidatos a la acción "recompilar" de este módulo.

   Fuente: sys.sql_expression_dependencies
   Permiso: VIEW ANY DEFINITION (ya concedido) */
SELECT
    SCHEMA_NAME(o.schema_id)   AS esquema,
    o.name                     AS objeto,
    o.type_desc                AS tipo,
    dep.referenced_schema_name AS esquema_referenciado,
    dep.referenced_entity_name AS objeto_referenciado
FROM {BASE}.sys.sql_expression_dependencies dep
JOIN {BASE}.sys.objects o ON o.object_id = dep.referencing_id
WHERE dep.referenced_id IS NULL
  AND dep.is_ambiguous = 0
ORDER BY esquema, o.name;
