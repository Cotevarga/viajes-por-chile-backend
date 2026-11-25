# Viajes por Chile — Backend

API REST de Viajes por Chile, desarrollada con Spring Boot. El proyecto requiere una instancia MariaDB activa y el esquema creado antes de iniciar la aplicación.

## Requisitos

- JDK 25.
- Maven 3.8 o superior.
- MariaDB disponible en `localhost:3308` (valor predeterminado local).

Maven usa el JDK indicado por `JAVA_HOME`. En PowerShell, configura la ruta de tu instalación de JDK 25 para la terminal actual:

```powershell
$env:JAVA_HOME = "$HOME\.jdks\temurin-25.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -version
```

Si tu JDK 25 está en otra ubicación, ajusta `JAVA_HOME`.

## Base de datos local

Crea/carga la base de datos `viajes_chile` usando `database/schema.sql`. Para una base existente, aplica las migraciones pendientes de `database/migrations/` en orden. La aplicación no crea ni migra el esquema automáticamente (`spring.jpa.hibernate.ddl-auto=none`).

La migración `database/migrations/20261124_reporting_and_pos.sql` agrega campos opcionales para reportes, datos de pasajeros y auditoría. Los registros anteriores conservan valores desconocidos; no se reconstruyen datos históricos.

Configura las variables en PowerShell antes de iniciar Spring Boot. Ajusta usuario y contraseña a tu instalación de MariaDB:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3308/viajes_chile"
$env:DB_USER = "root"
$env:DB_PASSWORD = ""
$env:CORS_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"

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

La prueba opcional `SaleRepositoryMariaDbTest` requiere una base MariaDB desechable con el esquema actualizado. Apunta `DB_URL` a esa base, configura sus credenciales y define `REPORTING_POS_DB_TEST=true` antes de ejecutar `mvn test`; no uses una base compartida.

La API escucha en el puerto **8080** por defecto; si el proveedor define `PORT`, Spring Boot lo utiliza:

- Swagger UI: <http://localhost:8080/swagger-ui.html> (también disponible en <http://localhost:8080/swagger-ui/index.html>).
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>.
- Catálogo: <http://localhost:8080/api/v1/products>.

Si MariaDB no está en ejecución, la base no existe o las credenciales no son correctas, Spring Boot no podrá iniciar. No se oculta ese error con una base de datos alternativa.

## Conexión desde el frontend

En desarrollo, Vite sirve el frontend en el puerto **5173** y reenvía `/api` a Spring Boot. Para otros orígenes, configura `CORS_ORIGINS` con el origen exacto del frontend. En producción, configura además `VITE_API_BASE_URL`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y, según el proveedor, `GOOGLE_CLIENT_ID`, `FRONTEND_URL` y `REFRESH_COOKIE_SECURE=true`.

Separa varios orígenes con comas, sin espacios; el proxy también reenvía el encabezado `Origin`. Un origen no permitido recibe `403 Invalid CORS request`, incluso al cerrar sesión.

No uses las credenciales locales por defecto en un despliegue público. La cookie de refresh requiere HTTPS cuando se habilita `REFRESH_COOKIE_SECURE`.

## Vendedor y reportes

SELLER y ADMIN pueden emitir compras, consultar órdenes, actualizar boletos con `PUT /api/v1/tickets/{ticketCode}` y anular ventas o boletos con `POST /api/v1/sales/cancellations`. Las actualizaciones mantienen el precio y la clase y comprueban disponibilidad con bloqueo transaccional. Las compras y anulaciones son simuladas: no efectúan cobros ni reembolsos reales.

El reporte administrativo incluye ventas, cancelaciones, eventos de flota, mensajes y sucesos del período. Los datos no registrados en operaciones antiguas se devuelven sin valor; el cliente los identifica como «No disponible».

La búsqueda `GET /api/v1/ordenes` acepta `search` (código, nombre o RUT) y `date` (`YYYY-MM-DD`). `GET /api/v1/reports` acepta `period`: `day`, `week`, `month` o `year`. Swagger y OpenAPI describen el contrato; las pruebas disponibles no cubren cada endpoint por HTTP.
