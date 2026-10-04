/*
  Módulo 1 - Estado general de la instancia
  Consulta: Inicio y tiempo de actividad de la instancia

  Devuelve una sola fila:
  - fecha_inicio:     fecha y hora en que se inició el servicio de SQL Server
                      (hora local del servidor).
  - segundos_activa:  segundos transcurridos desde el inicio hasta este momento (SYSDATETIME()).

  Fuente: sys.dm_os_sys_info (columna sqlserver_start_time).
  Ambas fechas están en la hora local del servidor, por lo que la diferencia es correcta.
  El backend convierte los segundos a un texto legible (días, horas y minutos).
  Permiso requerido: VIEW SERVER STATE
*/
SELECT
    sqlserver_start_time                                   AS fecha_inicio,
    DATEDIFF(SECOND, sqlserver_start_time, SYSDATETIME())  AS segundos_activa
FROM sys.dm_os_sys_info;
