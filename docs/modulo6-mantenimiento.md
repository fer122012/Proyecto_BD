# Módulo 6 – Mantenimiento preventivo

A diferencia de los módulos 1, 2, 3, 4 y 5 (que solo **leen** información), este módulo también
**ejecuta** acciones administrativas sobre la base de datos de práctica: recalcular
estadísticas, recompilar un objeto y validar todos los objetos de la base. Por eso su diseño incluye una capa extra de
validación que los demás módulos no necesitan.

- Consultas y plantillas de acción: `database/queries/modulo6_mantenimiento/` (01 a 05)
- Código: paquete `cr.ac.una.dbamonitor.mantenimiento` (controller, service, repository y `dto/`)
- Permisos adicionales: `database/scripts/02_permisos_mantenimiento.sql`
- Endpoints:
  - `GET /api/mantenimiento/estadisticas?baseDatos=...`: estado de las estadísticas de las tablas
  - `GET /api/mantenimiento/objetos-invalidos?baseDatos=...`: objetos candidatos a recompilar
  - `POST /api/mantenimiento/estadisticas/recalcular`: ejecuta `UPDATE STATISTICS`
  - `POST /api/mantenimiento/objetos/recompilar`: ejecuta `sp_refreshsqlmodule` y `sp_recompile`, y
    verifica si el objeto sigue con referencias rotas
  - `POST /api/mantenimiento/objetos/validar?baseDatos=...`: busca errores de compilación en toda la
    base sin dejar cambios

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
| Estadísticas (`/estadisticas`) | `sys.dm_db_stats_properties` | Por cada estadística de cada tabla de usuario: cuándo se actualizó por última vez (`ultimaActualizacion`), cuántas filas tiene (`filas`) y cuántas han cambiado desde la última actualización (`filasModificadas`). La lista llega ordenada con las más desactualizadas primero (más cambios sin reflejar, luego más antiguas), para priorizar sobre cuáles conviene recalcular. Si `ultimaActualizacion` es `null`, SQL Server **nunca calculó** esa estadística (por ejemplo, un índice creado sobre una tabla vacía que luego se llenó): aparece primero, como "Nunca", y se recomienda recalcularla. |
| Objetos candidatos (`/objetos-invalidos`) | `sys.sql_expression_dependencies` | Mismo criterio que el Módulo 5 (ver `docs/modulo5-auditoria.md`), aplicado sobre la base de práctica indicada. Son los objetos a los que les falta una tabla u otro objeto de la misma base. Requiere `SELECT` sobre esa vista dentro de la base (`02_permisos_mantenimiento.sql`). |

Las dos consultas obtienen el esquema de `{BASE}.sys.schemas` y no con `SCHEMA_NAME()`: como la
conexión está en `master`, esa función buscaba el esquema en `master` y devolvía `NULL` para
cualquier esquema distinto de `dbo`, con lo que "Recalcular" y "Recompilar" recibían un esquema
vacío y fallaban.

Por el mismo motivo, `sp_recompile` se invoca como `[BASE].sys.sp_recompile N'[esquema].[objeto]'`
(y `sp_refreshsqlmodule` de la misma forma): ese procedimiento solo busca el objeto en la base actual, y si se le pasa el nombre de tres partes
desde `master` responde con el error 15165 ("No se encontró el objeto").

## Las acciones administrativas

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

### Recompilar un objeto — `sp_refreshsqlmodule` y `sp_recompile`

`sp_recompile` es lo más cercano a `ALTER ... COMPILE` de Oracle: marca el objeto para que SQL
Server descarte su plan en caché y lo compile en la siguiente ejecución. Pero **no lo valida**:
responde que terminó bien aunque el objeto siga roto. Por eso la acción tiene tres pasos:

1. `sp_refreshsqlmodule` vuelve a analizar el objeto. Falla si una columna ya no existe o si a una
   vista le falta una tabla, y ese error de SQL Server se muestra al usuario.
2. `sp_recompile` descarta el plan en caché.
3. El backend vuelve a consultar `/objetos-invalidos`. Si el objeto sigue ahí, devuelve
   `exito = false` e indica lo que falta, porque a un procedimiento le puede faltar una tabla sin
   que los dos pasos anteriores den error.

Recompilar **no repara** un objeto: hay que crear lo que falta o corregirlo con `ALTER`. Si se crea
la tabla que faltaba, la referencia se resuelve y la acción confirma el éxito.

| Situación probada | Resultado |
|---|---|
| Procedimiento al que le falta una tabla | `exito = false`, indica la tabla que no existe |
| Objeto que usa una columna eliminada | `exito = false`, con el error de SQL Server |
| Objeto correcto, o procedimiento después de crear su tabla | `exito = true` |
| Objeto que no existe | `exito = false`, con el error de SQL Server |

### Validar los objetos de la base — `sp_refreshsqlmodule` con `ROLLBACK`

`/objetos-invalidos` lee el catálogo de dependencias, que no detecta columnas eliminadas: la tabla
sigue existiendo, así que la referencia aparece resuelta. El botón **Validar objetos** ejecuta
`sp_refreshsqlmodule` sobre cada vista, procedimiento y función y muestra el error que devuelva cada
uno. Cada intento va en una transacción que siempre se revierte, para no cambiar los metadatos.

Es más costosa que leer el catálogo y necesita el permiso `ALTER`, así que solo se ejecuta cuando el
usuario la pide. Las dos revisiones se complementan, por lo que un objeto puede aparecer en ambas.

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

Recalcular y recompilar devuelven siempre un `ResultadoAccionDto` (`exito`, `mensaje`, `objeto`,
`fecha`), nunca dejan que una falla de SQL Server se propague como un error 500 genérico: el
`MantenimientoService` captura la excepción, registra el detalle técnico en el log y devuelve
`exito = false` con un mensaje en español que explica qué revisar (por ejemplo, que la tabla
exista o que el login tenga el permiso `ALTER`). Al recompilar, el mensaje incluye el error que dio
SQL Server, y `exito` también es `false` si el objeto terminó sin errores pero sigue inválido.

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
