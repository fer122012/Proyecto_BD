# Módulo 5 – Auditoría

Da al DBA visibilidad sobre quién puede conectarse al servidor, qué puede hacer cada quien y si
hay objetos (vistas, procedimientos, funciones) con referencias rotas que podrían fallar al
ejecutarse.

- Consultas: `database/queries/modulo5_auditoria/` (01 a 04)
- Código: paquete `cr.ac.una.dbamonitor.auditoria` (controller, service, repository y `dto/`)
- Endpoints:
  - `GET /api/auditoria/logins`: logins registrados en el servidor
  - `GET /api/auditoria/roles`: roles de servidor con sus miembros
  - `GET /api/auditoria/privilegios`: permisos de servidor otorgados/denegados por login
  - `GET /api/auditoria/objetos-invalidos?baseDatos=BASE_MONITOR`: objetos con dependencias sin
    resolver dentro de la base indicada

Igual que el Módulo 1, el backend lee las consultas directamente de los archivos `.sql`
documentados (Maven los copia al classpath bajo `sql/`), así que el SQL que se ejecuta es
exactamente el que aparece aquí.

## Indicadores

| Indicador (endpoint) | Fuente | Cómo interpretarlo |
|---|---|---|
| Logins (`/logins`) | `sys.server_principals` | Cada fila es un login del servidor (tipo `SQL_LOGIN`, `WINDOWS_LOGIN` o `WINDOWS_GROUP`). `habilitado = false` significa que el login existe pero no puede conectarse (`DISABLE LOGIN`). `esSysadmin` se calcula revisando si el login pertenece al rol fijo `sysadmin`, que da control total sobre la instancia. |
| Roles (`/roles`) | `sys.server_role_members` | Los roles fijos de servidor (`sysadmin`, `dbcreator`, `securityadmin`, etc.) agrupan permisos predefinidos. La consulta trae una fila por cada par rol-miembro; el service las agrupa para mostrar, por cada rol, la lista completa de sus miembros. |
| Privilegios (`/privilegios`) | `sys.server_permissions` | Permisos otorgados (`GRANT`) o denegados (`DENY`) **directamente** a un login, sin pasar por un rol. Un login puede no tener privilegios aquí y aun así tener control total, si pertenece al rol `sysadmin` (ver `/roles`). |
| Objetos inválidos (`/objetos-invalidos`) | `sys.sql_expression_dependencies` | Objetos (vistas, procedimientos, funciones) que referencian una tabla, columna u otro objeto que SQL Server no pudo encontrar. Ver la sección siguiente para la explicación completa. |

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

Es una aproximación, no una garantía absoluta: cubre el caso más común y más fácil de
demostrar (una tabla o columna eliminada después de crear el objeto que la usa), que es también
el que pide la rúbrica del curso ("objetos inválidos o con errores de compilación").

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

## Preguntas probables en la defensa

**¿Por qué el login puede no tener privilegios listados y aun así tener acceso total?**
Porque los privilegios de `/privilegios` son solo los que se otorgaron directamente al login.
Si el login pertenece al rol `sysadmin` (visible en `/roles`), tiene control total sobre la
instancia sin que haga falta otorgarle ningún permiso individual.

**¿Qué diferencia hay entre un rol fijo de servidor y un rol de base de datos?**
Los roles de servidor (los que muestra este módulo) aplican a toda la instancia: a qué puede
entrar y qué puede administrar un login. Los roles de base de datos (`db_owner`,
`db_ddladmin`, etc., como el que usa el Módulo 6) aplican dentro de una base específica y se
asignan a un *usuario* de esa base, no directamente al login.

**¿Por qué SQL Server no detecta los objetos inválidos al crearlos, como Oracle?**
Por diseño: usa resolución de nombres diferida, para permitir desplegar objetos en un orden que
no sea estrictamente dependiente (por ejemplo, crear un procedimiento antes que la tabla que va
a usar, siempre que la tabla exista para cuando el procedimiento se ejecute por primera vez).
La contraparte es que el error solo aparece en tiempo de ejecución, o revisando el catálogo de
dependencias como hace esta consulta.

**¿Qué permisos necesita la aplicación para este módulo?**
Solo `VIEW ANY DEFINITION`, ya otorgado en `database/scripts/01_crear_login_monitor.sql`. No se
necesita `VIEW SERVER STATE` porque este módulo lee catálogos del sistema (metadatos), no
vistas de administración dinámica (DMVs) de actividad en tiempo real.
