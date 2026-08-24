#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly TEST_VERSION="0.9.29-test.1"
readonly TEST_APP_IMAGE="ghcr.io/eltgm/configurator-app:stable"
readonly TEST_GATEWAY_IMAGE="ghcr.io/eltgm/configurator-web:stable"
readonly TEMP_DIRECTORY="$(mktemp -d "${TMPDIR:-/tmp}/configurator-archive-test.XXXXXX")"
trap 'rm -rf "${TEMP_DIRECTORY}"' EXIT

fail() {
  echo "archive-contract: $*" >&2
  exit 1
}

if "${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" invalid-version \
  "${TEMP_DIRECTORY}/invalid" >/dev/null 2>&1; then
  fail 'invalid package version was accepted'
fi

"${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" \
  "${TEST_VERSION}" "${TEMP_DIRECTORY}/output"

if CONFIGURATOR_RELEASE_APP_IMAGE="${TEST_APP_IMAGE}" \
  "${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" \
  "${TEST_VERSION}" "${TEMP_DIRECTORY}/missing-image" >/dev/null 2>&1; then
  fail 'one-sided release image override was accepted'
fi
if CONFIGURATOR_RELEASE_CHANNEL='Stable Invalid' \
  "${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" \
  "${TEST_VERSION}" "${TEMP_DIRECTORY}/invalid-channel" >/dev/null 2>&1; then
  fail 'invalid release channel was accepted'
fi

CONFIGURATOR_RELEASE_CHANNEL=stable \
  CONFIGURATOR_RELEASE_APP_IMAGE="${TEST_APP_IMAGE}" \
  CONFIGURATOR_RELEASE_GATEWAY_IMAGE="${TEST_GATEWAY_IMAGE}" \
  "${REPOSITORY_ROOT}/scripts/release/build-delivery-packages.sh" \
  "${TEST_VERSION}" "${TEMP_DIRECTORY}/overridden-output"

readonly WINDOWS_ARCHIVE="${TEMP_DIRECTORY}/output/configurator-windows-v${TEST_VERSION}.zip"
readonly MACOS_ARCHIVE="${TEMP_DIRECTORY}/output/configurator-macos-v${TEST_VERSION}.tar.gz"
readonly OVERRIDDEN_MACOS_ARCHIVE="${TEMP_DIRECTORY}/overridden-output/configurator-macos-v${TEST_VERSION}.tar.gz"

[[ -s "${WINDOWS_ARCHIVE}" ]] || fail 'Windows archive is missing'
[[ -s "${MACOS_ARCHIVE}" ]] || fail 'macOS archive is missing'
(
  cd "${TEMP_DIRECTORY}/output"
  shasum -a 256 -c SHA256SUMS >/dev/null
)

mkdir -p "${TEMP_DIRECTORY}/windows" "${TEMP_DIRECTORY}/macos"
unzip -q "${WINDOWS_ARCHIVE}" -d "${TEMP_DIRECTORY}/windows"
tar -xzf "${MACOS_ARCHIVE}" -C "${TEMP_DIRECTORY}/macos"
mkdir -p "${TEMP_DIRECTORY}/overridden-macos"
tar -xzf "${OVERRIDDEN_MACOS_ARCHIVE}" -C "${TEMP_DIRECTORY}/overridden-macos"
grep -Fxq "CONFIGURATOR_CHANNEL=stable" \
  "${TEMP_DIRECTORY}/overridden-macos/Configurator/configurator.env" ||
  fail 'release channel was not injected'
grep -Fxq "CONFIGURATOR_APP_IMAGE=${TEST_APP_IMAGE}" \
  "${TEMP_DIRECTORY}/overridden-macos/Configurator/configurator.env" ||
  fail 'app release image was not injected'
grep -Fxq "CONFIGURATOR_GATEWAY_IMAGE=${TEST_GATEWAY_IMAGE}" \
  "${TEMP_DIRECTORY}/overridden-macos/Configurator/configurator.env" ||
  fail 'gateway release image was not injected'

validate_common_package() {
  local package_root="$1"
  local expected_os="$2"

  for file in compose.yaml configurator.env README.txt LICENSE.txt VERSION; do
    [[ -f "${package_root}/${file}" ]] || fail "${expected_os}: missing ${file}"
  done
  for directory in scripts backups logs; do
    [[ -d "${package_root}/${directory}" ]] || fail "${expected_os}: missing ${directory}/"
  done
  [[ "$(<"${package_root}/VERSION")" == "${TEST_VERSION}" ]] || fail "${expected_os}: invalid VERSION"
  grep -qx "CONFIGURATOR_PACKAGE_VERSION=${TEST_VERSION}" "${package_root}/configurator.env" ||
    fail "${expected_os}: package version was not injected"
  if find "${package_root}" \
    \( -name src -o -name build -o -name .git -o -name gradlew -o -name package.json \
    -o -name .DS_Store -o -name testcontainers.properties \) \
    -print -quit | grep -q .; then
    fail "${expected_os}: source/build tooling leaked into package"
  fi
}

readonly WINDOWS_ROOT="${TEMP_DIRECTORY}/windows/Configurator"
readonly MACOS_ROOT="${TEMP_DIRECTORY}/macos/Configurator"
validate_common_package "${WINDOWS_ROOT}" windows
validate_common_package "${MACOS_ROOT}" macos

for operation in Start Stop Update Backup Restore; do
  [[ -f "${WINDOWS_ROOT}/${operation}.cmd" ]] || fail "Windows: missing ${operation}.cmd"
  [[ -f "${MACOS_ROOT}/${operation}.command" ]] || fail "macOS: missing ${operation}.command"
  [[ -x "${MACOS_ROOT}/${operation}.command" ]] || fail "macOS: ${operation}.command is not executable"
done
[[ -f "${WINDOWS_ROOT}/scripts/configurator.ps1" ]] || fail 'Windows dispatcher is missing'
[[ -x "${MACOS_ROOT}/scripts/configurator.sh" ]] || fail 'macOS dispatcher is not executable'

while IFS= read -r -d '' windows_script; do
  perl -0777 -ne 'exit((/(?<!\r)\n/) ? 1 : 0)' "${windows_script}" ||
    fail "Windows script contains non-CRLF line endings: ${windows_script}"
done < <(find "${WINDOWS_ROOT}" -type f \( -name '*.cmd' -o -name '*.ps1' \) -print0)

[[ "$(od -An -tx1 -N3 "${WINDOWS_ROOT}/scripts/configurator.ps1" | tr -d ' \n')" == "efbbbf" ]] ||
  fail 'Windows PowerShell dispatcher has no UTF-8 BOM'

bash -n "${MACOS_ROOT}/scripts/configurator.sh" "${MACOS_ROOT}"/*.command

CONFIGURATOR_MAINTENANCE_DIR="${TEMP_DIRECTORY}/maintenance" \
  CONFIGURATOR_MAINTENANCE_USER="$(id -u):$(id -g)" \
  docker compose --project-directory "${MACOS_ROOT}" \
  --env-file "${MACOS_ROOT}/configurator.env" \
  -f "${MACOS_ROOT}/compose.yaml" --profile maintenance config --quiet

echo 'archive-contract: OK'
