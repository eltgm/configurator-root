#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly APP_IMAGE="${CONFIGURATOR_TEST_APP_IMAGE:-configurator-root-app}"
readonly GATEWAY_IMAGE="${CONFIGURATOR_TEST_GATEWAY_IMAGE:-configurator-web:local}"
readonly TEST_VERSION="0.9.29-docker-test.1"
readonly PROJECT_NAME="configurator-delivery-contract-${RANDOM}-$$"
mkdir -p "${REPOSITORY_ROOT}/delivery-output"
readonly TEMP_DIRECTORY="$(mktemp -d "${REPOSITORY_ROOT}/delivery-output/docker-contract.XXXXXX")"
readonly PACKAGE_ROOT="${TEMP_DIRECTORY}/package with spaces/Configurator"
readonly MAINTENANCE_DIRECTORY="${TEMP_DIRECTORY}/maintenance"
REGISTRY_CONTAINER=""

compose() {
  CONFIGURATOR_MAINTENANCE_DIR="${MAINTENANCE_DIRECTORY}" \
    docker compose --project-directory "${PACKAGE_ROOT}" \
    --env-file "${PACKAGE_ROOT}/configurator.env" -f "${PACKAGE_ROOT}/compose.yaml" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  if [[ -n "${REGISTRY_CONTAINER}" ]]; then
    docker rm --force "${REGISTRY_CONTAINER}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${APP_PREVIEW_IMAGE:-}" ]]; then
    docker image rm "${APP_PREVIEW_IMAGE}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${GATEWAY_PREVIEW_IMAGE:-}" ]]; then
    docker image rm "${GATEWAY_PREVIEW_IMAGE}" >/dev/null 2>&1 || true
  fi
  rm -rf "${TEMP_DIRECTORY}"
}
trap cleanup EXIT HUP INT TERM

fail() {
  echo "docker-lifecycle-contract: $*" >&2
  compose ps >&2 || true
  compose logs --no-color --tail 120 app gateway postgres minio >&2 || true
  exit 1
}

docker image inspect "${APP_IMAGE}" >/dev/null 2>&1 ||
  fail "missing app image ${APP_IMAGE}; build docker-compose.yml first"
docker image inspect "${GATEWAY_IMAGE}" >/dev/null 2>&1 ||
  fail "missing gateway image ${GATEWAY_IMAGE}; build docker-compose.yml first"

REGISTRY_CONTAINER="${PROJECT_NAME}-registry"
registry_port=""
for candidate_port in $(seq 5100 5199); do
  if docker run --detach --name "${REGISTRY_CONTAINER}" \
    --publish "${candidate_port}:5000" registry:2.8.3 >/dev/null 2>&1; then
    registry_port=${candidate_port}
    break
  fi
  docker rm --force "${REGISTRY_CONTAINER}" >/dev/null 2>&1 || true
done
[[ -n "${registry_port}" ]] || fail 'local test registry port was not resolved'
registry_ready=0
for _ in $(seq 1 30); do
  if curl --fail --silent "http://127.0.0.1:${registry_port}/v2/" >/dev/null; then
    registry_ready=1
    break
  fi
  sleep 1
done
[[ "${registry_ready}" -eq 1 ]] || fail 'local test registry did not become ready'
readonly APP_PREVIEW_IMAGE="localhost:${registry_port}/configurator-app:preview"
readonly GATEWAY_PREVIEW_IMAGE="localhost:${registry_port}/configurator-web:preview"
docker tag "${APP_IMAGE}" "${APP_PREVIEW_IMAGE}"
docker tag "${GATEWAY_IMAGE}" "${GATEWAY_PREVIEW_IMAGE}"
docker push "${APP_PREVIEW_IMAGE}" >/dev/null
docker push "${GATEWAY_PREVIEW_IMAGE}" >/dev/null

"${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" \
  "${TEST_VERSION}" "${TEMP_DIRECTORY}/archives" >/dev/null
mkdir -p "$(dirname "${PACKAGE_ROOT}")" "${MAINTENANCE_DIRECTORY}"
chmod 755 "${MAINTENANCE_DIRECTORY}"
tar -xzf "${TEMP_DIRECTORY}/archives/configurator-macos-v${TEST_VERSION}.tar.gz" \
  -C "$(dirname "${PACKAGE_ROOT}")"

sed -i.bak \
  -e "s|^name: configurator$|name: ${PROJECT_NAME}|" \
  -e "s|^    image: \${CONFIGURATOR_APP_IMAGE}$|    image: ${APP_PREVIEW_IMAGE}|" \
  -e "s|^    image: \${CONFIGURATOR_GATEWAY_IMAGE}$|    image: ${GATEWAY_PREVIEW_IMAGE}|" \
  "${PACKAGE_ROOT}/compose.yaml"
rm "${PACKAGE_ROOT}/compose.yaml.bak"

export CONFIGURATOR_DOCKER_WAIT_SECONDS=10
export CONFIGURATOR_READINESS_WAIT_SECONDS=120

"${PACKAGE_ROOT}/scripts/configurator.sh" start --non-interactive --no-open || fail 'Start failed'
curl --fail --silent --show-error 'http://127.0.0.1:8080/healthz' >/dev/null || fail 'gateway health failed'

domain_json=$(curl --fail --silent --show-error -X POST 'http://127.0.0.1:8080/api/domains/demo') ||
  fail 'demo domain creation failed'
domain_id=$(printf '%s' "${domain_json}" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')
[[ -n "${domain_id}" ]] || fail 'created domain id was not returned'

printf '%s' 'delivery-contract-object' >"${MAINTENANCE_DIRECTORY}/marker.txt"
compose run --rm --no-deps minio-maintenance \
  mb --ignore-existing configurator/configurator-components >/dev/null ||
  fail 'MinIO bucket creation failed'
compose run --rm --no-deps minio-maintenance \
  cp /backup/marker.txt configurator/configurator-components/delivery-contract/marker.txt >/dev/null ||
  fail 'MinIO marker creation failed'

"${PACKAGE_ROOT}/scripts/configurator.sh" backup --non-interactive --no-open || fail 'Backup failed'
backup_directory=$(find "${PACKAGE_ROOT}/backups" -mindepth 1 -maxdepth 1 -type d \
  ! -name '*.partial' ! -name 'pre-*' | LC_ALL=C sort | tail -1)
[[ -n "${backup_directory}" ]] || fail 'completed backup is missing'
[[ -s "${backup_directory}/database.dump" ]] || fail 'database dump is empty'
[[ -f "${backup_directory}/minio/delivery-contract/marker.txt" ]] || fail 'MinIO object is absent from backup'

compose run --rm --no-deps postgres-maintenance \
  psql --set ON_ERROR_STOP=1 --command \
  "UPDATE domain SET name = 'Delivery contract mutated' WHERE id = ${domain_id};" >/dev/null ||
  fail 'PostgreSQL mutation before Restore failed'
compose run --rm --no-deps minio-maintenance \
  rm --force configurator/configurator-components/delivery-contract/marker.txt >/dev/null ||
  fail 'MinIO mutation before Restore failed'

curl --fail --silent "http://127.0.0.1:8080/api/domains/${domain_id}" |
  grep -Fq 'Delivery contract mutated' || fail 'PostgreSQL mutation is not visible before Restore'

"${PACKAGE_ROOT}/scripts/configurator.sh" restore --non-interactive --no-open --yes \
  --backup "${backup_directory}" || fail 'Restore failed'

restored_domain=$(curl --fail --silent --show-error "http://127.0.0.1:8080/api/domains/${domain_id}") ||
  fail 'PostgreSQL data was not restored'
if printf '%s' "${restored_domain}" | grep -Fq 'Delivery contract mutated'; then
  fail 'PostgreSQL mutation survived Restore'
fi
restored_object=$(compose run --rm --no-deps minio-maintenance \
  cat configurator/configurator-components/delivery-contract/marker.txt 2>/dev/null) ||
  fail 'MinIO data was not restored'
[[ "${restored_object}" == 'delivery-contract-object' ]] || fail 'restored MinIO object differs'

"${PACKAGE_ROOT}/scripts/configurator.sh" update --non-interactive --no-open ||
  fail 'Update with reachable preview images failed'

postgres_image=$(sed -n 's/^CONFIGURATOR_POSTGRES_IMAGE=//p' "${PACKAGE_ROOT}/configurator.env")
docker tag "${postgres_image}" "${APP_PREVIEW_IMAGE}"
docker push "${APP_PREVIEW_IMAGE}" >/dev/null
# Keep the currently running/local preview healthy so the mandatory backup can complete.
# The subsequent pull must be the point where the intentionally non-ready remote image becomes active.
docker tag "${APP_IMAGE}" "${APP_PREVIEW_IMAGE}"
export CONFIGURATOR_READINESS_WAIT_SECONDS=30
set +e
"${PACKAGE_ROOT}/scripts/configurator.sh" update --non-interactive --no-open
strict_update_exit=$?
set -e
[[ "${strict_update_exit}" -eq 60 ]] || fail "strict Update returned ${strict_update_exit}, expected 60"
[[ -z "$(compose ps --status running --quiet app)" ]] || fail 'app is running after strict Update failure'
[[ -z "$(compose ps --status running --quiet gateway)" ]] || fail 'gateway is running after strict Update failure'
if grep -R -Fq 'configurator-local-preview' "${PACKAGE_ROOT}/logs" "${PACKAGE_ROOT}/backups"; then
  fail 'local credentials leaked into logs or backup metadata'
fi

"${PACKAGE_ROOT}/scripts/configurator.sh" stop --non-interactive --no-open || fail 'Stop failed'
echo 'docker-lifecycle-contract: OK'
