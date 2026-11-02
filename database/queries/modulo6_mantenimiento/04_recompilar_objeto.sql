/* Módulo 6 - Mantenimiento Preventivo | Acción: Recompilar un objeto

   Se ejecutan dos pasos, porque ninguno alcanza por sí solo:
   - sp_refreshsqlmodule vuelve a analizar el objeto contra el catálogo actual. Falla si una columna
     que usa ya no existe (error 207) o si a una vista le falta una tabla, pero no falla si a un
     procedimiento le falta una tabla: la resolución de nombres diferida lo permite.
   - sp_recompile descarta el plan en caché para que se compile en la siguiente ejecución. Es lo más
     cercano a ALTER ... COMPILE de Oracle, pero NO valida el objeto ni lo repara.
   Por eso, después de esta acción, el backend vuelve a ejecutar 02_objetos_invalidos.sql e informa
   si el objeto sigue teniendo referencias rotas.

   Los dos procedimientos trabajan en la base actual y la conexión del backend está en master, así
   que se invocan a través de la base elegida ({BASE}.sys.sp_...): un procedimiento del sistema
   llamado con el nombre de una base se ejecuta en el contexto de esa base. Pasarle el nombre de
   tres partes desde master falla con el error 15165 ("No se encontró el objeto").

   {BASE} se reemplaza por la base entre corchetes y {OBJETO} por "[esquema].[objeto]", ambos
   formados con identificadores ya validados contra una lista blanca de caracteres (ver
   SqlIdentificadores), por lo que no pueden contener comillas ni cerrar el literal de texto.

   Permiso requerido: ALTER sobre el objeto, otorgado mediante el rol db_ddladmin
   (ver database/scripts/02_permisos_mantenimiento.sql). */
EXEC {BASE}.sys.sp_refreshsqlmodule N'{OBJETO}';
EXEC {BASE}.sys.sp_recompile N'{OBJETO}';
