/*
  Módulo 1 - Estado general de la instancia
  Consulta: Bases de datos administradas por la instancia

  Devuelve una fila por base de datos, ordenadas por nombre:
  - nombre:                nombre de la base de datos.
  - estado:                ONLINE, OFFLINE, RESTORING, RECOVERING, RECOVERY_PENDING,
                           SUSPECT o EMERGENCY. Solo ONLINE acepta conexiones normales.
  - modelo_recuperacion:   FULL, BULK_LOGGED o SIMPLE (define qué respaldos de log son posibles).
  - nivel_compatibilidad:  nivel de compatibilidad (160 = comportamiento de SQL Server 2022).
  - fecha_creacion:        fecha y hora de creación de la base.
  - es_sistema:            1 para las bases del sistema (master, tempdb, model, msdb;
                           database_id <= 4), 0 para las bases de usuario.

  Fuente: vista de catálogo sys.databases.
  Permiso requerido: VIEW ANY DATABASE (lo tiene el rol public de forma predeterminada).
  Sin ese permiso, un login solo ve master, tempdb y las bases de las que es dueño.
*/
SELECT
    name                                                    AS nombre,
    state_desc                                              AS estado,
    recovery_model_desc                                     AS modelo_recuperacion,
    compatibility_level                                     AS nivel_compatibilidad,
    create_date                                             AS fecha_creacion,
    CAST(CASE WHEN database_id <= 4 THEN 1 ELSE 0 END AS BIT) AS es_sistema
FROM sys.databases
ORDER BY name;
