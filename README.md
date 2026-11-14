# API de Gestión de Pólizas de Arrendamiento

API REST para la gestión de pólizas de arrendamiento **individuales** y **colectivas**: consulta, renovación con incremento por IPC, cancelación y administración de riesgos, con notificación de cada cambio a un CORE transaccional simulado.

Desarrollada como prueba técnica para el cargo de Desarrollador TI.

## Tecnologías

- Java 17+ (desarrollado y probado con Java 21).
- Spring Boot 4.1.2 (Spring Web MVC, Spring Data JPA y Bean Validation).
- Hibernate 7 y base de datos H2 en memoria.
- JUnit 5, Mockito y AssertJ para pruebas.
- Maven Wrapper (no requiere instalar Maven).

## Requisitos

- JDK 17 o superior, con la variable `JAVA_HOME` configurada.
- No se necesita instalar Maven ni una base de datos. El proyecto incluye Maven Wrapper y utiliza H2 en memoria con datos de prueba precargados.

## Ejecución

Desde la raíz del proyecto:

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

**Windows (PowerShell o CMD)**

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en:

`http://localhost:8080`

### Ejecutar las pruebas

**Linux / macOS**

```bash
./mvnw test
```

**Windows**

```powershell
.\mvnw.cmd test
```

## Seguridad

Todas las peticiones a la API requieren el siguiente header:

```http
x-api-key: 123456
```

Si el header está ausente o contiene una clave incorrecta, la API responde `401 Unauthorized`.

La clave es configurable en `application.properties` mediante la propiedad:

`app.security.api-key`

## Datos de prueba

Al iniciar la aplicación se cargan automáticamente los datos definidos en `src/main/resources/data.sql`.

| ID | Número | Tipo | Estado | Riesgos |
|---|---|---|---|---|
| 1 | POL-IND-001 | INDIVIDUAL | ACTIVA | 1 activo |
| 2 | POL-COL-001 | COLECTIVA | ACTIVA | 2 activos |
| 3 | POL-COL-002 | COLECTIVA | ACTIVA | 1 activo |
| 4 | POL-IND-002 | INDIVIDUAL | CANCELADA | 1 cancelado |

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/polizas?tipo={tipo}&estado={estado}` | Lista pólizas. Filtros opcionales: `tipo` (`INDIVIDUAL`, `COLECTIVA`) y `estado` (`ACTIVA`, `RENOVADA`, `CANCELADA`). |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza. |
| POST | `/polizas/{id}/renovar` | Renueva la póliza: incrementa canon y prima según el IPC y pasa a estado `RENOVADA`. |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos activos. |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo pólizas colectivas). |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo. |
| POST | `/core-mock/evento` | Mock del CORE: registra en logs el evento recibido. |

### Ejemplos de uso

Todos los ejemplos requieren que la aplicación esté en ejecución.

**Listar pólizas colectivas activas**

```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"
```

**Consultar riesgos de una póliza**

```bash
curl -H "x-api-key: 123456" http://localhost:8080/polizas/2/riesgos
```

**Renovar una póliza**

```bash
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar
```

**Agregar un riesgo a una póliza colectiva**

```bash
curl -X POST \
  -H "x-api-key: 123456" \
  -H "Content-Type: application/json" \
  -d '{"direccionInmueble":"Calle 26 # 50-10, Apto 801","arrendatario":"Sofia Ramirez"}' \
  http://localhost:8080/polizas/2/riesgos
```

**Cancelar una póliza**

```bash
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/3/cancelar
```

**Cancelar un riesgo**

```bash
curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/2/cancelar
```

## Reglas de negocio

- Una póliza individual solo puede tener un riesgo. Únicamente se permite agregar riesgos a pólizas colectivas.
- No se puede renovar una póliza cancelada.
- La cancelación de una póliza cancela todos sus riesgos activos.
- La prima corresponde al canon mensual multiplicado por el número de meses de vigencia.
- La renovación se realiza por el mismo periodo de vigencia inicial, incrementando el canon según el IPC configurado en `app.negocio.ipc` (5 % por defecto).
- Toda operación que modifica pólizas o riesgos notifica al CORE.

### Respuestas de error

| Código HTTP | Caso |
|---|---|
| 400 | Datos de entrada inválidos o parámetros con valores no permitidos. |
| 401 | Header `x-api-key` ausente o inválido. |
| 404 | Póliza o riesgo inexistente. |
| 409 | Violación de una regla de negocio (por ejemplo, renovar una póliza cancelada). |

Los errores se devuelven en un formato JSON uniforme con los campos `timestamp`, `status`, `error`, `mensaje` y `ruta`.

## Arquitectura

Arquitectura en capas con un adaptador de integración hacia el CORE:

```text
com.alexanderaya.polizas
├── controller    # Endpoints REST (delgados, solo delegan al servicio)
├── service       # Lógica y reglas de negocio, transacciones
├── repository    # Acceso a datos con Spring Data JPA
├── domain        # Entidades JPA y enums
├── dto           # Contratos de entrada y salida de la API
├── integration   # Puerto (CoreClient) y adaptador REST hacia el CORE
├── security      # Filtro de validación de x-api-key
└── exception     # Excepciones de negocio y manejo global de errores
```

## Decisiones de diseño

- **`BigDecimal` para valores monetarios:** con redondeo explícito (`HALF_UP`, 2 decimales), para evitar errores de precisión.
- **IPC configurable:** definido en propiedades, sin valores fijos en el código.
- **Puerto y adaptador para el CORE:** el servicio depende de la interfaz `CoreClient`, no de la tecnología de integración. Esto permite reemplazar el mock por un adaptador para el servicio real sin modificar la lógica de negocio.
- **Transacciones:** cada operación de escritura se ejecuta dentro de una transacción. La cancelación de una póliza y de sus riesgos se aplica completa o no se aplica.
- **DTOs:** separados de las entidades para no exponer directamente el modelo de persistencia.
- **Manejo global de errores:** utiliza códigos HTTP coherentes y evita exponer detalles internos.
- **Comparación de la API Key en tiempo constante:** como práctica de desarrollo seguro.

## Mejoras para un entorno productivo

- Implementar el patrón **Outbox** y mecanismos de reintentos para garantizar la entrega de eventos al CORE, junto con **Circuit Breaker** (Resilience4j) ante caídas del servicio legado.
- Notificar al CORE después de confirmar la transacción, de forma asíncrona.
- Incorporar autenticación con **Spring Security y OAuth2/JWT** en lugar de una API Key fija, y almacenar las credenciales en un gestor de secretos.
- Documentar la API con **OpenAPI/Swagger** e implementar versionamiento (`/api/v1`).
- Utilizar una base de datos relacional productiva (Oracle/PostgreSQL) con migraciones versionadas mediante Flyway o Liquibase.
- Incorporar paginación en los listados, auditoría de cambios y pruebas de integración con Testcontainers.

## Flujo de trabajo con Git

Se utilizó **GitHub Flow**: `main` se mantiene estable y cada funcionalidad se desarrolla en una rama propia (`feature/...`, `test/...`, `docs/...`), que se integra mediante Pull Request.

Los commits siguen la convención **Conventional Commits**, utilizando prefijos como `feat`, `fix`, `refactor`, `test` y `docs`.

## Autor

**Alexander Aya Aponte**