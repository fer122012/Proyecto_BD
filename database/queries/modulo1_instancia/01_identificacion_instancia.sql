/*
  Módulo 1 - Estado general de la instancia
  Consulta: Identificación de la instancia

  Devuelve una sola fila con los datos que identifican al servidor y a la instancia:
  - nombre_equipo:      equipo (máquina Windows) donde está instalado SQL Server.
  - nombre_servidor:    nombre con el que se conecta a la instancia (EQUIPO o EQUIPO\INSTANCIA).
  - nombre_instancia:   nombre de la instancia; es NULL cuando es la instancia predeterminada
                        (MSSQLSERVER).
  - version_producto:   versión del motor, por ejemplo 16.0.xxxx (16 = SQL Server 2022).
  - nivel_producto:     nivel de actualización: RTM, SPn o CTPn.
  - edicion:            edición instalada, por ejemplo "Express Edition (64-bit)".
  - edicion_motor:      código numérico de la edición del motor (2 = Standard,
                        3 = Enterprise/Developer, 4 = Express).
  - version_completa:   texto completo de @@VERSION (versión, compilación y sistema operativo).

  Fuente: función SERVERPROPERTY y variable @@VERSION.
  SERVERPROPERTY devuelve sql_variant; se convierte a NVARCHAR/INT para leerlo desde JDBC.
  Permiso requerido: ninguno especial (cualquier login puede ejecutarla).
*/
SELECT
    CAST(SERVERPROPERTY('MachineName')    AS NVARCHAR(128)) AS nombre_equipo,
    CAST(SERVERPROPERTY('ServerName')     AS NVARCHAR(256)) AS nombre_servidor,
    CAST(SERVERPROPERTY('InstanceName')   AS NVARCHAR(128)) AS nombre_instancia,
    CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(128)) AS version_producto,
    CAST(SERVERPROPERTY('ProductLevel')   AS NVARCHAR(128)) AS nivel_producto,
    CAST(SERVERPROPERTY('Edition')        AS NVARCHAR(128)) AS edicion,
    CAST(SERVERPROPERTY('EngineEdition')  AS INT)           AS edicion_motor,
    @@VERSION                                               AS version_completa;
