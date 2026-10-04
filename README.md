# DBA Monitor – Proyecto EIF402

Herramienta de monitoreo, administración y auditoría de SQL Server.
Proyecto Integrador, Administración de Bases de Datos, UNA, II Ciclo 2026.

## Integrantes
- Mariela Orozco Rayo 
- Paulina Porras Núñez
- Katherine Valverde Fallas
- Alex Quesada 

## Tecnologías
- SQL Server
- Backend: Java + Spring Boot
- Frontend: React

## Estructura del repositorio
- `backend/`: API en Spring Boot
- `frontend/`: interfaz en React
- `database/scripts/`: scripts de creación y permisos
- `database/queries/`: consultas administrativas documentadas
- `docs/`: documentación técnica, manual de usuario, pruebas y bitácora

## Requisitos del entorno

### 1. SQL Server
- SQL Server **Developer** o **Express**. La instancia de desarrollo del grupo es Express,
  que **no incluye SQL Server Agent**.
- **Autenticación mixta** (SQL Server y Windows). En SSMS: clic derecho sobre el servidor →
  *Properties* → *Security* → *SQL Server and Windows Authentication mode*. Luego reiniciar
  el servicio de SQL Server.

### 2. TCP/IP en el puerto 1433
1. Abrir **SQL Server Configuration Manager**.
2. Ir a *SQL Server Network Configuration* → *Protocols for <NOMBRE_INSTANCIA>*
   (en Express normalmente `SQLEXPRESS`).
3. Clic derecho sobre **TCP/IP** → *Enable*.
4. Abrir las propiedades de TCP/IP → pestaña *IP Addresses* → sección **IPAll**:
   - Dejar vacío *TCP Dynamic Ports*.
   - Poner `1433` en *TCP Port*.
5. Ir a *SQL Server Services* y **reiniciar el servicio** de SQL Server
   (los cambios no se aplican hasta reiniciar).

### 3. Login de la aplicación
Ejecutar `database/scripts/01_crear_login_monitor.sql` con un usuario administrador.
Antes de ejecutarlo, reemplazar el placeholder `<CAMBIAR_CONTRASENA>` por una contraseña
segura **sin guardar ese cambio en el repositorio**. El script crea el login `dba_monitor`
y le otorga `VIEW SERVER STATE` y `VIEW ANY DEFINITION`.

### 4. Herramientas
- **Java 17** (JDK)
- **Node.js** (versión LTS) con npm

### 5. Configuración local del backend
Las credenciales nunca se suben a Git. En `backend/src/main/resources/`:
1. Copiar `application-example.properties` y renombrar la copia como
   `application-local.properties` (este archivo está en `.gitignore`).
2. Completar los valores reales en la copia local:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true
spring.datasource.username=dba_monitor
spring.datasource.password=<CAMBIAR_CONTRASENA>
```

> **Nota sobre la conexión:** la URL usa `encrypt=true;trustServerCertificate=true`.
> La conexión va cifrada, pero se acepta el certificado autofirmado de la instancia local
> sin validarlo. Es adecuado para desarrollo, no para producción.

## Base de datos de práctica
El grupo debe acordar una base de datos de ejemplo común (por ejemplo **AdventureWorks**)
para que todos prueben los módulos con los mismos datos.

**Pendiente:** definir la base de práctica y documentar aquí cómo instalarla.

## Instalación y ejecución
(Se completa conforme avance el proyecto)
