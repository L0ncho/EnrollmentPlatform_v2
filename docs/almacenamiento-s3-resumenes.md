# Almacenamiento S3 de resúmenes de inscripción

## Propósito

Cada inscripción puede tener un **comprobante JSON** almacenado en Amazon S3. La base de datos (Oracle/H2) es la fuente de verdad del agregado `Enrollment`; S3 guarda el archivo exportado para descarga, auditoría y corrección manual.

## Jerarquía en el bucket

```text
{bucket}/
  {enrollmentId}/
    summary.json
```

El `enrollmentId` es el identificador del resumen y el nombre de la carpeta, según el requisito del proyecto.

## Sincronización con la base de datos

| Operación API | Efecto en S3 |
|---------------|--------------|
| `POST /enrollments` | Crea `summary.json` (upload automático) |
| `PUT /enrollments/{id}` | Si ya existe el objeto, lo reemplaza con JSON regenerado desde BD |
| `DELETE /enrollments/{id}` | Elimina el objeto si existe |
| `POST /enrollments/{id}/summary` | Vuelve a subir desde el estado actual en BD |
| `PUT /enrollments/{id}/summary` | Reemplaza el JSON con el body enviado |
| `DELETE /enrollments/{id}/summary` | Borra solo el archivo (la inscripción en BD permanece) |

## Variables de entorno

| Variable | Local (LocalStack) | Producción |
|----------|-------------------|------------|
| `AWS_REGION` | `us-east-1` | Región del bucket |
| `AWS_S3_BUCKET` | `enrollment-platform-summaries` | Nombre del bucket |
| `AWS_S3_ENDPOINT` | `http://localhost:4566` | Omitir (SDK usa endpoint regional) |
| `AWS_ACCESS_KEY_ID` | `test` | IAM access key |
| `AWS_SECRET_ACCESS_KEY` | `test` | IAM secret key |
| `AWS_SESSION_TOKEN` | — | Credenciales temporales (AWS Academy) |
| `ENROLLMENT_SUMMARY_STORAGE` | `s3` o `in-memory` (tests usan `in-memory`) | `s3` |

Propiedades Spring: `aws.region`, `aws.s3.bucket`, `aws.s3.endpoint`, `enrollment.summary.storage`.

Local: `./run-local.sh`. Producción: `./run-prod.sh` (sin `AWS_S3_ENDPOINT`).

## Permisos IAM mínimos

Bucket **privado**. La app accede con credenciales IAM vía SDK; un bucket público no reemplaza esos permisos.

La política siguiente va en el **usuario IAM** (inline o managed), no en S3 → Bucket policy:

- `s3:PutObject`
- `s3:GetObject`
- `s3:DeleteObject`
- `s3:ListBucket`

Ejemplo (reemplazar `BUCKET_NAME`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::BUCKET_NAME"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::BUCKET_NAME/*"
    }
  ]
}
```

**AWS Academy:** credenciales en Start Lab → AWS Details (incluir `AWS_SESSION_TOKEN`). Bucket policy suele ser innecesaria; si la consola no permite guardarla, validar acceso con `aws s3 cp` y `aws s3 ls` antes de probar la app. Renovar credenciales al expirar el lab.

**Crear bucket:** `aws s3 mb s3://BUCKET_NAME --region us-east-1` (misma región que `AWS_REGION`, Block Public Access activado).

## Formato del archivo

```json
{
  "enrollmentId": "uuid",
  "studentId": "s-001",
  "studentFullName": "Juan Soto",
  "studentEmail": "juan.soto@duoc.cl",
  "enrolledAt": "2026-05-27T12:00:00",
  "lines": [
    { "courseId": "c-001", "courseName": "Introducción a Java", "unitPrice": 150000.0 }
  ],
  "totalAmount": 270000.0
}
```

`GET /enrollments/{id}/summary?format=pdf` genera un PDF a partir de ese JSON sin guardar el PDF en S3.

## Local con LocalStack

```bash
docker compose up -d localstack
docker exec "$(docker ps -qf 'ancestor=localstack/localstack:4.4.0')" \
  awslocal s3 mb s3://enrollment-platform-summaries
./run-local.sh
```

## Verificación

1. `POST /enrollments` con estudiante y cursos semilla.
2. `GET /enrollments/summaries` — debe listar el `enrollmentId`.
3. `GET /enrollments/{id}/summary` — descarga el JSON.
4. Consola AWS S3 (producción) o `aws s3 ls s3://BUCKET/ --recursive`.

## Troubleshooting

| Síntoma | Causa probable |
|---------|----------------|
| 500 al crear inscripción | Bucket inexistente, credenciales incorrectas, `AWS_SESSION_TOKEN` faltante o sin `PutObject` |
| 404 al descargar resumen | Objeto no subido o `enrollmentId` incorrecto |
| Lista vacía en `/summaries` | Bucket distinto al configurado o prefijo sin objetos |
| Funciona en local pero no en EC2 | Variables S3 no inyectadas en `docker run` (ver [guia-despliegue-ec2.md](guia-despliegue-ec2.md)) |
| Error al guardar bucket policy | Restricción típica en AWS Academy; usar credenciales del lab sin bucket policy |
