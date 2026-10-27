# Módulo 4 – Respaldo y recuperación

Revisa si cada base de datos de la instancia tiene respaldos suficientes para recuperarse ante una
falla: cuándo fue el último respaldo completo, diferencial y de log, qué modelo de recuperación usa,
si su situación es adecuada o representa un riesgo de pérdida de datos, el historial de respaldos de
cada base y los respaldos o restauraciones que se están ejecutando en este momento.

- Consultas: `database/queries/modulo4_respaldo/` (01 a 03)
- Código: paquete `cr.ac.una.dbamonitor.respaldo` (controller, service, repository y `dto/`)
- Permisos: `database/scripts/04_permisos_respaldo.sql`
- Pantalla: `frontend/src/modulos/respaldo/`

## Cobertura del enunciado

| Funcionalidad | Endpoint | Consulta |
|---|---|---|
| Último respaldo de cada tipo por base, modelo de recuperación y nivel de riesgo | `GET /api/respaldos` | `01_estado_respaldos_por_base.sql` |
| Historial de respaldos de una base (tipo, fechas, duración, tamaño, estado, destino) | `GET /api/respaldos/historial?baseDatos=X` | `02_historial_respaldos.sql` |
| Respaldos y restauraciones en ejecución, con su avance | `GET /api/respaldos/en-curso` | `03_respaldos_en_curso.sql` |

Todo el historial de respaldos de la instancia está en `msdb`, así que la conexión del backend a
`master` alcanza. En el historial, el nombre de la base es un **valor** del `WHERE` y se pasa como
parámetro (`?`), no se inserta en el texto de la consulta. Igual que en los demás módulos, antes se
valida con `SqlIdentificadores` (un nombre con caracteres no permitidos devuelve un 400).

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

## Preguntas probables en la defensa

**¿Qué diferencia hay entre un respaldo completo, uno diferencial y uno de log?**
El completo copia toda la base y es el punto de partida de cualquier restauración. El diferencial
copia solo las extensiones que cambiaron desde el último completo, así que es más pequeño y rápido.
El de log copia los registros de transacciones desde el último respaldo de log; solo existe en los
modelos `FULL` y `BULK_LOGGED`.

**¿Cómo se restauraría `BASE_MONITOR` a las 10:30?**
Restaurar el último completo anterior a esa hora `WITH NORECOVERY`, luego el último diferencial
posterior a ese completo (si hay) y después, en orden, cada respaldo de log, el último con
`STOPAT = '... 10:30'` y `WITH RECOVERY`.

**¿Por qué una base en modelo FULL sin respaldos de log es un riesgo?**
Porque en ese modelo el log solo se libera al respaldarlo: crece hasta llenar el disco. Además, sin
respaldos de log no se aprovecha la ventaja del modelo, que es restaurar a un punto en el tiempo.

**¿Por qué las bases del sistema salen como críticas?**
Porque nunca se respaldaron. `master` guarda los logins y la configuración de la instancia, y `msdb`
el historial de respaldos y los trabajos; perderlas obliga a reconstruir todo eso a mano. `tempdb`
se excluye porque se recrea en cada inicio y no se puede respaldar.

**¿Qué es un respaldo COPY_ONLY?**
Un respaldo fuera de la cadena: no sirve de base a los diferenciales ni corta el log. Se usa para
sacar una copia puntual sin alterar la estrategia de respaldos. Por eso no cuenta como "último".

**¿Qué aporta `WITH CHECKSUM`?**
SQL Server verifica el checksum de cada página al leerla y guarda uno del respaldo completo. Si una
página está dañada, el respaldo falla en lugar de copiar datos corruptos, y al restaurar o con
`RESTORE VERIFYONLY` se detecta si el archivo de respaldo se dañó.

**¿Por qué no se programan los respaldos desde la herramienta?**
SQL Server Express no tiene SQL Server Agent. En producción se programarían con trabajos del Agent o
con el Programador de tareas de Windows ejecutando `sqlcmd` con un script como el 05.
