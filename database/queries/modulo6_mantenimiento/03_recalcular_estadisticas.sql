/* Módulo 6 - Mantenimiento Preventivo | Acción: Recalcular estadísticas de una tabla

   UPDATE STATISTICS vuelve a leer los datos de la tabla y reconstruye sus estadísticas. WITH
   FULLSCAN lee el 100 % de las filas (más preciso, más costoso en tablas grandes) en vez de
   tomar una muestra, lo cual es aceptable para una base de práctica de este proyecto.

   {BASE}, {ESQUEMA} y {TABLA} se reemplazan en el backend por los valores que eligió el
   usuario, ya validados contra una lista blanca de caracteres (ver SqlIdentificadores) y
   escritos entre corchetes, porque T-SQL no permite enviar un nombre de tabla como parámetro
   de consulta (?): la sintaxis de UPDATE STATISTICS exige el nombre del objeto en el propio
   texto del comando.

   Permiso requerido: ALTER sobre la tabla, otorgado mediante el rol db_ddladmin
   (ver database/scripts/02_permisos_mantenimiento.sql). */
UPDATE STATISTICS {BASE}.{ESQUEMA}.{TABLA} WITH FULLSCAN;
