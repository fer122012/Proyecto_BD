/* Módulo 5 - Auditoría | Consulta: Usuarios de una base de datos

   Un login da entrada al SERVIDOR; un usuario da entrada a UNA BASE. Esta consulta lista los
   usuarios de la base elegida y el login al que está asociado cada uno (se relacionan por sid).

   - tipo:           SQL_USER, WINDOWS_USER, WINDOWS_GROUP, EXTERNAL_USER o EXTERNAL_GROUP.
   - autenticacion:  INSTANCE (por login), WINDOWS, DATABASE (base contenida) o NONE (sin login:
                     existe en la base pero no puede conectarse).
   - login_asociado: NULL si el usuario no tiene login.
   Se omiten sys, INFORMATION_SCHEMA y guest, que existen en toda base y no son cuentas de personas.

   {BASE} es la base elegida por el usuario: el backend la reemplaza ya validada y entre corchetes
   (ver SqlIdentificadores), porque la conexión del backend sigue en master.

   Fuente: {BASE}.sys.database_principals + sys.server_principals
   Permiso: VIEW ANY DEFINITION y un usuario en la base (02_permisos_mantenimiento.sql). */
SELECT
    dp.name                     AS nombre,
    dp.type_desc                AS tipo,
    dp.authentication_type_desc AS autenticacion,
    sp.name                     AS login_asociado,
    dp.default_schema_name      AS esquema_predeterminado,
    dp.create_date              AS fecha_creacion
FROM {BASE}.sys.database_principals AS dp
LEFT JOIN sys.server_principals AS sp ON sp.sid = dp.sid
WHERE dp.type IN ('S', 'U', 'G', 'E', 'X')
  AND dp.name NOT IN (N'sys', N'INFORMATION_SCHEMA', N'guest')
ORDER BY dp.name;
