/* Módulo 6 - Mantenimiento Preventivo | Acción: Forzar la recompilación de un objeto

   sp_recompile marca un procedimiento, función o disparador para que SQL Server descarte su
   plan en caché y lo compile de nuevo en la siguiente ejecución. Es el equivalente en SQL
   Server a ALTER ... COMPILE de Oracle: no "repara" el objeto, pero sí lo recompila, lo que
   revela de inmediato cualquier error de compilación pendiente (por ejemplo una dependencia
   detectada en la consulta 02_objetos_invalidos.sql).

   sp_recompile solo busca el objeto en la base de datos ACTUAL, y la conexión del backend está en
   master. Por eso se invoca a través de la base de práctica ({BASE}.sys.sp_recompile): un
   procedimiento del sistema llamado con el nombre de una base se ejecuta en el contexto de esa
   base. Pasarle el nombre de tres partes desde master falla con el error 15165 ("No se encontró
   el objeto").

   {BASE} se reemplaza por la base entre corchetes y {OBJETO} por "[esquema].[objeto]", ambos
   formados con identificadores ya validados contra una lista blanca de caracteres (ver
   SqlIdentificadores), por lo que no pueden contener comillas ni cerrar el literal de texto.

   Permiso requerido: ALTER sobre el objeto, otorgado mediante el rol db_ddladmin
   (ver database/scripts/02_permisos_mantenimiento.sql). */
EXEC {BASE}.sys.sp_recompile N'{OBJETO}';
