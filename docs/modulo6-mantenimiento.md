# Módulo 6 – Mantenimiento preventivo

A diferencia de los módulos 1, 2, 3, 4 y 5 (que solo **leen** información), este módulo también
**ejecuta** dos acciones administrativas sobre la base de datos de práctica: recalcular
estadísticas y forzar la recompilación de un objeto. Por eso su diseño incluye una capa extra de
validación que los demás módulos no necesitan.

- Consultas y plantillas de acción: `database/queries/modulo6_mantenimiento/` (01 a 04)
- Código: paquete `cr.ac.una.dbamonitor.mantenimiento` (controller, service, repository y `dto/`)
- Permisos adicionales: `database/scripts/02_permisos_mantenimiento.sql`
- Endpoints:
  - `GET /api/mantenimiento/estadisticas?baseDatos=...`: estado de las estadísticas de las tablas
  - `GET /api/mantenimiento/objetos-invalidos?baseDatos=...`: objetos candidatos a recompilar
  - `POST /api/mantenimiento/estadisticas/recalcular`: ejecuta `UPDATE STATISTICS`
  - `POST /api/mantenimiento/objetos/recompilar`: ejecuta `sp_recompile`

## Por qué este módulo recibe `baseDatos` en cada solicitud

La conexión del backend (`application-local.properties`) apunta siempre a `master`. Eso fue
suficiente para los módulos 1, 2 y 5 porque sus datos (estado de la instancia, sesiones,
logins) son a nivel de *servidor*, visibles desde cualquier base. Pero las estadísticas de una
tabla y las dependencias de un objeto son propias de *cada base de datos*. Para no obligar a
reconfigurar y reiniciar el backend cada vez que se quiera trabajar con una base distinta, cada
solicitud de este módulo indica con qué base de práctica trabajar (por ejemplo
`AdventureWorks`), y el backend arma la consulta o la acción usando el nombre calificado de tres
partes (`base.esquema.objeto`), sin necesidad de cambiar la base de datos activa de la conexión.

## Indicadores

| Indicador (endpoint) | Fuente | Cómo interpretarlo |
|---|---|---|
| Estadísticas (`/estadisticas`) | `sys.dm_db_stats_properties` | Por cada estadística de cada tabla de usuario: cuándo se actualizó por última vez (`ultimaActualizacion`), cuántas filas tiene (`filas`) y cuántas han cambiado desde la última actualización (`filasModificadas`). La lista llega ordenada con las más desactualizadas primero (más cambios sin reflejar, luego más antiguas), para priorizar sobre cuáles conviene recalcular. |
| Objetos candidatos (`/objetos-invalidos`) | `sys.sql_expression_dependencies` | Mismo criterio que el Módulo 5 (ver `docs/modulo5-auditoria.md`), aplicado sobre la base de práctica indicada. Son los objetos que conviene recompilar para confirmar si de verdad tienen un error. |

## Las dos acciones administrativas

### Recalcular estadísticas — `UPDATE STATISTICS ... WITH FULLSCAN`

El optimizador de consultas usa estadísticas (histogramas de distribución de valores) para
decidir el plan de ejecución de cada consulta. Si una tabla cambia mucho (muchos `INSERT`,
`UPDATE` o `DELETE`) y sus estadísticas quedan desactualizadas, el optimizador puede subestimar
o sobrestimar cuántas filas va a tocar una consulta y elegir un plan lento. `WITH FULLSCAN` lee
el 100 % de las filas de la tabla en vez de una muestra: es más exacto y más costoso, aceptable
para el tamaño de una base de práctica de este proyecto.

```
Antes:  Ventas (10 000 filas, estadística con fecha de hace 3 meses, 40 % de filas modificadas)
                 ↓  POST /api/mantenimiento/estadisticas/recalcular
                    { "baseDatos": "AdventureWorks", "esquema": "Sales", "tabla": "SalesOrderHeader" }
Después: Ventas (estadística con fecha de hoy, 0 % de filas modificadas desde el recálculo)
```

### Recompilar un objeto — `sp_recompile`

`sp_recompile` marca un procedimiento, función o disparador para que SQL Server descarte su plan
en caché y lo compile de nuevo en la siguiente ejecución. Es el equivalente en SQL Server a
`ALTER ... COMPILE` de Oracle: **no "repara" el objeto**, pero sí lo recompila, lo que obliga a
SQL Server a volver a resolver sus referencias de inmediato (en vez de esperar a que alguien lo
ejecute) y revela cualquier error de compilación pendiente, como los detectados en
`/objetos-invalidos`.

## Seguridad: por qué el nombre de tabla u objeto no es un parámetro de consulta

En una consulta normal, un valor como `WHERE nombre = ?` se envía por separado del texto SQL, y
el controlador JDBC lo escapa automáticamente. Pero `UPDATE STATISTICS <tabla>` y
`sp_recompile '<objeto>'` necesitan el **nombre del objeto** dentro del propio comando: T-SQL no
tiene forma de parametrizar un nombre de tabla con `?`. Por eso este módulo valida cada
identificador (base, esquema, tabla, objeto) con `SqlIdentificadores.validar(...)` **antes** de
construir el comando:

- Solo se aceptan letras, números, guion bajo, `#` y `$` (los caracteres válidos de un
  identificador de SQL Server sin comillas). Cualquier otro caracter —incluidas comillas,
  espacios o `;`— se rechaza con un error 400 antes de tocar la base de datos.
- Los identificadores ya validados se envuelven entre corchetes (`[AdventureWorks]`), igual que
  hace `QUOTENAME` de SQL Server, antes de insertarlos en la plantilla SQL.

Esto hace que la inyección SQL sea imposible en esta posición, sin depender de "escapar"
comillas (un enfoque frágil) ni de construir el SQL a mano en cada llamada.

## Cómo se confirma el resultado al usuario

Las dos acciones devuelven siempre un `ResultadoAccionDto` (`exito`, `mensaje`, `objeto`,
`fecha`), nunca dejan que una falla de SQL Server se propague como un error 500 genérico: el
`MantenimientoService` captura la excepción, registra el detalle técnico en el log y devuelve
`exito = false` con un mensaje en español que explica qué revisar (por ejemplo, que la tabla
exista o que el login tenga el permiso `ALTER`).

```json
// Éxito
{ "exito": true,  "mensaje": "Estadísticas recalculadas correctamente.",
  "objeto": "AdventureWorks.Sales.SalesOrderHeader", "fecha": "2026-10-11T09:30:00" }

// Error controlado (no un 500)
{ "exito": false, "mensaje": "No se pudieron recalcular las estadísticas. Verifique que la tabla
   exista y que el usuario de la aplicación tenga el permiso ALTER sobre ella (rol db_ddladmin).",
  "objeto": "AdventureWorks.Sales.TablaQueNoExiste", "fecha": "2026-10-11T09:31:00" }
```

## Permisos que necesita este módulo

A diferencia de los demás módulos (que solo necesitan permisos de **servidor**, de solo
lectura), este módulo ejecuta DDL dentro de una base de datos específica y necesita el permiso
`ALTER` sobre sus tablas y objetos. Por eso existe
`database/scripts/02_permisos_mantenimiento.sql`, que:

1. Crea el *usuario* `dba_monitor` dentro de la base de práctica (el *login* ya existe a nivel
   de servidor desde `01_crear_login_monitor.sql`; un login necesita un usuario distinto en cada
   base donde vaya a operar).
2. Agrega ese usuario al rol `db_ddladmin`, que permite ejecutar DDL y comandos de mantenimiento
   sin dar acceso a los datos (a diferencia de `db_owner`, que sí lo daría). Esto respeta el
   principio de mínimo privilegio: la aplicación puede mantener la base, pero no leer ni
   modificar su contenido.

Este script debe ejecutarse una vez por cada base de práctica que el grupo use para la
demostración del Módulo 6.

## Preguntas probables en la defensa

**¿Por qué este módulo pide la base de datos en cada solicitud y los demás no?**
Porque la conexión JDBC del backend es fija (a `master`), y los indicadores de los módulos 1, 2
y 5 son de servidor. Las estadísticas y las dependencias de objetos son propias de cada base,
así que hace falta indicar con cuál trabajar.

**¿Qué pasa si recompilo un objeto que no tiene ningún error?**
Nada grave: `sp_recompile` solo descarta el plan en caché. La próxima ejecución del objeto
tarda un poco más (porque vuelve a compilar), pero no cambia su comportamiento.

**¿`UPDATE STATISTICS` bloquea la tabla?**
Toma un bloqueo breve para leer los datos y actualizar los metadatos de la estadística; no
bloquea lecturas ni escrituras normales de forma prolongada, aunque sí consume recursos de E/S y
CPU mientras se ejecuta, más con `WITH FULLSCAN` que con una muestra.

**¿Por qué no se usa `sp_updatestats` para toda la base de una vez?**
Porque la rúbrica pide poder actuar sobre un objeto identificable, con confirmación del
resultado de esa acción puntual. `sp_updatestats` actualiza todas las estadísticas desactualizadas
de la base en un solo paso y sería una buena mejora adicional, pero no permitiría mostrar la
confirmación objeto por objeto que pide este módulo.

**¿Cómo se evita la inyección SQL si el nombre de la tabla va dentro del comando?**
Validando el identificador contra una lista blanca de caracteres antes de usarlo (ver la
sección de seguridad arriba), no intentando escapar caracteres peligrosos.
