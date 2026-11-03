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

