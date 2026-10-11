/* Módulo 5 - Auditoría | Consulta: Roles de servidor y sus miembros
   Cada fila es un par rol-miembro; el backend las agrupa por rol para mostrar, por cada rol,
   la lista completa de sus miembros (logins u otros roles).
   Fuente: sys.server_role_members + sys.server_principals
   Permiso: VIEW ANY DEFINITION (ya concedido) */
SELECT
    r.name      AS rol,
    m.name      AS miembro,
    m.type_desc AS tipo_miembro
FROM sys.server_role_members rm
JOIN sys.server_principals r ON r.principal_id = rm.role_principal_id
JOIN sys.server_principals m ON m.principal_id = rm.member_principal_id
ORDER BY r.name, m.name;
