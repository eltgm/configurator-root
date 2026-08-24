#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly VERSION="${1:-}"
readonly OUTPUT_DIRECTORY="${2:-${REPOSITORY_ROOT}/delivery-output}"
readonly RELEASE_CHANNEL="${CONFIGURATOR_RELEASE_CHANNEL:-preview}"
readonly RELEASE_APP_IMAGE="${CONFIGURATOR_RELEASE_APP_IMAGE:-}"
readonly RELEASE_GATEWAY_IMAGE="${CONFIGURATOR_RELEASE_GATEWAY_IMAGE:-}"

if [[ ! "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Usage: $0 X.Y.Z [output-directory]" >&2
  exit 2
fi

if [[ ! "${RELEASE_CHANNEL}" =~ ^[a-z0-9][a-z0-9._-]*$ ]]; then
  echo "Invalid release channel: ${RELEASE_CHANNEL}" >&2
  exit 2
fi

if [[ -n "${RELEASE_APP_IMAGE}" || -n "${RELEASE_GATEWAY_IMAGE}" ]]; then
  if [[ -z "${RELEASE_APP_IMAGE}" || -z "${RELEASE_GATEWAY_IMAGE}" ]]; then
    echo "Both CONFIGURATOR_RELEASE_APP_IMAGE and CONFIGURATOR_RELEASE_GATEWAY_IMAGE are required" >&2
    exit 2
  fi
  for image_reference in "${RELEASE_APP_IMAGE}" "${RELEASE_GATEWAY_IMAGE}"; do
    if [[ ! "${image_reference}" =~ ^[a-z0-9]+([._-][a-z0-9]+)*(\.[a-z0-9]+([._-][a-z0-9]+)*)*(:[0-9]+)?/[a-z0-9]+([._/-][a-z0-9]+)*:[a-z0-9][a-z0-9._-]*$ ]]; then
      echo "Invalid release image reference: ${image_reference}" >&2
      exit 2
    fi
  done
fi

for command in gzip zip tar shasum; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is missing: ${command}" >&2
    exit 3
  fi
done

readonly WORK_DIRECTORY="$(mktemp -d "${TMPDIR:-/tmp}/configurator-delivery.XXXXXX")"
trap 'rm -rf "${WORK_DIRECTORY}"' EXIT

mkdir -p "${OUTPUT_DIRECTORY}"
find "${OUTPUT_DIRECTORY}" -maxdepth 1 -type f \
  \( -name "configurator-windows-v${VERSION}.zip" \
  -o -name "configurator-macos-v${VERSION}.tar.gz" \
  -o -name "SHA256SUMS" \) -delete

stage_common_files() {
  local package_root="$1"

  mkdir -p "${package_root}/scripts" "${package_root}/backups" "${package_root}/logs"
  cp "${REPOSITORY_ROOT}/delivery/common/compose.yaml" "${package_root}/compose.yaml"
  cp "${REPOSITORY_ROOT}/delivery/common/configurator.env" "${package_root}/configurator.env"
  cp "${REPOSITORY_ROOT}/delivery/common/README.txt" "${package_root}/README.txt"
  cp "${REPOSITORY_ROOT}/LICENSE" "${package_root}/LICENSE.txt"
  printf '%s\n' "${VERSION}" >"${package_root}/VERSION"

  sed -i.bak \
    -e "s/^CONFIGURATOR_PACKAGE_VERSION=.*/CONFIGURATOR_PACKAGE_VERSION=${VERSION}/" \
    -e "s/^CONFIGURATOR_CHANNEL=.*/CONFIGURATOR_CHANNEL=${RELEASE_CHANNEL}/" \
    "${package_root}/configurator.env"
  if [[ -n "${RELEASE_APP_IMAGE}" ]]; then
    sed -i.bak \
      -e "s|^CONFIGURATOR_APP_IMAGE=.*|CONFIGURATOR_APP_IMAGE=${RELEASE_APP_IMAGE}|" \
      -e "s|^CONFIGURATOR_GATEWAY_IMAGE=.*|CONFIGURATOR_GATEWAY_IMAGE=${RELEASE_GATEWAY_IMAGE}|" \
      "${package_root}/configurator.env"
  fi
  rm "${package_root}/configurator.env.bak"

}

stage_windows_package() {
  local package_root="${WORK_DIRECTORY}/windows/Configurator"

  stage_common_files "${package_root}"
  cp "${REPOSITORY_ROOT}"/delivery/windows/*.cmd "${package_root}/"
  cp "${REPOSITORY_ROOT}/delivery/windows/scripts/configurator.ps1" "${package_root}/scripts/"

  while IFS= read -r -d '' file; do
    perl -pi -e 's/\r?\n/\r\n/g' "${file}"
  done < <(find "${package_root}" -type f \( -name '*.cmd' -o -name '*.ps1' \) -print0)

  if [[ "$(od -An -tx1 -N3 "${package_root}/scripts/configurator.ps1" | tr -d ' \n')" != "efbbbf" ]]; then
    local with_bom="${package_root}/scripts/configurator.ps1.bom"
    printf '\357\273\277' >"${with_bom}"
    tail -c +1 "${package_root}/scripts/configurator.ps1" >>"${with_bom}"
    mv "${with_bom}" "${package_root}/scripts/configurator.ps1"
  fi

  find "${WORK_DIRECTORY}/windows" -exec touch -t 198001010000 {} +
  (
    cd "${WORK_DIRECTORY}/windows"
    zip -X -q -r "${OUTPUT_DIRECTORY}/configurator-windows-v${VERSION}.zip" Configurator
  )
}

stage_macos_package() {
  local package_root="${WORK_DIRECTORY}/macos/Configurator"
  local tar_file="${WORK_DIRECTORY}/configurator-macos-v${VERSION}.tar"

  stage_common_files "${package_root}"
  cp "${REPOSITORY_ROOT}"/delivery/macos/*.command "${package_root}/"
  cp "${REPOSITORY_ROOT}/delivery/macos/scripts/configurator.sh" "${package_root}/scripts/"
  chmod 755 "${package_root}"/*.command "${package_root}/scripts/configurator.sh"
  find "${WORK_DIRECTORY}/macos" -exec touch -t 198001010000 {} +

  if tar --version 2>/dev/null | grep -q 'GNU tar'; then
    tar --sort=name --mtime='1980-01-01 00:00:00Z' --owner=0 --group=0 --numeric-owner \
      -C "${WORK_DIRECTORY}/macos" -cf "${tar_file}" Configurator
  else
    COPYFILE_DISABLE=1 tar --uid 0 --gid 0 --uname root --gname root \
      -C "${WORK_DIRECTORY}/macos" -cf "${tar_file}" Configurator
  fi
  gzip -n -9 -c "${tar_file}" >"${OUTPUT_DIRECTORY}/configurator-macos-v${VERSION}.tar.gz"
}

stage_windows_package
stage_macos_package

(
  cd "${OUTPUT_DIRECTORY}"
  shasum -a 256 \
    "configurator-windows-v${VERSION}.zip" \
    "configurator-macos-v${VERSION}.tar.gz" >SHA256SUMS
)

echo "Delivery packages created in ${OUTPUT_DIRECTORY}"
