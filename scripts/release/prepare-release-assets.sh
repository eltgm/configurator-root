#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly VERSION="${1:-}"
readonly JAR_FILE="${2:-}"
readonly APP_IMAGE_NAME="${3:-}"
readonly APP_IMAGE_DIGEST="${4:-}"
readonly GATEWAY_IMAGE_NAME="${5:-}"
readonly GATEWAY_IMAGE_DIGEST="${6:-}"
readonly OUTPUT_DIRECTORY="${7:-${REPOSITORY_ROOT}/release-assets}"

usage() {
  echo "Usage: $0 X.Y.Z JAR_FILE APP_IMAGE_NAME APP_DIGEST GATEWAY_IMAGE_NAME GATEWAY_DIGEST [OUTPUT_DIRECTORY]" >&2
  exit 2
}

[[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || usage
[[ -f "${JAR_FILE}" && -s "${JAR_FILE}" ]] || {
  echo "Release JAR is missing or empty: ${JAR_FILE}" >&2
  exit 3
}
[[ ! -e "${OUTPUT_DIRECTORY}" || -d "${OUTPUT_DIRECTORY}" ]] || {
  echo "Output path is not a directory: ${OUTPUT_DIRECTORY}" >&2
  exit 3
}
[[ ! -L "${OUTPUT_DIRECTORY}" ]] || {
  echo "Output directory must not be a symbolic link: ${OUTPUT_DIRECTORY}" >&2
  exit 3
}

for image_name in "${APP_IMAGE_NAME}" "${GATEWAY_IMAGE_NAME}"; do
  if [[ ! "${image_name}" =~ ^ghcr\.io/[a-z0-9]+([._-][a-z0-9]+)*/[a-z0-9]+([._-][a-z0-9]+)*$ ]]; then
    echo "Invalid public GHCR image name: ${image_name}" >&2
    exit 2
  fi
done
for image_digest in "${APP_IMAGE_DIGEST}" "${GATEWAY_IMAGE_DIGEST}"; do
  if [[ ! "${image_digest}" =~ ^sha256:[0-9a-f]{64}$ ]]; then
    echo "Invalid OCI image digest: ${image_digest}" >&2
    exit 2
  fi
done

for command in cp mkdir mktemp rm shasum; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command is missing: ${command}" >&2
    exit 3
  }
done

readonly WORK_DIRECTORY="$(mktemp -d "${TMPDIR:-/tmp}/configurator-release-assets.XXXXXX")"
trap 'rm -rf "${WORK_DIRECTORY}"' EXIT
readonly ASSET_DIRECTORY="${WORK_DIRECTORY}/assets"
mkdir -p "${ASSET_DIRECTORY}"

readonly RELEASE_JAR="configurator-${VERSION}.jar"
readonly WINDOWS_ARCHIVE="configurator-windows-v${VERSION}.zip"
readonly MACOS_ARCHIVE="configurator-macos-v${VERSION}.tar.gz"

if [[ -d "${OUTPUT_DIRECTORY}" ]]; then
  while IFS= read -r existing_entry; do
    case "$(basename "${existing_entry}")" in
      "${RELEASE_JAR}" | configurator-api.yaml | "${WINDOWS_ARCHIVE}" | "${MACOS_ARCHIVE}" | IMAGE_DIGESTS | SHA256SUMS) ;;
      *)
        echo "Output directory contains an unexpected entry: ${existing_entry}" >&2
        exit 3
        ;;
    esac
  done < <(find "${OUTPUT_DIRECTORY}" -mindepth 1 -maxdepth 1 -print)
fi

cp "${JAR_FILE}" "${ASSET_DIRECTORY}/${RELEASE_JAR}"
cp "${REPOSITORY_ROOT}/specs/configurator-api.yaml" "${ASSET_DIRECTORY}/configurator-api.yaml"

CONFIGURATOR_RELEASE_CHANNEL=stable \
  CONFIGURATOR_RELEASE_APP_IMAGE="${APP_IMAGE_NAME}:stable" \
  CONFIGURATOR_RELEASE_GATEWAY_IMAGE="${GATEWAY_IMAGE_NAME}:stable" \
  "${SCRIPT_DIR}/build-delivery-packages.sh" "${VERSION}" "${ASSET_DIRECTORY}"

{
  printf 'CONFIGURATOR_APP_IMAGE=%s@%s\n' "${APP_IMAGE_NAME}" "${APP_IMAGE_DIGEST}"
  printf 'CONFIGURATOR_APP_VERSION_TAG=%s:%s\n' "${APP_IMAGE_NAME}" "${VERSION}"
  printf 'CONFIGURATOR_GATEWAY_IMAGE=%s@%s\n' "${GATEWAY_IMAGE_NAME}" "${GATEWAY_IMAGE_DIGEST}"
  printf 'CONFIGURATOR_GATEWAY_VERSION_TAG=%s:%s\n' "${GATEWAY_IMAGE_NAME}" "${VERSION}"
} >"${ASSET_DIRECTORY}/IMAGE_DIGESTS"

(
  cd "${ASSET_DIRECTORY}"
  shasum -a 256 \
    "${RELEASE_JAR}" \
    configurator-api.yaml \
    "${WINDOWS_ARCHIVE}" \
    "${MACOS_ARCHIVE}" \
    IMAGE_DIGESTS >SHA256SUMS
  shasum -a 256 -c SHA256SUMS >/dev/null
)

mkdir -p "${OUTPUT_DIRECTORY}"
for asset in \
  "${RELEASE_JAR}" \
  configurator-api.yaml \
  "${WINDOWS_ARCHIVE}" \
  "${MACOS_ARCHIVE}" \
  IMAGE_DIGESTS \
  SHA256SUMS; do
  rm -f "${OUTPUT_DIRECTORY}/${asset}"
  cp "${ASSET_DIRECTORY}/${asset}" "${OUTPUT_DIRECTORY}/${asset}"
done

echo "Release assets created in ${OUTPUT_DIRECTORY}"
