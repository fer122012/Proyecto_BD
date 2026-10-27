# DBA Monitor – Proyecto EIF402

Herramienta web para monitorear, administrar y auditar SQL Server.
Proyecto Integrador, Administración de Bases de Datos, UNA, II Ciclo 2026.

## Integrantes
- Mariela Orozco Rayo
- Paulina Porras Núñez
- Katherine Valverde Fallas
- Alex Quesada

## Tecnologías
SQL Server · Java 17 + Spring Boot · React + Vite

## Estructura
| Carpeta | Contenido |
|---|---|
| `backend/` | API en Spring Boot |
| `frontend/` | Interfaz en React |
| `database/scripts/` | Creación de la base de práctica, login y permisos |
| `database/queries/` | Consultas SQL de cada módulo |
| `docs/` | Documentación de cada módulo y manual de usuario |

## Cómo correrlo

Necesitas **SQL Server** (Express o Developer), **Java 17** y **Node.js**.

### 1. Preparar SQL Server
- Activar la **autenticación mixta** (SQL Server y Windows).
- Habilitar **TCP/IP en el puerto 1433** en *SQL Server Configuration Manager* y reiniciar el servicio.

### 2. Crear el login y la base de práctica
Desde la raíz del repositorio, con un usuario administrador. Antes, cambiar `<CAMBIAR_CONTRASENA>`
en el script 01 por una contraseña (sin subir ese cambio a Git).

```
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\01_crear_login_monitor.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\00_crear_base_monitor.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\02_permisos_mantenimiento.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\03_permisos_almacenamiento.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\04_permisos_respaldo.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\05_generar_historial_respaldos.sql
```

Esto crea el login `dba_monitor` y la base de práctica **`BASE_MONITOR`**, con datos de ejemplo,
sus permisos y un historial de respaldos.

### 3. Configurar el backend
En `backend/src/main/resources/`, copiar `application-example.properties` como
`application-local.properties` y poner la contraseña del paso 2. Ese archivo no se sube a Git.

### 4. Ejecutar
```
cd backend
.\mvnw.cmd spring-boot:run
```
```
cd frontend
npm install
npm run dev
```
Abrir **http://localhost:5173**.

## Más información
- [Instalación detallada y solución de problemas](docs/instalacion.md)
- [Manual de usuario](docs/manual-usuario.md)
- Documentación de cada módulo en [`docs/`](docs/)
