# Enrollment Platform

Sistema de inscripción educativa virtual desarrollado como monolito modular hexagonal con Spring Boot.

## Caso de negocio

Una plataforma educativa necesita gestionar la inscripción de estudiantes a cursos virtuales. El sistema cubre tres requisitos funcionales:

1. **Consultar cursos disponibles** — `GET /courses` lista todos los cursos con nombre, instructor, duración y costo.
2. **Agregar cursos** — `POST /courses` incorpora nuevos cursos con persistencia en base de datos.
3. **Inscribir estudiantes** — `POST /enrollments` inscribe a un estudiante en uno o más cursos y devuelve un resumen con el costo de cada curso y el total a pagar.

## Requisitos mínimos


| Herramienta | Versión mínima                              |
| ----------- | ------------------------------------------- |
| Java        | 21                                          |
| Maven       | 3.9+                                        |
| Oracle DB   | Solo perfil `prod` (opcional en desarrollo) |


## Estructura del proyecto

```
src/main/java/com/duoc/enrollmentplatform/
├── shared/domain/          # DomainError, Id, Money, Email
├── courses/
│   ├── domain/             # Course entity, CourseRepository + InMemory
│   ├── application/        # ListCoursesUseCase, CreateCourseUseCase, DTOs
│   └── infrastructure/     # JpaCourseRepository, CourseController
├── enrollment/
│   ├── domain/             # Student, Enrollment, EnrollmentLine, repos + InMemory
│   ├── application/        # CRUD inscripciones (use cases + DTOs)
│   │   ├── summary/        # Resúmenes JSON/PDF y S3 (use cases)
│   │   └── ports/          # EnrollmentSummaryStorage, PdfRenderer
│   └── infrastructure/     # JPA/S3 adapters, EnrollmentController, EnrollmentSummaryController
└── factory/                # EnrollmentPlatformFactory, ApplicationConfiguration

src/main/resources/
├── application.properties          # Base; perfil: ${SPRING_PROFILES_ACTIVE:local}
├── application-local.properties    # H2 in-memory + Flyway
├── application-prod.properties     # Oracle Autonomous (variables de entorno)
└── db/migration/
    ├── V1__schema.sql              # Creación de tablas
    └── V2__seed_data.sql           # Datos iniciales en español
```

## Ejecutar

**Perfil `local` (H2 + LocalStack S3)**

```bash
docker compose up -d localstack
docker exec "$(docker ps -qf 'ancestor=localstack/localstack:4.4.0')" \
  awslocal s3 mb s3://enrollment-platform-summaries
./run-local.sh
```

`run-local.sh` fuerza el perfil `local`, exporta credenciales `test`/`test` para LocalStack y **ignora** las variables Oracle de `.env` (si las tienes para prod). Si tienes un `.env` con `SPRING_PROFILES_ACTIVE=prod`, no afecta: este script siempre usa `local`.

**Perfil `prod` (Oracle + wallet mTLS)** — coloca la wallet descargada en `Wallet_ENROLLMENTPLATFORMDB/` (incluye `tnsnames.ora`, `sqlnet.ora`, `ewallet.pem`), crea `.env` y arranca:

```bash
cp .env.example .env
# Edita .env: usuario/contraseña de la BD y, si quieres, el alias TNS (por defecto enrollmentplatformdb_high)
./run-prod.sh
```

`run-prod.sh` configura `TNS_ADMIN` y `ORACLE_WALLET_DIR` apuntando a la wallet; la URL JDBC usa el alias del `tnsnames.ora` (mTLS, sin pegar el descriptor completo en `.env`).

La wallet debe incluir `cwallet.sso` (y `tnsnames.ora`). El proyecto declara `oraclepki` en el `pom.xml` para que el driver JDBC pueda abrir el keystore SSO (`ORA-17957` / `SSO KeyStore not available` si falta).

Alias típicos: `enrollmentplatformdb_high`, `enrollmentplatformdb_medium`, `enrollmentplatformdb_low`.

Base URL: `http://localhost:8080`

| Método | URL                                      | Descripción                              |
| ------ | ---------------------------------------- | ---------------------------------------- |
| GET    | `/courses`                               | Lista cursos                             |
| POST   | `/courses`                               | Crea curso                               |
| POST   | `/enrollments`                           | Inscribe estudiante (sube resumen a S3)  |
| GET    | `/enrollments`                           | Lista inscripciones                      |
| GET    | `/enrollments/{id}`                      | Obtiene inscripción                      |
| PUT    | `/enrollments/{id}`                      | Actualiza cursos de la inscripción       |
| DELETE | `/enrollments/{id}`                      | Elimina inscripción y resumen en S3      |
| GET    | `/enrollments/summaries`                 | Lista resúmenes almacenados en S3        |
| GET    | `/enrollments/{id}/summary/file`         | Genera JSON descargable (sin S3)         |
| POST   | `/enrollments/{id}/summary`              | Sube o reemplaza resumen en S3           |
| GET    | `/enrollments/{id}/summary`              | Descarga resumen (`?format=pdf` opcional)|
| PUT    | `/enrollments/{id}/summary`              | Reemplaza JSON del resumen en S3         |
| DELETE | `/enrollments/{id}/summary`              | Elimina resumen en S3                    |
| GET    | `/actuator/health`                       | Health check                             |
| GET    | `/h2-console`                            | Consola H2 (solo perfil local)           |

Los resúmenes se guardan en S3 como `{enrollmentId}/summary.json`. Detalle en [docs/almacenamiento-s3-resumenes.md](docs/almacenamiento-s3-resumenes.md).

Variables mínimas: `AWS_REGION`, `AWS_S3_BUCKET`; en local con LocalStack también `AWS_S3_ENDPOINT` (ver `.env.example`).

## Ejecutar tests

Todos los tests (unitarios, integración y E2E) usan H2 con el perfil `local`:

```bash
./mvnw test
```

La pirámide de tests incluye:

- **Unitarios** — Dominio y use cases con InMemory repos. Sin Spring.
- **Integración** — Adaptadores JPA contra H2 con esquema Flyway.
- **E2E** — Endpoints HTTP completos con Spring Boot y datos semilla.

## Datos de prueba (seed)

El archivo `V2__seed_data.sql` carga al arrancar en perfil `local`

Ejemplo de inscripción con curl:

```bash
curl -X POST http://localhost:8080/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": "s-001", "courseIds": ["c-001", "c-002"]}'
```

## Contratos HTTP

### GET /courses

Respuesta `200`:

```json
[
  { "id": "c-001", "name": "Introducción a Java", "instructor": "María González", "durationHours": 40, "price": 150000.0 }
]
```

### POST /courses

Body:

```json
{ "name": "Nuevo curso", "instructor": "Prof", "durationHours": 20, "price": 100000 }
```

Respuesta `201`:

```json
{ "id": "...", "name": "Nuevo curso", "instructor": "Prof", "durationHours": 20, "price": 100000.0 }
```

### POST /enrollments

Body:

```json
{ "studentId": "s-001", "courseIds": ["c-001", "c-002"] }
```

Respuesta `201`:

```json
{
  "enrollmentId": "...",
  "studentId": "s-001",
  "lines": [
    { "courseId": "c-001", "courseName": "Introducción a Java", "unitPrice": 150000.0 },
    { "courseId": "c-002", "courseName": "Bases de datos", "unitPrice": 120000.0 }
  ],
  "totalAmount": 270000.0
}
```

### PUT /enrollments/{enrollmentId}

Body (solo `courseIds`; el `studentId` no cambia):

```json
{ "courseIds": ["c-001", "c-003"] }
```

Si existe resumen en S3, se regenera y reemplaza automáticamente.

## Seguridad

En producción, la capa de seguridad **activa hoy** es **AWS API Gateway** con un **JWT Authorizer** configurado contra **Azure AD B2C**. Los clientes (Postman, frontend, integraciones) deben consumir la API a través del **Invoke URL** del Gateway enviando `Authorization: Bearer <access_token>`; el tráfico directo a `http://<IP_EC2>:8080` no pasa por esa validación.

Adicionalmente, el proyecto incluye una capa **opcional** de JWT en Spring Boot (`ENROLLMENT_SECURITY_JWT_ENABLED`, desactivada por defecto) para defensa en profundidad si el puerto 8080 queda expuesto.

Diagramas, variables de entorno, escenarios (Gateway solo, Spring solo, ambas capas) y cómo activar el toggle: **[docs/seguridad-jwt.md](docs/seguridad-jwt.md)**.

## Producción con Docker

La imagen usa Java 21, perfil `prod`, wallet Oracle en `/app/wallet` y puerto **8080**. El despliegue automático a EC2 se hace con GitHub Actions (`.github/workflows/docker-deploy.yml`) al hacer push a `main`.

### Prerrequisitos

- Carpeta `Wallet_ENROLLMENTPLATFORMDB/` completa (incluye `cwallet.sso` o `ewallet.pem`, `tnsnames.ora`, etc.).
- Archivo `.env` con credenciales Oracle (copia desde `.env.docker.example`).
- En EC2: Docker instalado, security group con TCP **8080** abierto.

### Build y ejecución local

Los endpoints de `/enrollments` y resúmenes S3 requieren LocalStack y el bucket S3 (igual que con `./run-local.sh`). Sin esto, esas rutas responden **500**.

**1. LocalStack y bucket S3**

```bash
docker compose up -d localstack
docker exec "$(docker ps -qf 'ancestor=localstack/localstack:4.4.0')" \
  awslocal s3 mb s3://enrollment-platform-summaries
```

**2. Variables de entorno**

```bash
cp .env.docker.example .env
# Edita .env: usuario/contraseña Oracle y variables AWS.
# Con Docker Compose (misma red que localstack):
#   AWS_S3_ENDPOINT=http://localstack:4566
#   AWS_ACCESS_KEY_ID=test
#   AWS_SECRET_ACCESS_KEY=test
#   AWS_S3_BUCKET=enrollment-platform-summaries
```

**3. Imagen y contenedor**

```bash
docker build -t enrollment-platform .
docker run -d --name enrollment-platform -p 8080:8080 --env-file .env enrollment-platform
```

Si usas `docker run` sin la red de Compose, LocalStack en el host se alcanza con `AWS_S3_ENDPOINT=http://host.docker.internal:4566` (macOS/Windows).

**Con Docker Compose** (app + LocalStack en la misma red; recomendado en local):

```bash
docker compose up -d --build
```

Verificar:

```bash
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": "s-001", "courseIds": ["c-001", "c-002"]}'
curl http://localhost:8080/enrollments/summaries
```

### CI/CD (GitHub Actions + Docker Hub + EC2)

Guía paso a paso (secrets, EC2, push a `main`, verificación): **[docs/guia-despliegue-ec2.md](docs/guia-despliegue-ec2.md)**. Seguridad JWT (Gateway + toggle Spring): **[docs/seguridad-jwt.md](docs/seguridad-jwt.md)**.

| Evento | Acción |
| ------ | ------ |
| Pull request → `main` | `./mvnw test` |
| Push → `main` | Tests, build/push imagen, deploy SSH a EC2 |

## Errores


| Código | Situación                                                             |
| ------ | --------------------------------------------------------------------- |
| 400    | Campo requerido faltante o de tipo incorrecto                         |
| 404    | Estudiante o curso no encontrado                                      |
| 422    | Regla de negocio violada (nombre vacío, lista vacía, precio negativo) |
| 500    | Error técnico inesperado                                              |


