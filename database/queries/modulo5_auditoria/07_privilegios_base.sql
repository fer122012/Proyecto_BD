/* Módulo 5 - Auditoría | Consulta: Privilegios dentro de una base de datos

   Permisos otorgados o denegados a usuarios y roles de la base. Un privilegio asignado a un rol
   aparece con el nombre del rol; quién lo recibe a través de él se ve en 06_roles_base.sql.

   - tipo_recurso: DATABASE, SCHEMA, OBJECT_OR_COLUMN, etc.
   - recurso:      esquema u objeto sobre el que aplica; NULL si aplica a toda la base.
   El objeto se busca en sys.all_objects porque también puede ser una vista del sistema.
   Se omite el permiso CONNECT de la base, que tiene todo usuario habilitado, y los principales del
   sistema (public, dbo, guest, sys e INFORMATION_SCHEMA), que no son asignaciones del DBA.

   {BASE} se reemplaza igual que en 05_usuarios_base.sql.

   Fuente: {BASE}.sys.database_permissions + {BASE}.sys.database_principals +
           {BASE}.sys.schemas + {BASE}.sys.all_objects
   Permiso: VIEW ANY DEFINITION y un usuario en la base (02_permisos_mantenimiento.sql). */
SELECT
    p.name             AS principal,
    p.type_desc        AS tipo_principal,
    dp.permission_name AS permiso,
    dp.state_desc      AS estado,
    dp.class_desc      AS tipo_recurso,
    CASE dp.class
        WHEN 3 THEN s.name
        WHEN 1 THEN esquema_objeto.name + N'.' + o.name
    END                AS recurso
FROM {BASE}.sys.database_permissions AS dp
JOIN {BASE}.sys.database_principals AS p ON p.principal_id = dp.grantee_principal_id
LEFT JOIN {BASE}.sys.schemas AS s ON dp.class = 3 AND s.schema_id = dp.major_id
LEFT JOIN {BASE}.sys.all_objects AS o ON dp.class = 1 AND o.object_id = dp.major_id
LEFT JOIN {BASE}.sys.schemas AS esquema_objeto ON esquema_objeto.schema_id = o.schema_id
WHERE p.name NOT IN (N'public', N'dbo', N'guest', N'sys', N'INFORMATION_SCHEMA')
  AND NOT (dp.class = 0 AND dp.permission_name = N'CONNECT')
ORDER BY p.name, recurso, dp.permission_name;
