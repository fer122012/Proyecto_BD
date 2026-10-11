/* Módulo 5 - Auditoría | Consulta: Logins registrados
   Fuente: sys.server_principals. Permiso: VIEW ANY DEFINITION (ya concedido) */
SELECT
    sp.name                  AS nombre,
    sp.type_desc             AS tipo,   -- SQL_LOGIN, WINDOWS_LOGIN, WINDOWS_GROUP
    CAST(1 - sp.is_disabled AS BIT) AS habilitado,
    sp.default_database_name AS base_predeterminada,
    sp.create_date           AS fecha_creacion,
    CAST(CASE WHEN EXISTS (
        SELECT 1 FROM sys.server_role_members rm
                          JOIN sys.server_principals r ON r.principal_id = rm.role_principal_id
        WHERE rm.member_principal_id = sp.principal_id AND r.name = N'sysadmin')
                  THEN 1 ELSE 0 END AS BIT) AS es_sysadmin
FROM sys.server_principals sp
WHERE sp.type IN ('S','U','G') AND sp.name NOT LIKE '##%'
ORDER BY sp.name;