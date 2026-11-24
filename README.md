# Viajes por Chile — Backend

API REST de Viajes por Chile, desarrollada con Spring Boot. El proyecto requiere una instancia MariaDB activa y el esquema creado antes de iniciar la aplicación.

## Requisitos

- JDK 17.
- Maven 3.8 o superior.
- MariaDB disponible en `localhost:3308` (valor predeterminado local).

## Base de datos local

Crea/carga la base de datos `viajes_chile` usando `database/schema.sql`. Para una base existente, aplica las migraciones pendientes de `database/migrations/` en orden. La aplicación no crea ni migra el esquema automáticamente (`spring.jpa.hibernate.ddl-auto=none`).

Configura las variables en PowerShell antes de iniciar Spring Boot. Ajusta usuario y contraseña a tu instalación de MariaDB:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3308/viajes_chile"
$env:DB_USER = "root"
$env:DB_PASSWORD = ""
$env:CORS_ORIGINS = "http://localhost:5173"

$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

`JWT_SECRET` es obligatorio y se genera aleatoriamente para esta terminal; no uses una clave fija en el código ni la subas a GitHub. Si abres otra terminal, vuelve a definir las variables.

## Pruebas y ejecución

Desde esta carpeta (`backend`):

```powershell
mvn test
mvn spring-boot:run
```

La API escucha en el puerto **8080** por defecto; si el proveedor define `PORT`, Spring Boot lo utiliza:

- Swagger UI: <http://localhost:8080/swagger-ui.html> (también disponible en <http://localhost:8080/swagger-ui/index.html>).
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>.
- Catálogo: <http://localhost:8080/api/v1/products>.

Si MariaDB no está en ejecución, la base no existe o las credenciales no son correctas, Spring Boot no podrá iniciar. No se oculta ese error con una base de datos alternativa.

## Conexión desde el frontend

En desarrollo, Vite sirve el frontend en el puerto **5173** y reenvía `/api` a Spring Boot. Para otros orígenes, configura `CORS_ORIGINS` con el origen exacto del frontend. En producción, configura además `VITE_API_BASE_URL`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y, según el proveedor, `GOOGLE_CLIENT_ID`, `FRONTEND_URL` y `REFRESH_COOKIE_SECURE=true`.

No uses las credenciales locales por defecto en un despliegue público. La cookie de refresh requiere HTTPS cuando se habilita `REFRESH_COOKIE_SECURE`.
