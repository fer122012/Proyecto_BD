# Módulo 1 – Estado general de la instancia

Da al DBA una visión rápida de la instancia de SQL Server monitoreada: qué es, desde cuándo
está activa, cómo está usando la memoria y si sus bases de datos están disponibles.

- Consultas: `database/queries/modulo1_instancia/` (01 a 04)
- Código: paquete `cr.ac.una.dbamonitor.instancia` (controller, service, repository y `dto/`)
- Endpoints:
  - `GET /api/instancia`: resumen de la instancia
  - `GET /api/instancia/bases-de-datos`: lista de bases de datos

El backend lee las consultas directamente de los archivos `.sql` documentados (Maven los copia
al classpath bajo `sql/`), así que el SQL que se ejecuta es exactamente el que aparece aquí.

## Indicadores

| Indicador (campo JSON) | Fuente | Cómo interpretarlo |
|---|---|---|
| Servidor (`servidor`) | `SERVERPROPERTY('MachineName')` | Equipo (máquina) donde está instalado SQL Server. |
| Nombre de conexión (`nombreConexion`) | `SERVERPROPERTY('ServerName')` | Lo que se escribe para conectarse: `EQUIPO` o `EQUIPO\INSTANCIA`. |
| Instancia (`instancia`) | `SERVERPROPERTY('InstanceName')` | Nombre de la instancia. Si es NULL, es la instancia predeterminada `MSSQLSERVER`. En Express suele ser `SQLEXPRESS`. |
| Tipo y versión (`tipoGestor`, `versionComercial`, `version`, `nivel`) | `SERVERPROPERTY('ProductVersion')`, `SERVERPROPERTY('ProductLevel')` | La versión principal indica el producto: 16 = SQL Server 2022. El nivel es RTM, SPn o CTPn. |
| Edición (`edicion`) | `SERVERPROPERTY('Edition')`, `SERVERPROPERTY('EngineEdition')` | Edición instalada. `EngineEdition = 4` es Express, y activa la lista `limitacionesEdicion`. |
| Versión completa (`versionCompleta`) | `@@VERSION` | Texto completo con la compilación y el sistema operativo. |
| Estado (`estado`, `basesConAlerta`) | Calculado en el backend a partir de `sys.databases` | `EN_LINEA`: la instancia respondió y todas las bases están ONLINE. `ATENCION`: alguna base no está ONLINE; `basesConAlerta` dice cuál, su estado y qué significa. Si la instancia no responde, la API devuelve un error 503 con un mensaje claro. |
| Fecha y hora de inicio (`fechaInicio`) | `sys.dm_os_sys_info.sqlserver_start_time` | Último arranque del servicio, en la hora local del servidor. Las estadísticas de las DMVs (por ejemplo las del Módulo 2) se acumulan desde ese momento. |
| Tiempo de actividad (`tiempoActividadSegundos`, `tiempoActividadTexto`) | `DATEDIFF(SECOND, sqlserver_start_time, SYSDATETIME())` | Cuánto tiempo lleva la instancia sin reiniciarse, por ejemplo "3 días, 4 h, 12 min". |
| Memoria asignada (`memoria.asignadaMb`) | `sys.dm_os_sys_info.committed_kb` | Memoria que el administrador de memoria de SQL Server tiene reservada y confirmada. |
| Memoria objetivo (`memoria.objetivoMb`) | `sys.dm_os_sys_info.committed_target_kb` | Memoria que SQL Server considera que puede usar, según la RAM libre, `max server memory` y los límites de la edición. |
| Memoria en uso (`memoria.enUsoMb`) | `sys.dm_os_process_memory.physical_memory_in_use_kb` | Memoria física que el proceso realmente ocupa, incluidas asignaciones fuera del buffer pool. |
| Porcentaje de uso (`memoria.porcentajeUso`) | Calculado: en uso / objetivo × 100 | Qué parte de la memoria que SQL Server puede usar ya está ocupada. Puede superar el 100 % porque "en uso" incluye memoria que no cuenta en el objetivo. |
| Máximo configurado (`memoria.maximaConfiguradaMb`) | `sys.configurations`, `'max server memory (MB)'` | Límite configurado por el DBA. 2147483647 (el valor predeterminado) se muestra como "Sin límite". |
| Bases de datos (`/bases-de-datos`) | `sys.databases` | Nombre, estado, modelo de recuperación, nivel de compatibilidad, fecha de creación y si es base del sistema (`database_id <= 4`: master, tempdb, model, msdb). |

### Memoria asignada vs. memoria usada

SQL Server no libera memoria apenas deja de necesitarla. La guarda en caché (sobre todo en el
buffer pool, donde mantiene páginas de datos) para no tener que volver a leer del disco. Por eso
es **normal** que la memoria asignada y la usada sean altas y crezcan con el tiempo hasta acercarse
al objetivo: eso no es una fuga ni un problema. Lo que sí merece atención es:

- que la memoria objetivo sea muy baja respecto a la RAM del equipo, porque indica presión de
  memoria del sistema operativo o un `max server memory` muy restrictivo;
- que `max server memory` esté "Sin límite" en un servidor compartido con otras aplicaciones,
  porque SQL Server puede quitarles memoria.

## Limitaciones de SQL Server Express

La instancia de desarrollo es SQL Server 2022 Express. Cuando `EngineEdition = 4`, la API devuelve
estas limitaciones en `limitacionesEdicion` para mostrarlas junto a los indicadores:

- **Memoria:** el buffer pool está limitado a 1410 MB por instancia. La memoria total del proceso
  puede ser algo mayor, porque otras áreas de memoria no cuentan en ese límite. Por eso, en
  Express, la memoria objetivo nunca va a acercarse a la RAM total del equipo.
- **SQL Server Agent:** no está incluido. No se pueden programar jobs, alertas ni operadores.
  Esto afecta a los módulos de respaldo y mantenimiento, no a este módulo.
- **Tamaño:** 10 GB de datos como máximo por base de datos.
- **CPU:** usa como máximo el menor entre 1 socket o 4 núcleos.

## Preguntas probables en la defensa

**¿Cuál es la diferencia entre servidor, instancia y base de datos?**
El servidor es el equipo (físico o virtual) donde está instalado SQL Server. Una instancia es
una instalación independiente del motor, con su propio servicio, memoria, configuración y logins.
Un mismo servidor puede tener varias instancias (por ejemplo la predeterminada `MSSQLSERVER` y
una con nombre como `SQLEXPRESS`). Una base de datos es un conjunto de objetos y datos que vive
dentro de una instancia, y cada instancia administra varias bases.

**¿Qué permisos necesita la aplicación?**
`VIEW SERVER STATE`, para leer las DMVs `sys.dm_os_sys_info` y `sys.dm_os_process_memory`.
`SERVERPROPERTY`, `@@VERSION` y `sys.configurations` no requieren permisos especiales.
`sys.databases` muestra todas las bases gracias a `VIEW ANY DATABASE`, que el rol `public` tiene
de forma predeterminada. El login `dba_monitor` se crea con `database/scripts/01_crear_login_monitor.sql`
y **no** es sysadmin: tiene solo lo necesario (principio de mínimo privilegio).

**¿Cómo se calcula el estado de la instancia?**
Si las consultas responden, la instancia está en ejecución. Luego se revisa `state_desc` de cada
base en `sys.databases`: si todas están ONLINE el estado es `EN_LINEA`. Si alguna está OFFLINE,
RESTORING, RECOVERY_PENDING, SUSPECT u otro estado, el estado es `ATENCION` y se indica cuál y por
qué. Si no hay conexión, el manejador global de errores devuelve un 503 con un mensaje en español.

**¿Por qué el tiempo de actividad importa?**
Porque las estadísticas acumuladas de las DMVs se reinician cuando la instancia se reinicia. Un
tiempo de actividad corto significa que los datos de rendimiento cubren poco tiempo.

**¿Por qué la memoria en uso es tan alta si no hay casi actividad?**
Porque SQL Server mantiene datos en caché a propósito y solo libera memoria si el sistema
operativo la necesita. Para controlar cuánto toma se usa `max server memory`.

**¿Cómo se configura la instancia monitoreada?**
Con `spring.datasource.url` en `application-local.properties`, sin modificar el código. Para
monitorear otra instancia se cambia el servidor o la instancia en esa URL.
