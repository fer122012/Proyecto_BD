/* Módulo 6 - Mantenimiento Preventivo | Acción: Forzar la recompilación de un objeto

   sp_recompile marca un procedimiento, función o disparador para que SQL Server descarte su
   plan en caché y lo compile de nuevo en la siguiente ejecución. Es el equivalente en SQL
   Server a ALTER ... COMPILE de Oracle: no "repara" el objeto, pero sí lo recompila, lo que
   revela de inmediato cualquier error de compilación pendiente (por ejemplo una dependencia
   detectada en la consulta 02_objetos_invalidos.sql).

   {OBJETO} se reemplaza en el backend con el nombre completo "base.esquema.objeto", formado a
   partir de tres identificadores ya validados contra una lista blanca de caracteres (ver
   SqlIdentificadores), por lo que no puede contener comillas ni cerrar el literal de texto.

   Permiso requerido: ALTER sobre el objeto, otorgado mediante el rol db_ddladmin
   (ver database/scripts/02_permisos_mantenimiento.sql). */
EXEC sp_recompile N'{OBJETO}';
