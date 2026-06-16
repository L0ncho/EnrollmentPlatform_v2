#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Falta .env. Cópialo desde el ejemplo y completa las credenciales Oracle:"
  echo "  cp .env.example .env"
  exit 1
fi

WALLET_DIR="${ORACLE_WALLET_DIR:-$(pwd)/Wallet_ENROLLMENTPLATFORMDB}"
if [[ ! -f "${WALLET_DIR}/tnsnames.ora" ]]; then
  echo "No se encontró la wallet Oracle en: ${WALLET_DIR}"
  echo "Descarga la wallet (Instance Wallet) y colócala en Wallet_ENROLLMENTPLATFORMDB/"
  exit 1
fi

export TNS_ADMIN="${WALLET_DIR}"
export ORACLE_WALLET_DIR="${WALLET_DIR}"

set -a
source .env
set +a

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
fi

echo "Perfil: ${SPRING_PROFILES_ACTIVE:-prod}"
echo "Wallet (TNS_ADMIN): ${TNS_ADMIN}"
echo "JDBC: ${SPRING_DATASOURCE_URL:-jdbc:oracle:thin:@learningplatformvalidation_high}"
./mvnw spring-boot:run
