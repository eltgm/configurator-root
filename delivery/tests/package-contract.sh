#!/bin/bash

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE="$REPOSITORY_ROOT/delivery/common/compose.yaml"
ENV_FILE="$REPOSITORY_ROOT/delivery/common/configurator.env"
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/configurator-package-contract.XXXXXX")

cleanup() {
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT HUP INT TERM

fail() {
  echo "package-contract: $1" >&2
  exit 1
}

test -f "$COMPOSE_FILE" || fail "compose.yaml is missing"
test -f "$ENV_FILE" || fail "configurator.env is missing"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config >"$TEMP_DIR/config.yaml"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --services >"$TEMP_DIR/core-services.txt"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile maintenance config \
  >"$TEMP_DIR/maintenance.yaml"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile maintenance config --services >"$TEMP_DIR/services.txt"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --profiles >"$TEMP_DIR/profiles.txt"

if grep -Eq '^[[:space:]]+build:' "$TEMP_DIR/config.yaml"; then
  fail "end-user Compose must not contain build"
fi
if grep -Eq '0\.0\.0\.0|(^|[^0-9])8081:|(^|[^0-9])5432:|(^|[^0-9])9000:|(^|[^0-9])9001:' "$TEMP_DIR/config.yaml"; then
  fail "end-user Compose exposes a forbidden host port"
fi
grep -Fq 'host_ip: 127.0.0.1' "$TEMP_DIR/config.yaml" || fail "gateway is not bound to loopback"
grep -Fq 'published: "8080"' "$TEMP_DIR/config.yaml" || fail "gateway port is missing"
grep -Fxq 'maintenance' "$TEMP_DIR/profiles.txt" || fail "maintenance profile is missing"
grep -Fq 'read_only: true' "$TEMP_DIR/config.yaml" || fail "read-only hardening is missing"
grep -Fq 'no-new-privileges:true' "$TEMP_DIR/config.yaml" || fail "no-new-privileges hardening is missing"
test "$(grep -Ec "user: ['\"]?0:0['\"]?" "$TEMP_DIR/maintenance.yaml")" -eq 2 ||
  fail "maintenance services do not use the default container user"

for maintenance_service in postgres-maintenance minio-maintenance; do
  if grep -Fxq "$maintenance_service" "$TEMP_DIR/core-services.txt"; then
    fail "$maintenance_service starts outside maintenance profile"
  fi
done

for service in postgres minio app gateway postgres-maintenance minio-maintenance; do
  grep -Fxq "$service" "$TEMP_DIR/services.txt" || fail "service $service is missing"
done

CONFIGURATOR_APP_IMAGE=configurator-app:test \
  CONFIGURATOR_GATEWAY_IMAGE=configurator-web:test \
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config >"$TEMP_DIR/overridden.yaml"
grep -Fq 'image: configurator-app:test' "$TEMP_DIR/overridden.yaml" || fail "app image override failed"
grep -Fq 'image: configurator-web:test' "$TEMP_DIR/overridden.yaml" || fail "gateway image override failed"

CONFIGURATOR_MAINTENANCE_USER=1234:5678 \
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile maintenance config \
  >"$TEMP_DIR/maintenance-user.yaml"
test "$(grep -Fc 'user: 1234:5678' "$TEMP_DIR/maintenance-user.yaml")" -eq 2 ||
  fail "maintenance user override failed"

grep -v '^CONFIGURATOR_APP_IMAGE=' "$ENV_FILE" >"$TEMP_DIR/missing-app-image.env"
if env -u CONFIGURATOR_APP_IMAGE docker compose --env-file "$TEMP_DIR/missing-app-image.env" \
  -f "$COMPOSE_FILE" config >/dev/null 2>&1; then
  fail "missing app image was accepted"
fi

for image_variable in CONFIGURATOR_POSTGRES_IMAGE CONFIGURATOR_MINIO_IMAGE CONFIGURATOR_MINIO_CLIENT_IMAGE; do
  grep -E "^${image_variable}=.+@sha256:[0-9a-f]{64}$" "$ENV_FILE" >/dev/null ||
    fail "$image_variable is not digest-pinned"
done

echo "package-contract: OK"
