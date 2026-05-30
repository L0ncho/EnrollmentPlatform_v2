#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

export SPRING_PROFILES_ACTIVE=local

# .env suele tener Oracle (prod); no debe pisar el datasource H2 del perfil local
unset SPRING_DATASOURCE_URL \
      SPRING_DATASOURCE_USERNAME \
      SPRING_DATASOURCE_PASSWORD \
      SPRING_DATASOURCE_DRIVER_CLASS_NAME \
      TNS_ADMIN \
      ORACLE_WALLET_DIR

export AWS_REGION="${AWS_REGION:-us-east-1}"
export AWS_S3_BUCKET="${AWS_S3_BUCKET:-enrollment-platform-summaries}"
export AWS_S3_ENDPOINT="${AWS_S3_ENDPOINT:-http://localhost:4566}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
fi

echo "Perfil: local (H2 en memoria). Verifica en logs: The following 1 profile is active: \"local\""
echo "S3: ${AWS_S3_ENDPOINT} / bucket ${AWS_S3_BUCKET}"
./mvnw spring-boot:run
