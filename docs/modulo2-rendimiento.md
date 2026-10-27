# Módulo 2 – Monitoreo de rendimiento

Ayuda a identificar problemas de rendimiento de la instancia: qué sentencias consumen más CPU,
cuáles tardan más en promedio, quién está conectado, qué sesiones están bloqueadas y qué procesos
consumen más CPU o memoria en este momento.

- Consultas: `database/queries/modulo2_rendimiento/` (01 a 05)
- Código: paquete `cr.ac.una.dbamonitor.rendimiento` (controller, service, repository y `dto/`)
- Permisos: `VIEW SERVER STATE`, ya otorgado por `database/scripts/01_crear_login_monitor.sql`
- Pantalla: `frontend/src/modulos/rendimiento/`
- Generar datos: `database/scripts/06_generar_carga_rendimiento.sql` y
  `database/scripts/prueba_sesiones_bloqueadas.sql`

## Cobertura del enunciado

| Funcionalidad del enunciado | Endpoint | Consulta |
|---|---|---|
| Consultas SQL con mayor consumo de recursos | `GET /api/rendimiento/consultas-consumo` | `01_top_consultas_consumo.sql` |
| Tiempo promedio de ejecución de consultas | `GET /api/rendimiento/tiempo-promedio` | `02_tiempo_promedio_consultas.sql` |
| Sesiones activas | `GET /api/rendimiento/sesiones` | `03_sesiones_activas.sql` |
| Sesiones bloqueadas (si existen) | `GET /api/rendimiento/bloqueos` | `04_sesiones_bloqueadas.sql` |
| Procesos que consumen mayor cantidad de CPU o memoria | `GET /api/rendimiento/procesos?orden=cpu` o `orden=memoria` | `05_procesos_cpu_memoria.sql` |

Todo se consulta para la instancia completa: las DMVs de rendimiento no dependen de una base, así
que ninguna ruta recibe `baseDatos`. Un `orden` distinto de `cpu` o `memoria` devuelve un 400.

## Indicadores

| Indicador (campo JSON) | Fuente | Cómo interpretarlo |
|---|---|---|
| CPU, duración, lecturas y escrituras totales (`cpuTotalMs`, `duracionTotalMs`, …) | `sys.dm_exec_query_stats` | Acumulados de cada sentencia desde que su plan entró en la caché. Una sentencia barata pero muy frecuente puede quedar arriba: importa el total, no solo cada ejecución. |
| Duración y CPU promedio (`duracionPromedioMs`, `cpuPromedioMs`) | Total / `execution_count` | Cuánto tarda cada ejecución. Si la duración es mucho mayor que el CPU, la sentencia pasa tiempo esperando (bloqueos, disco). Una diferencia grande entre `duracionMinMs` y `duracionMaxMs` indica un rendimiento irregular. |
| Base de datos (`baseDatos`) | `dm_exec_sql_text.dbid` o, si es NULL, el atributo `dbid` del plan | Las sentencias que envía una aplicación (ad hoc o con `sp_executesql`) no traen `dbid` en el texto; el plan sí guarda la base en la que se compiló. |
| Sesiones activas (`/sesiones`) | `sys.dm_exec_sessions` + `sys.dm_exec_requests` | Sesiones de usuario conectadas. Si están ejecutando algo, se ve el estado de la solicitud, el comando y el tipo de espera. |
| Sesión bloqueada y bloqueadora | `dm_exec_requests.blocking_session_id <> 0` | La sentencia de la bloqueadora sale de `dm_exec_connections.most_recent_sql_handle`, porque casi siempre la bloqueadora está inactiva con una transacción abierta y no tiene solicitud en curso. |
| Estado del bloqueo (`estado`, `interpretacion`) | Calculado en el backend | `ATENCION` si la espera es menor a 30 s (puede ser una contención breve) y `CRITICO` desde 30 s (suele ser una transacción sin confirmar; se sugiere `KILL`). |
| Sesión raíz (`mensaje`) | Calculado en el backend | La que bloquea a otras sin estar bloqueada. Es la que hay que revisar para liberar toda la cadena. |
| CPU del proceso (`cpuMs`, `porcentajeCpu`) | `dm_exec_sessions.cpu_time` + `dm_exec_requests.cpu_time` | `dm_exec_sessions` solo suma el CPU de una solicitud cuando termina; sin sumar la solicitud en curso, un proceso que está consumiendo ahora aparecería con 0 ms. El porcentaje es sobre los procesos listados. |
| Memoria (`memoriaSesionKb`, `memoriaConcedidaKb`) | `dm_exec_sessions.memory_usage` (páginas de 8 KB) y `dm_exec_query_memory_grants` | La de la sesión es pequeña y casi fija. La concedida es la memoria de trabajo (ordenamientos, hashes) de la consulta que corre ahora; es la que crece en consultas pesadas. |

Los procesos sin consumo que no ejecutan nada se omiten: las tareas internas de SQL Server
siempre tienen una solicitud en estado `background` o `sleeping` y llenarían la tabla de ceros.

## Cómo generar datos para la demostración

En una instancia de práctica las DMVs están casi vacías, así que hay que generar actividad:

```
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\06_generar_carga_rendimiento.sql -o NUL
```

El script ejecuta sobre `BASE_MONITOR` reportes de ventas, búsquedas y actualizaciones típicas
del caso de estudio, varias veces y con `sp_executesql`, como lo haría una aplicación. Tarda
alrededor de 20 s. Para ver varias sesiones y procesos a la vez en la pantalla, ejecutarlo en 2 o 3
terminales al mismo tiempo y activar *Actualizar cada 15 s*. Las actualizaciones de existencias se
revierten, así que la base queda igual.

Para los bloqueos, seguir los pasos de `database/scripts/prueba_sesiones_bloqueadas.sql` (tres
ventanas en SSMS: una deja una transacción abierta, otra queda esperando y la pantalla muestra el
bloqueo).

### AUTO_CLOSE y la caché de planes

SQL Server Express crea las bases con `AUTO_CLOSE` activado. Cuando la última sesión de una base
se desconecta, la base se cierra y SQL Server **vacía la caché de planes de toda la instancia**, así
que las secciones de consultas con mayor consumo y tiempo promedio quedan vacías. El script 06
desactiva `AUTO_CLOSE` en `BASE_MONITOR`. Si otras bases de la instancia lo tienen activado,
todavía pueden vaciar la caché de vez en cuando; se puede revisar con:

```sql
SELECT name FROM sys.databases WHERE is_auto_close_on = 1;
```

### Poca memoria libre en el equipo

Si Windows se queda con poca RAM libre, SQL Server reduce su caché de planes para devolverle
memoria al sistema y las estadísticas de las consultas duran muy poco (en las pruebas, con 575 MB
libres de 16 GB, la caché quedó en 1 MB y solo conservaba una o dos sentencias). Se puede revisar con:

```sql
SELECT available_physical_memory_kb / 1024 AS ram_libre_mb, system_memory_state_desc
FROM sys.dm_os_sys_memory;
```

Para la demostración: cerrar programas que no se usen, ejecutar el script 06 y mostrar las
secciones de consultas justo después. Las secciones de sesiones, bloqueos y procesos no dependen de
la caché, así que no se ven afectadas.

### Lo que muestra la carga

Con la carga, la consulta que más CPU acumula es la más sencilla: el detalle de una factura por su
número (`WHERE FacturaId = @factura`), ejecutada 50 veces por vuelta. `ventas.DetalleFactura` no
tiene índice sobre `FacturaId`, así que cada ejecución recorre las 200 000 filas. Es un buen
ejemplo para la presentación: la solución es un índice, no más hardware.

```sql
CREATE INDEX IX_DetalleFactura_FacturaId ON ventas.DetalleFactura (FacturaId);
```

La más lenta en promedio es el reporte de ventas por provincia (une 200 000 detalles y agrupa), y
la búsqueda de clientes con `LIKE '%…'` y el filtro `YEAR(FechaFactura) = @anio` muestran
recorridos completos porque no pueden usar un índice.

## Preguntas probables en la defensa

**¿De dónde salen los datos de consumo de las consultas?**
De `sys.dm_exec_query_stats`, que guarda estadísticas por cada sentencia que tiene un plan en la
caché. No es un historial: si la instancia se reinicia o la caché se vacía, se pierden.

**¿Por qué una sentencia que tarda 10 ms puede ser la que más CPU consume?**
Porque el total es tiempo × ejecuciones. Una sentencia de 10 ms ejecutada 500 veces consume más
que una de 300 ms ejecutada 9 veces. Para optimizar, conviene empezar por las de mayor total.

**¿Qué diferencia hay entre duración y CPU?**
La duración es el tiempo real; el CPU es el tiempo que el procesador trabajó en la sentencia. Si la
duración es mucho mayor, la sentencia estuvo esperando: bloqueos, lecturas de disco o red.

**¿Qué es un bloqueo y cuándo es un problema?**
Una sesión que modifica datos los bloquea hasta confirmar o revertir su transacción; otra que
quiera leerlos o modificarlos espera (`LCK_M_S`, `LCK_M_X`, …). Es normal que dure milisegundos.
Es un problema cuando dura mucho, casi siempre porque una transacción quedó abierta.

**¿Qué se hace con un bloqueo crítico?**
Identificar la sesión raíz, ver qué está haciendo y, si es una transacción olvidada, terminarla con
`KILL <id>`. SQL Server revierte su transacción y las sesiones bloqueadas continúan.

**¿Un bloqueo es lo mismo que un deadlock?**
No. En un bloqueo una sesión espera a otra y se resuelve cuando la primera termina. En un deadlock
dos sesiones se esperan mutuamente; SQL Server lo detecta solo y cancela una de ellas (error 1205).

**¿Por qué los procesos del sistema aparecen como "Sistema"?**
Son tareas internas de SQL Server (escritura del log, checkpoint, monitor de bloqueos…). Se
muestran porque también pueden consumir recursos, pero solo si consumen o están trabajando.
