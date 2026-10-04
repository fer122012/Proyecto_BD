/*
  Módulo 1 - Estado general de la instancia
  Consulta: Memoria de la instancia

  Devuelve una sola fila (valores en MB; las vistas los entregan en KB y se dividen entre 1024):
  - memoria_asignada_mb:            memoria que el administrador de memoria de SQL Server tiene
                                    reservada y confirmada (committed) en este momento.
  - memoria_objetivo_mb:            memoria que SQL Server considera que puede llegar a usar
                                    (committed target). Depende de la RAM del equipo, de
                                    'max server memory' y de los límites de la edición.
  - memoria_en_uso_mb:              memoria física que el proceso de SQL Server está usando
                                    (working set), incluidas asignaciones fuera del buffer pool.
  - memoria_maxima_configurada_mb:  valor efectivo de 'max server memory (MB)'.
                                    2147483647 significa "sin límite" (valor predeterminado).

  Fuentes: sys.dm_os_sys_info (committed_kb, committed_target_kb),
           sys.dm_os_process_memory (physical_memory_in_use_kb) y
           sys.configurations ('max server memory (MB)', columna value_in_use).
  Cada fuente devuelve una sola fila, por eso se combinan con CROSS JOIN.
  value_in_use es sql_variant y se convierte a BIGINT.
  Permiso requerido: VIEW SERVER STATE (sys.configurations es visible para cualquier login).
*/
SELECT
    si.committed_kb / 1024                 AS memoria_asignada_mb,
    si.committed_target_kb / 1024          AS memoria_objetivo_mb,
    pm.physical_memory_in_use_kb / 1024    AS memoria_en_uso_mb,
    CAST(cfg.value_in_use AS BIGINT)       AS memoria_maxima_configurada_mb
FROM sys.dm_os_sys_info AS si
CROSS JOIN sys.dm_os_process_memory AS pm
CROSS JOIN sys.configurations AS cfg
WHERE cfg.name = N'max server memory (MB)';
