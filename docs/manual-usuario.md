# Manual de usuario – DBA Monitor

Este manual explica cómo usar las funcionalidades ya implementadas del proyecto. Se irá
ampliando conforme avancen los demás módulos y la interfaz web (`frontend/`).

> **Estado actual:** el frontend todavía no está implementado, así que esta versión del manual
> explica cómo usar la API directamente (por ejemplo desde el navegador para las consultas
> `GET`, o con una herramienta como Postman/Insomnia o `curl` para las acciones `POST`). Cuando
> el equipo tenga la interfaz web, cada sección se actualizará con las pantallas correspondientes
> y los pasos para usarla desde ahí.

## 1. Antes de empezar

1. Seguir los pasos de `README.md` (SQL Server con TCP/IP habilitado, login `dba_monitor`
   creado, `application-local.properties` configurado).
2. Para el Módulo 6, ejecutar además `database/scripts/02_permisos_mantenimiento.sql` sobre la
   base de práctica que se vaya a usar (ver `docs/modulo6-mantenimiento.md`).
3. Iniciar el backend:
   ```
   cd backend
   ./mvnw.cmd spring-boot:run      # Windows
   ./mvnw spring-boot:run          # Linux/macOS
   ```
4. El backend queda escuchando en `http://localhost:8080`. Para comprobar que responde:
   `GET http://localhost:8080/api/salud`.

## 2. Módulo 5 – Auditoría

Responde a la pregunta: *¿quién puede entrar al servidor, qué puede hacer y hay algún objeto
roto que debería revisar?*

| Qué quiero ver | Cómo pedirlo |
|---|---|
| Logins registrados | `GET /api/auditoria/logins` |
| Roles de servidor y sus miembros | `GET /api/auditoria/roles` |
| Privilegios otorgados por login | `GET /api/auditoria/privilegios` |
| Objetos con dependencias rotas | `GET /api/auditoria/objetos-invalidos` |

**Cómo leer los logins:** cada fila tiene `habilitado` (si puede conectarse) y `esSysadmin` (si
tiene control total de la instancia). Un login puede no tener privilegios individuales en
`/privilegios` y aun así tener control total si `esSysadmin` es `true` — eso se explica revisando
`/roles` y viendo si pertenece al rol `sysadmin`.

**Cómo leer los objetos inválidos:** cada fila indica el objeto (`esquema`/`objeto`/`tipo`) y a
qué intentaba hacer referencia (`esquemaReferenciado`/`objetoReferenciado`) sin encontrarlo. Una
lista vacía (`[]`) es buena señal: no se detectaron referencias rotas.

## 3. Módulo 6 – Mantenimiento preventivo

Responde a la pregunta: *¿qué tablas necesitan que les recalcule las estadísticas, y puedo
arreglar un objeto con errores de compilación desde aquí?*

Todas las solicitudes de este módulo necesitan indicar la base de datos de práctica con la que
se está trabajando (por ejemplo `AdventureWorks`), porque la conexión del backend apunta a
`master` (ver `docs/modulo6-mantenimiento.md` para el detalle).

### 3.1 Consultar el estado de las estadísticas

```
GET /api/mantenimiento/estadisticas?baseDatos=AdventureWorks
```

Devuelve una lista ordenada con las tablas cuyas estadísticas conviene revisar primero (más
filas modificadas desde la última actualización, o estadística más antigua).

### 3.2 Recalcular las estadísticas de una tabla

```
POST /api/mantenimiento/estadisticas/recalcular
Content-Type: application/json

{
  "baseDatos": "AdventureWorks",
  "esquema": "Sales",
  "tabla": "SalesOrderHeader"
}
```

Ejemplo con `curl`:
```
curl -X POST http://localhost:8080/api/mantenimiento/estadisticas/recalcular \
  -H "Content-Type: application/json" \
  -d "{\"baseDatos\":\"AdventureWorks\",\"esquema\":\"Sales\",\"tabla\":\"SalesOrderHeader\"}"
```

La respuesta siempre indica si la acción tuvo éxito:
```json
{ "exito": true, "mensaje": "Estadísticas recalculadas correctamente.",
  "objeto": "AdventureWorks.Sales.SalesOrderHeader", "fecha": "2026-10-11T09:30:00" }
```

Si `exito` es `false`, el campo `mensaje` explica qué revisar (por ejemplo que la tabla no
exista, o que falte ejecutar `database/scripts/02_permisos_mantenimiento.sql`).

### 3.3 Consultar y recompilar objetos con errores

```
GET /api/mantenimiento/objetos-invalidos?baseDatos=AdventureWorks
```

Para recompilar uno de los objetos que aparezcan en esa lista:
```
POST /api/mantenimiento/objetos/recompilar
Content-Type: application/json

{
  "baseDatos": "AdventureWorks",
  "esquema": "dbo",
  "objeto": "uspGetProductosDescontinuados"
}
```

La respuesta sigue el mismo formato que la de recalcular estadísticas (`exito`, `mensaje`,
`objeto`, `fecha`).

## 4. Errores comunes y qué significan

| Situación | Respuesta de la API | Qué hacer |
|---|---|---|
| SQL Server no está disponible | HTTP 503, mensaje "No se pudo establecer conexión con SQL Server..." | Verificar que el servicio esté iniciado y que `application-local.properties` tenga los datos correctos (ver README). |
| El nombre de base/esquema/tabla tiene caracteres no válidos | HTTP 400, mensaje indicando cuál identificador y por qué | Usar solo letras, números, guion bajo, `#` o `$`; revisar que no haya espacios ni comillas. |
| Se intenta recalcular estadísticas sin haber ejecutado `02_permisos_mantenimiento.sql` | Respuesta 200 con `"exito": false` y mensaje sobre el permiso `ALTER` | Ejecutar ese script sobre la base de práctica correspondiente (ver sección 1). |
| Una consulta de otro módulo falla inesperadamente | HTTP 500, mensaje genérico en español | El detalle técnico queda en el log del backend; revisarlo para diagnosticar. |

## 5. Próximas secciones

Pendiente de completar conforme avance el proyecto:
- Módulos 1 a 4: capturas de pantalla y pasos de uso desde la interfaz web una vez que exista.
- Módulos 5 y 6: capturas de pantalla y pasos de uso desde la interfaz web (por ahora, ver las
  secciones 2 y 3 de este manual, que documentan el uso directo de la API).
