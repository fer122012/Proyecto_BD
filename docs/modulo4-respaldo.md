# Módulo 4 – Respaldo y recuperación

Revisa si cada base de datos de la instancia tiene respaldos suficientes para recuperarse ante una
falla: cuándo fue el último respaldo completo, diferencial y de log, qué modelo de recuperación usa,
si su situación es adecuada o representa un riesgo de pérdida de datos, el historial de respaldos de
cada base y los respaldos o restauraciones que se están ejecutando en este momento.

- Consultas: `database/queries/modulo4_respaldo/` (01 a 03)
- Código: paquete `cr.ac.una.dbamonitor.respaldo` (controller, service, repository y `dto/`)
- Permisos: `database/scripts/04_permisos_respaldo.sql`
- Pantalla: `frontend/src/modulos/respaldo/`

## Indicadores

| Indicador (campo JSON) | Fuente | Cómo interpretarlo |
|---|---|---|
| Modelo de recuperación (`modeloRecuperacion`) | `sys.databases.recovery_model_desc` | `SIMPLE`: el log se vacía solo y solo se puede restaurar al último completo o diferencial. `FULL` / `BULK_LOGGED`: el log se conserva hasta respaldarlo y permite restaurar a un punto en el tiempo. |
| Último completo, diferencial y de log (`ultimoCompleto`, `ultimoDiferencial`, `ultimoLog`) | `msdb.dbo.backupset` (`type` D, I, L) | Fin del último respaldo de ese tipo que terminó. `null` si nunca hubo uno. Los respaldos `COPY_ONLY` no cuentan porque no forman parte de la cadena de respaldos. |
| Nivel de riesgo (`nivelRiesgo`, `interpretacion`) | Calculado en el backend | Ver la tabla siguiente. |
| Estado de cada respaldo (`estado`, `detalleEstado`) | `is_damaged`, `has_incomplete_metadata`, `has_backup_checksums` | `EXITOSO`, `DANADO` (se respaldó con páginas dañadas, no es confiable) o `INCOMPLETO` (cola del log con metadatos incompletos). |
| Checksum (`conChecksum`) | `has_backup_checksums` | Si se hizo `WITH CHECKSUM`, SQL Server verificó cada página al respaldar. |
| Destino (`destino`) | `msdb.dbo.backupmediafamily.physical_device_name` | Archivo o dispositivo donde quedó el respaldo. |
| Avance (`porcentajeAvance`, `segundosRestantes`) | `sys.dm_exec_requests` | Porcentaje y tiempo estimado que informa SQL Server para un `BACKUP` o `RESTORE` en curso. |

### Criterios de riesgo

| Nivel | Condición | Por qué |
|---|---|---|
| `CRITICO` | La base nunca tuvo un respaldo completo. | Si se pierde o se daña, no hay desde dónde restaurarla. |
| `ATENCION` | El último completo tiene más de 7 días. | Restaurar exige aplicar muchos respaldos posteriores o perder ese periodo. |
| `ATENCION` | Modelo `FULL`/`BULK_LOGGED` sin respaldos de log, o el último tiene más de 24 horas. | Una falla perdería las transacciones de ese periodo, y el log crece sin límite porque solo se libera al respaldarlo. |
| `ADECUADO` | Cualquier otro caso. | La interpretación indica la pérdida máxima esperada. |

Los umbrales están en las constantes `HORAS_MAXIMAS_COMPLETO` y `HORAS_MAXIMAS_LOG` de
`RespaldoService`, y los criterios se prueban en `RespaldoServiceTest`.

### Por qué no se muestran los respaldos fallidos

`msdb.dbo.backupset` solo registra los respaldos que **terminan**. Un `BACKUP` que falla (disco
lleno, ruta inexistente, falta de permisos) queda únicamente en el log de errores de SQL Server.
Por eso "estado" en este módulo distingue entre respaldos terminados confiables y no confiables, y
una base sin respaldos recientes se detecta por la fecha del último exitoso.

## Permisos que necesita este módulo

| Consulta | Permiso |
|---|---|
| Estado de cada base (`sys.databases`) | Permisos de servidor de `01_crear_login_monitor.sql`. |
| Historial (`msdb.dbo.backupset`, `backupmediafamily`) | Un usuario en `msdb` con `SELECT` solo sobre esas dos tablas. |
| En curso (`sys.dm_exec_requests`) | `VIEW SERVER STATE`, ya otorgado por `01_crear_login_monitor.sql`. |

`database/scripts/04_permisos_respaldo.sql` crea el usuario en `msdb` (si no existe por el script
03) y otorga esos dos `SELECT`. No se le da ningún rol de `msdb`: `db_datareader` daría acceso
también a los trabajos, alertas y operadores. Para tener datos que mostrar, ejecutar
`05_generar_historial_respaldos.sql`, que hace respaldos completos, diferenciales y de log reales
de `BASE_MONITOR`.
