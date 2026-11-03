# Módulo 3 – Gestión del almacenamiento

Supervisa el espacio físico asignado a las bases de datos y ayuda a detectar problemas por el
crecimiento del almacenamiento: qué tan llenos están los filegroups, cuánto ocupa cada base y cada
archivo, qué objetos consumen más espacio y cómo ha crecido la base en el tiempo.

- Consultas: `database/queries/modulo3_almacenamiento/` (01 a 05)
- Código: paquete `cr.ac.una.dbamonitor.almacenamiento` (controller, service, repository y `dto/`)
- Permisos: `database/scripts/03_permisos_almacenamiento.sql`
- Pantalla: `frontend/src/modulos/almacenamiento/`

## Indicadores

| Indicador (campo JSON) | Fuente | Cómo interpretarlo |
|---|---|---|
| Tamaño de datos, log y total (`tamanoDatosMb`, `tamanoLogMb`, `tamanoTotalMb`) | `sys.master_files.size` (páginas de 8 KB) | Espacio que ocupan en disco los archivos de la base, tengan datos o no. Es el "tamaño actual" de la base. |
| Tamaño del filegroup (`tamanoMb`) | Suma de `sys.database_files.size` de sus archivos | Espacio asignado al filegroup. |
| Usado (`usadoMb`) | Suma de `sys.allocation_units.total_pages` del filegroup | Páginas reservadas por tablas e índices. Ya no están disponibles para datos nuevos. |
| Disponible y % de uso (`disponibleMb`, `porcentajeUso`) | Calculado en el backend | Tamaño − usado, y usado / tamaño × 100. |
| Estado (`estado`, `interpretacion`) | Calculado en el backend | `NORMAL` por debajo de 85 %. `ATENCION` si supera 85 % pero algún archivo puede crecer solo. `CRITICO` si supera 85 % y ningún archivo puede crecer: al llenarse, las inserciones fallan con el error 1105. |
| Archivos (`/archivos`) | `sys.database_files` + `sys.filegroups` | Nombre lógico, tipo (datos o log), filegroup, ruta física, estado, tamaño, máximo (`Ilimitado` si `max_size = -1`) y crecimiento automático (`8 MB`, `10 %` o `Sin crecimiento automático`). |
| Objetos de mayor tamaño (`/objetos`) | `sys.partitions` + `sys.allocation_units` + `sys.indexes` | Las 20 tablas e índices que más espacio reservan, con filas, espacio reservado y usado. Cada fila es un índice (o el `HEAP` de una tabla sin índice clúster), porque el espacio se asigna por índice. |
| Crecimiento (`/crecimiento`) | `msdb.dbo.backupset` + `msdb.dbo.backupfile` | Tamaño asignado y usado de la base en cada uno de los últimos 30 respaldos completos, la variación entre ellos, el crecimiento total y el promedio diario. |

### Por qué el crecimiento sale de los respaldos

SQL Server no guarda un historial del tamaño de las bases. Lo que sí guarda es el historial de
respaldos en `msdb`, y cada respaldo registra el tamaño que tenía cada archivo (`backupfile.file_size`)
y cuántos datos contenía (`backupset.backup_size`). Con los respaldos completos se obtiene una serie
**real** del tamaño de la base, sin crear tablas ni jobs adicionales (la instancia Express no tiene
SQL Server Agent). El enunciado acepta "información histórica real o un mecanismo documentado para
registrar y comparar el crecimiento".

Si la base nunca se respaldó, la pantalla lo explica en lugar de mostrar un gráfico vacío. Para
generar historial basta con hacer respaldos completos de vez en cuando (ver Módulo 4):

```sql
BACKUP DATABASE [MiBase] TO DISK = N'C:\Respaldos\MiBase.bak' WITH INIT;
```

### Por qué las consultas usan `{BASE}.sys.schemas` y no `SCHEMA_NAME()`

Las consultas por base usan nombres de tres partes (`[MiBase].sys.objects`) porque la conexión
sigue en `master`. Funciones como `SCHEMA_NAME()` u `OBJECT_NAME()` (sin el parámetro de base)
resuelven el nombre en la base **actual** de la conexión (`master`) y devolverían nombres
equivocados o `NULL`. Por eso el esquema se obtiene uniendo `{BASE}.sys.schemas`.

## Permisos que necesita este módulo

| Consulta | Permiso |
|---|---|
| Tamaño de las bases (`sys.master_files`) | `VIEW ANY DEFINITION`, ya otorgado por `01_crear_login_monitor.sql`. |
| Filegroups, archivos y objetos (`{BASE}.sys.*`) | Un usuario `dba_monitor` dentro de la base, sin roles. `VIEW ANY DEFINITION` ya le deja ver los metadatos. |
| Crecimiento (`msdb.dbo.backupset`, `backupfile`) | Un usuario en `msdb` con `SELECT` solo sobre esas dos tablas. |

`database/scripts/03_permisos_almacenamiento.sql` crea ambos usuarios para la base de práctica
`BASE_MONITOR`. Para analizar otra base, cambiar el `USE` y ejecutarlo una vez por cada base. Si la base ya tiene
el usuario por el script 02 (Módulo 6), esa parte no hace nada. Sin estos permisos, las rutas por
base responden con un 500 y el mensaje de permisos insuficientes.

