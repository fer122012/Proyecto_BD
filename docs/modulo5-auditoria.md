# Módulo 5 – Auditoría

Da al DBA visibilidad sobre quién puede conectarse al servidor, quién puede entrar a cada base,
qué puede hacer cada quien y si hay objetos (vistas, procedimientos, funciones) con referencias rotas que podrían fallar al
ejecutarse.

- Consultas: `database/queries/modulo5_auditoria/` (01 a 07)
- Código: paquete `cr.ac.una.dbamonitor.auditoria` (controller, service, repository y `dto/`)
- Endpoints:
  - `GET /api/auditoria/logins`: logins registrados en el servidor
  - `GET /api/auditoria/roles`: roles de servidor con sus miembros, incluidos los que no tienen
  - `GET /api/auditoria/privilegios`: permisos de servidor otorgados/denegados por login
  - `GET /api/auditoria/objetos-invalidos?baseDatos=BASE_MONITOR`: objetos con dependencias sin
    resolver dentro de la base indicada
  - `GET /api/auditoria/usuarios-base?baseDatos=BASE_MONITOR`: usuarios de la base y su login
  - `GET /api/auditoria/roles-base?baseDatos=BASE_MONITOR`: roles de la base con sus miembros
  - `GET /api/auditoria/privilegios-base?baseDatos=BASE_MONITOR`: permisos de usuarios y roles de
    la base

Igual que el Módulo 1, el backend lee las consultas directamente de los archivos `.sql`
documentados (Maven los copia al classpath bajo `sql/`), así que el SQL que se ejecuta es
exactamente el que aparece aquí.

## Indicadores

| Indicador (endpoint) | Fuente | Cómo interpretarlo |
|---|---|---|
| Logins (`/logins`) | `sys.server_principals` | Cada fila es un login del servidor (tipo `SQL_LOGIN`, `WINDOWS_LOGIN` o `WINDOWS_GROUP`). `habilitado = false` significa que el login existe pero no puede conectarse (`DISABLE LOGIN`). `esSysadmin` se calcula revisando si el login pertenece al rol fijo `sysadmin`, que da control total sobre la instancia. |
| Roles (`/roles`) | `sys.server_role_members` | Los roles fijos de servidor (`sysadmin`, `dbcreator`, `securityadmin`, etc.) agrupan permisos predefinidos. La consulta trae una fila por cada par rol-miembro; el service las agrupa para mostrar, por cada rol, la lista completa de sus miembros. |
| Privilegios (`/privilegios`) | `sys.server_permissions` | Permisos otorgados (`GRANT`) o denegados (`DENY`) **directamente** a un login, sin pasar por un rol. Un login puede no tener privilegios aquí y aun así tener control total, si pertenece al rol `sysadmin` (ver `/roles`). |
| Usuarios de la base (`/usuarios-base`) | `sys.database_principals`, `sys.server_principals` | Un login da entrada al servidor y un usuario da entrada a una base; se relacionan por `sid`. Si `loginAsociado` es `null`, el usuario existe en la base pero no puede conectarse. |
| Roles de la base (`/roles-base`) | `sys.database_principals`, `sys.database_role_members` | Roles creados en la base, con o sin miembros, y roles fijos que tienen miembros. `esFijo` distingue los predefinidos de SQL Server. |
| Privilegios en la base (`/privilegios-base`) | `sys.database_permissions` | Permisos otorgados o denegados a un usuario o a un rol, y sobre qué recurso aplican. Un `DENY` prevalece sobre un `GRANT`. Un permiso asignado a un rol lo reciben todos sus miembros. |
| Objetos inválidos (`/objetos-invalidos`) | `sys.sql_expression_dependencies` | Objetos (vistas, procedimientos, funciones) que referencian una tabla u otro objeto de la misma base que SQL Server no pudo encontrar. Ver la sección siguiente para la explicación completa. |

## El equivalente en SQL Server de los "objetos inválidos" de Oracle

Oracle marca un objeto como `INVALID` apenas detecta que depende de algo que falta o cambió
(por ejemplo si se elimina una columna que usa una vista), y lo deja visible en
`DBA_OBJECTS`/`ALL_OBJECTS` sin necesidad de ejecutar nada.

SQL Server **no hace esa validación al crear el objeto** porque usa *resolución de nombres
diferida* (deferred name resolution): permite crear un procedimiento o una vista que referencia
una tabla que todavía no existe, apostando a que existirá quando se ejecute. Esto significa que
SQL Server no tiene una columna "válido/inválido" que se pueda consultar directamente.

Para lograr el mismo resultado sin tener que ejecutar cada objeto uno por uno (lo cual sería
lento y, en el caso de procedimientos que modifican datos, peligroso), este módulo consulta el
catálogo de dependencias (`sys.sql_expression_dependencies`), que SQL Server construye al crear
cada objeto. Cuando una referencia no se pudo resolver contra ningún objeto real, la columna
`referenced_id` queda en `NULL`. Esa es la señal que usa la consulta `04_objetos_invalidos.sql`.

```
Vista  dbo.ReporteVentas
   referencia →  dbo.Ventas (existe)        → referenced_id = 123   (OK, no aparece)
   referencia →  dbo.VentasHistorico (no existe) → referenced_id = NULL  (aparece en la lista)
```

Las referencias a otra base o a otro servidor se excluyen: en ellas `referenced_id` siempre es
`NULL`, aunque el objeto exista, y aparecerían como falsos positivos.

Es una aproximación, no una garantía absoluta: detecta tablas u objetos que faltan, pero no
columnas eliminadas, porque la tabla sigue existiendo y la referencia aparece resuelta. Esas se
encuentran con la acción "Validar objetos" del Módulo 6 (ver `docs/modulo6-mantenimiento.md`), que
no se ejecuta aquí porque necesita el permiso `ALTER` y es más costosa que leer el catálogo.

### Por qué se consulta una base específica y qué permiso necesita

Las dependencias son propias de cada base de datos. La conexión del backend está en `master`,
donde no hay objetos de las aplicaciones, así que consultar ahí siempre devolvería una lista
vacía. Por eso la ruta recibe `baseDatos` y la consulta usa `{BASE}.sys.sql_expression_dependencies`
(el nombre se valida con `SqlIdentificadores` antes de insertarlo). El esquema se obtiene de
`{BASE}.sys.schemas`, porque `SCHEMA_NAME()` lo buscaría en `master` y devolvería `NULL`.

`sys.sql_expression_dependencies` es una excepción entre las vistas de catálogo: por defecto solo
la puede leer `db_owner`, y `VIEW ANY DEFINITION` **no alcanza**. Sin un permiso explícito, SQL
Server responde *"Se denegó el permiso SELECT en el objeto 'sql_expression_dependencies'"*.
`database/scripts/02_permisos_mantenimiento.sql` otorga `SELECT` sobre esa vista al usuario
`dba_monitor` dentro de la base de práctica.

En `BASE_MONITOR` aparecen tres objetos inválidos creados a propósito por
`00_crear_base_monitor.sql`: la vista `ventas.vw_FacturasConDescuento` y el procedimiento
`ventas.usp_AplicarDescuento` (la tabla `ventas.Descuento` se eliminó después de crearlos) y
`rrhh.usp_CalcularBonos` (referencia la tabla `rrhh.Bono`, que nunca existió).

