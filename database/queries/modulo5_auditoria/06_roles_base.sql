/* Módulo 5 - Auditoría | Consulta: Roles de una base de datos y sus miembros

   Cada fila es un par rol-miembro; el backend las agrupa por rol. Se parte de los roles con
   LEFT JOIN para que también aparezcan los roles creados que todavía no tienen miembros.
   De los roles fijos (db_owner, db_ddladmin, etc.) solo se muestran los que tienen miembros, para
   no llenar la lista con roles sin uso. public se omite: todo usuario pertenece a él.

   {BASE} se reemplaza igual que en 05_usuarios_base.sql.

   Fuente: {BASE}.sys.database_principals + {BASE}.sys.database_role_members
   Permiso: VIEW ANY DEFINITION y un usuario en la base (02_permisos_mantenimiento.sql). */
SELECT
    r.name          AS rol,
    r.is_fixed_role AS es_fijo,
    m.name          AS miembro
FROM {BASE}.sys.database_principals AS r
LEFT JOIN {BASE}.sys.database_role_members AS rm ON rm.role_principal_id = r.principal_id
LEFT JOIN {BASE}.sys.database_principals AS m ON m.principal_id = rm.member_principal_id
WHERE r.type = 'R'
  AND r.name <> N'public'
  AND (r.is_fixed_role = 0 OR m.name IS NOT NULL)
ORDER BY r.is_fixed_role, r.name, m.name;
