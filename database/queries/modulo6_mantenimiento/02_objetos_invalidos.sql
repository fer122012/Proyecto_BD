/* Módulo 6 - Mantenimiento Preventivo | Consulta: Objetos candidatos a recompilar

   Mismo criterio que el Módulo 5 (ver database/queries/modulo5_auditoria/04_objetos_invalidos.sql):
   objetos cuyas referencias a otras tablas, vistas o columnas no se pudieron resolver en el
   catálogo. Aquí se consultan dentro de la base de práctica {BASE} elegida por el usuario, para
   ofrecerlos directamente como candidatos a la acción "recompilar" de este módulo.

   El esquema se toma de {BASE}.sys.schemas y no con SCHEMA_NAME(), que buscaría en master.

   Fuente: sys.sql_expression_dependencies
   Permiso: SELECT sobre sys.sql_expression_dependencies dentro de la base. Por defecto solo lo
   tiene db_owner; VIEW ANY DEFINITION no alcanza (ver 02_permisos_mantenimiento.sql). */
SELECT
    sch.name                   AS esquema,
    o.name                     AS objeto,
    o.type_desc                AS tipo,
    dep.referenced_schema_name AS esquema_referenciado,
    dep.referenced_entity_name AS objeto_referenciado
FROM {BASE}.sys.sql_expression_dependencies dep
JOIN {BASE}.sys.objects o ON o.object_id = dep.referencing_id
JOIN {BASE}.sys.schemas sch ON sch.schema_id = o.schema_id
WHERE dep.referenced_id IS NULL
  AND dep.is_ambiguous = 0
ORDER BY esquema, o.name;
