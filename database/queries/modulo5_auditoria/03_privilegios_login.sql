/* Módulo 5 - Auditoría | Consulta: Privilegios (permisos) de servidor otorgados a logins
   Permisos concedidos o denegados DIRECTAMENTE a un login (no a través de un rol). Los
   permisos heredados por pertenecer a un rol se ven en 02_roles_servidor.sql: quien
   pertenece al rol sysadmin, por ejemplo, tiene todos los permisos sin necesidad de que
   aparezcan aquí uno por uno.
   Fuente: sys.server_permissions + sys.server_principals
   Permiso: VIEW ANY DEFINITION (ya concedido) */
SELECT
    p.name              AS login,
    sp.permission_name  AS permiso,
    sp.state_desc       AS estado   -- GRANT, DENY o GRANT_WITH_GRANT_OPTION
FROM sys.server_permissions sp
JOIN sys.server_principals p ON p.principal_id = sp.grantee_principal_id
WHERE p.type IN ('S', 'U', 'G') AND p.name NOT LIKE '##%'
ORDER BY p.name, sp.permission_name;
