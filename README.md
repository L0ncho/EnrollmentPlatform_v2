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
│   ├── application/        # CreateEnrollmentUseCase, EnrollmentSummaryDTO
│   └── infrastructure/     # JPA adapters, EnrollmentController
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

**Perfil `local` (H2)**

```bash
./run-local.sh
```

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

| Método | URL                | Descripción                  |
| ------ | ------------------ | ---------------------------- |
| GET    | `/courses`         | Lista cursos                 |
| POST   | `/courses`         | Crea curso                   |
| POST   | `/enrollments`     | Inscribe estudiante          |
| GET    | `/actuator/health` | Health check                 |
| GET    | `/h2-console`      | Consola H2 (solo perfil local) |

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

## Producción con Docker

La imagen usa Java 21, perfil `prod`, wallet Oracle en `/app/wallet` y puerto **8080**. El despliegue automático a EC2 se hace con GitHub Actions (`.github/workflows/docker-deploy.yml`) al hacer push a `main`.

### Prerrequisitos

- Carpeta `Wallet_ENROLLMENTPLATFORMDB/` completa (incluye `cwallet.sso` o `ewallet.pem`, `tnsnames.ora`, etc.).
- Archivo `.env` con credenciales Oracle (copia desde `.env.docker.example`).
- En EC2: Docker instalado, security group con TCP **8080** abierto.

### Build y ejecución local

```bash
cp .env.docker.example .env
# Edita .env con usuario y contraseña Oracle

docker build -t enrollment-platform .
docker run -d --name enrollment-platform -p 8080:8080 --env-file .env enrollment-platform
```

Con Docker Compose (mismo `.env`):

```bash
docker compose up -d --build
```

Verificar: `curl http://localhost:8080/actuator/health`

### CI/CD (GitHub Actions + Docker Hub + EC2)

| Evento | Acción |
| ------ | ------ |
| Pull request → `main` | `./mvnw test` |
| Push → `main` | Tests, build/push imagen, deploy SSH a EC2 |

**Secrets en el repositorio GitHub:**

| Secret | Uso |
| ------ | --- |
| `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` | Publicar imagen `usuario/enrollment-platform:latest` |
| `ORACLE_WALLET_BASE64` | Zip de `Wallet_ENROLLMENTPLATFORMDB` en base64 (ver abajo) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciales Oracle en el contenedor |
| `SPRING_DATASOURCE_URL` | Opcional; por defecto `@enrollmentplatformdb_high` |
| `EC2_SSH_KEY`, `EC2_HOST`, `USER_SERVER` | Deploy por SSH |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Credenciales AWS (token solo si son temporales) |

Generar `ORACLE_WALLET_BASE64` (una vez):

```bash
zip -r wallet.zip Wallet_ENROLLMENTPLATFORMDB
base64 -i wallet.zip | pbcopy
```

En EC2 también puedes usar `docker compose up` con `.env` y la imagen de Docker Hub (`DOCKERHUB_USERNAME` en `.env` si usas `image:` en lugar de `build`).

## Errores


| Código | Situación                                                             |
| ------ | --------------------------------------------------------------------- |
| 400    | Campo requerido faltante o de tipo incorrecto                         |
| 404    | Estudiante o curso no encontrado                                      |
| 422    | Regla de negocio violada (nombre vacío, lista vacía, precio negativo) |
| 500    | Error técnico inesperado                                              |


