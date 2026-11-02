/* Módulo 5 - Auditoría | Consulta: Roles de servidor y sus miembros
   Cada fila es un par rol-miembro; el backend las agrupa por rol para mostrar, por cada rol,
   la lista completa de sus miembros (logins u otros roles). Se parte de los roles con LEFT JOIN
   para que también aparezcan los que no tienen miembros (el miembro llega en NULL).
   public se omite porque todo login pertenece a él. Los roles internos ## solo se muestran si
   tienen miembros.
   Fuente: sys.server_principals + sys.server_role_members
   Permiso: VIEW ANY DEFINITION (ya concedido) */
SELECT
    r.name      AS rol,
    m.name      AS miembro,
    m.type_desc AS tipo_miembro
FROM sys.server_principals r
LEFT JOIN sys.server_role_members rm ON rm.role_principal_id = r.principal_id
LEFT JOIN sys.server_principals m ON m.principal_id = rm.member_principal_id
WHERE r.type = 'R'
  AND r.name <> N'public'
  AND (r.name NOT LIKE '##%' OR m.name IS NOT NULL)
ORDER BY r.name, m.name;
