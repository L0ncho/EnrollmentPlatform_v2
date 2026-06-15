# Guía de despliegue: GitHub Actions → Docker Hub → EC2

Despliegue mediante [`.github/workflows/docker-deploy.yml`](../.github/workflows/docker-deploy.yml).

## Requisitos previos

- Aplicación operativa en local con `run-prod.sh` o Docker.
- Wallet Oracle en `Wallet_ENROLLMENTPLATFORMDB/`.
- Credenciales Oracle en `.env` (mismos valores que se usarán en los secrets).

La wallet y el `.env` no se copian al servidor EC2. La wallet se incluye en la imagen durante el build en CI; usuario y contraseña de Oracle se inyectan en el contenedor desde secrets de GitHub.

---

## 1. Secrets en GitHub

Configurar en **Settings → Secrets and variables → Actions**:

| Secret | Valor |
| ------ | ----- |
| `ORACLE_WALLET_BASE64` | Archivo zip de `Wallet_ENROLLMENTPLATFORMDB` codificado en base64 |
| `SPRING_DATASOURCE_USERNAME` | Usuario Oracle (equivalente a `.env` local) |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña Oracle (equivalente a `.env` local) |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Access token de Docker Hub (no la contraseña de la cuenta) |
| `EC2_HOST` | IP pública de la instancia EC2 |
| `USER_SERVER` | Usuario SSH: `ubuntu` (Ubuntu) o `ec2-user` (Amazon Linux) |
| `EC2_SSH_KEY` | Contenido del archivo `.pem` (líneas `BEGIN` a `END` inclusive) |
| `AWS_ACCESS_KEY_ID` | Access key IAM con permisos S3 sobre el bucket de resúmenes |
| `AWS_SECRET_ACCESS_KEY` | Secret key IAM |
| `AWS_SESSION_TOKEN` | Credenciales STS temporales; omitir si no aplica |
| `AWS_REGION` | Región del bucket S3 (ej. `us-east-1`) |
| `AWS_S3_BUCKET` | Bucket donde se guardan los resúmenes (`{enrollmentId}/summary.json`) |
| `SPRING_DATASOURCE_URL` | Opcional. Por defecto: `jdbc:oracle:thin:@enrollmentplatformdb_high` |
| `ENROLLMENT_SECURITY_JWT_ENABLED` | Opcional. `true` activa JWT en Spring; default: `false` |
| `AZURE_B2C_JWK_SET_URI` | Requerido si JWT Spring activo. URL JWK set de Azure AD B2C |
| `AZURE_B2C_AUDIENCE` | Requerido si JWT Spring activo. Client ID de la app en Azure |

Detalle de las capas de seguridad (Gateway vs Spring): [seguridad-jwt.md](seguridad-jwt.md).

### S3 en producción

1. Crear el bucket en la misma región que `AWS_REGION`.
2. Asignar al IAM de las credenciales permisos `s3:PutObject`, `GetObject`, `DeleteObject` y `ListBucket` sobre ese bucket.
3. Configurar los secrets `AWS_S3_BUCKET` y `AWS_REGION` en GitHub Actions.

Política de ejemplo y formato del resumen: [almacenamiento-s3-resumenes.md](almacenamiento-s3-resumenes.md).

No definir `AWS_S3_ENDPOINT` en producción; el SDK usa el endpoint regional de AWS.

### Generar `ORACLE_WALLET_BASE64`

```bash
cd /ruta/a/EnrollmentPlatform
zip -r wallet.zip Wallet_ENROLLMENTPLATFORMDB
base64 -i wallet.zip | pbcopy
```

Asignar el resultado al secret `ORACLE_WALLET_BASE64`.

### Determinar `USER_SERVER`

Consola AWS → EC2 → **Connect**, o verificación por SSH:

```bash
ssh -i ruta/a/tu-key.pem ubuntu@TU_IP_EC2
# Alternativa (Amazon Linux):
ssh -i ruta/a/tu-key.pem ec2-user@TU_IP_EC2
```

El usuario válido en el comando SSH es el valor de `USER_SERVER`.

### Configurar `EC2_SSH_KEY`

```bash
cat ruta/a/tu-key.pem
```

Copiar la salida completa al secret. No versionar el archivo `.pem` en el repositorio.

---

## 2. Configuración de EC2

- Docker instalado.
- Security group: regla de entrada TCP en el puerto **8080**.
- Acceso SSH con la llave asociada a `EC2_SSH_KEY`.
- No desplegar `Wallet_ENROLLMENTPLATFORMDB/` ni `.env` en el servidor.

---

## 3. Ejecutar el despliegue

1. Publicar el código en GitHub (incluye el workflow).
2. Push o merge a la rama `main`.

| Evento | Pipeline |
| ------ | -------- |
| Pull request → `main` | Solo `./mvnw test` |
| Push → `main` | Tests, build, push a Docker Hub, deploy por SSH |

Secuencia del job `build-and-deploy`:

1. `./mvnw test`
2. Build de imagen Docker (wallet desde `ORACLE_WALLET_BASE64`)
3. Push a `{DOCKERHUB_USERNAME}/enrollment-platform:latest`
4. SSH a EC2: `docker pull`, recreación del contenedor con variables Oracle

---

## 4. Verificación

Sustituir `<IP_EC2>` por la IP pública de la instancia:

```text
http://<IP_EC2>:8080/actuator/health
http://<IP_EC2>:8080/courses
```

Endpoint `/actuator/health`: respuesta con `"status":"UP"`.

Errores de despliegue: revisar el job `build-and-deploy` en **Actions** del repositorio.

---

## Resumen

| Entorno | Wallet | Credenciales Oracle | S3 |
| ------- | ------ | --------------------- | --- |
| Local | `Wallet_ENROLLMENTPLATFORMDB/` | `.env` | `.env` + LocalStack opcional |
| GitHub | `ORACLE_WALLET_BASE64` | `SPRING_DATASOURCE_*` | `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_*` |
| EC2 | Imagen Docker (`/app/wallet`) | Variables en `docker run` (workflow) | Mismas variables S3 en `docker run` |

---

## Docker en local (referencia)

```bash
cp .env.docker.example .env
# Configurar usuario y contraseña Oracle

docker build -t enrollment-platform .
docker run -d --name enrollment-platform -p 8080:8080 --env-file .env enrollment-platform
curl http://localhost:8080/actuator/health
```

Ver también [README.md](../README.md) (sección «Producción con Docker»).
