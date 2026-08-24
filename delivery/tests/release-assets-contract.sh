#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly PREPARE_SCRIPT="${REPOSITORY_ROOT}/scripts/release/prepare-release-assets.sh"
readonly VERSION="0.9.30-test.1"
readonly APP_IMAGE="ghcr.io/eltgm/configurator-app"
readonly GATEWAY_IMAGE="ghcr.io/eltgm/configurator-web"
readonly APP_DIGEST="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly GATEWAY_DIGEST="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
readonly TEMP_DIRECTORY="$(mktemp -d "${TMPDIR:-/tmp}/configurator-release-assets-test.XXXXXX")"
trap 'rm -rf "${TEMP_DIRECTORY}"' EXIT

fail() {
  echo "release-assets-contract: $*" >&2
  exit 1
}

expect_failure() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    fail "${description}"
  fi
}

printf 'deterministic release jar\n' >"${TEMP_DIRECTORY}/input.jar"
printf 'not a directory\n' >"${TEMP_DIRECTORY}/output-file"
mkdir -p "${TEMP_DIRECTORY}/unexpected-output"
printf 'do not delete me\n' >"${TEMP_DIRECTORY}/unexpected-output/unrelated.txt"
ln -s "${TEMP_DIRECTORY}/first" "${TEMP_DIRECTORY}/symlink-output"

expect_failure 'invalid version was accepted' \
  "${PREPARE_SCRIPT}" invalid "${TEMP_DIRECTORY}/input.jar" \
  "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" "${TEMP_DIRECTORY}/invalid"
expect_failure 'missing JAR was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/missing.jar" \
  "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" "${TEMP_DIRECTORY}/missing"
expect_failure 'non-GHCR image name was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
  'docker.io/eltgm/configurator-app' "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" \
  "${TEMP_DIRECTORY}/invalid-image"
expect_failure 'malformed digest was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
  "${APP_IMAGE}" 'sha256:not-a-digest' "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" \
  "${TEMP_DIRECTORY}/invalid-digest"
expect_failure 'file output path was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
  "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" "${TEMP_DIRECTORY}/output-file"
expect_failure 'unexpected output entry was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
  "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" \
  "${TEMP_DIRECTORY}/unexpected-output"
[[ -f "${TEMP_DIRECTORY}/unexpected-output/unrelated.txt" ]] || fail 'unexpected output entry was deleted'
expect_failure 'symbolic-link output directory was accepted' \
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
  "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" "${TEMP_DIRECTORY}/symlink-output"

for run in first second; do
  "${PREPARE_SCRIPT}" "${VERSION}" "${TEMP_DIRECTORY}/input.jar" \
    "${APP_IMAGE}" "${APP_DIGEST}" "${GATEWAY_IMAGE}" "${GATEWAY_DIGEST}" "${TEMP_DIRECTORY}/${run}"
done

readonly OUTPUT_DIRECTORY="${TEMP_DIRECTORY}/first"
readonly EXPECTED_ASSETS="IMAGE_DIGESTS
SHA256SUMS
configurator-${VERSION}.jar
configurator-api.yaml
configurator-macos-v${VERSION}.tar.gz
configurator-windows-v${VERSION}.zip"
readonly ACTUAL_ASSETS="$(find "${OUTPUT_DIRECTORY}" -maxdepth 1 -type f -exec basename {} \; | LC_ALL=C sort)"
[[ "${ACTUAL_ASSETS}" == "${EXPECTED_ASSETS}" ]] || fail "unexpected release asset inventory: ${ACTUAL_ASSETS}"

(
  cd "${OUTPUT_DIRECTORY}"
  shasum -a 256 -c SHA256SUMS >/dev/null
)
[[ "$(wc -l <"${OUTPUT_DIRECTORY}/SHA256SUMS" | tr -d ' ')" == 5 ]] ||
  fail 'SHA256SUMS must describe exactly five non-checksum assets'
grep -Fxq "CONFIGURATOR_APP_IMAGE=${APP_IMAGE}@${APP_DIGEST}" "${OUTPUT_DIRECTORY}/IMAGE_DIGESTS" ||
  fail 'immutable app digest reference is missing'
grep -Fxq "CONFIGURATOR_APP_VERSION_TAG=${APP_IMAGE}:${VERSION}" "${OUTPUT_DIRECTORY}/IMAGE_DIGESTS" ||
  fail 'app version tag is missing'
grep -Fxq "CONFIGURATOR_GATEWAY_IMAGE=${GATEWAY_IMAGE}@${GATEWAY_DIGEST}" "${OUTPUT_DIRECTORY}/IMAGE_DIGESTS" ||
  fail 'immutable gateway digest reference is missing'
grep -Fxq "CONFIGURATOR_GATEWAY_VERSION_TAG=${GATEWAY_IMAGE}:${VERSION}" "${OUTPUT_DIRECTORY}/IMAGE_DIGESTS" ||
  fail 'gateway version tag is missing'

cmp "${TEMP_DIRECTORY}/first/SHA256SUMS" "${TEMP_DIRECTORY}/second/SHA256SUMS" >/dev/null ||
  fail 'release assets are not reproducible'

mkdir -p "${TEMP_DIRECTORY}/package"
tar -xzf "${OUTPUT_DIRECTORY}/configurator-macos-v${VERSION}.tar.gz" -C "${TEMP_DIRECTORY}/package"
grep -Fxq "CONFIGURATOR_PACKAGE_VERSION=${VERSION}" "${TEMP_DIRECTORY}/package/Configurator/configurator.env" ||
  fail 'package version was not injected'
grep -Fxq 'CONFIGURATOR_CHANNEL=stable' "${TEMP_DIRECTORY}/package/Configurator/configurator.env" ||
  fail 'stable channel was not injected'
grep -Fxq "CONFIGURATOR_APP_IMAGE=${APP_IMAGE}:stable" \
  "${TEMP_DIRECTORY}/package/Configurator/configurator.env" || fail 'stable app image was not injected'
grep -Fxq "CONFIGURATOR_GATEWAY_IMAGE=${GATEWAY_IMAGE}:stable" \
  "${TEMP_DIRECTORY}/package/Configurator/configurator.env" || fail 'stable gateway image was not injected'

echo 'release-assets-contract: OK'
