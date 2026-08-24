#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly WORKFLOW="${REPOSITORY_ROOT}/.github/workflows/release.yml"
readonly BACKEND_DOCKERFILE="${REPOSITORY_ROOT}/Dockerfile"
readonly GATEWAY_DOCKERFILE="${REPOSITORY_ROOT}/configurator-web/Dockerfile"
readonly PLAYWRIGHT_CONFIG="${REPOSITORY_ROOT}/configurator-web/playwright.config.ts"
readonly WORK_DIRECTORY="$(mktemp -d "${TMPDIR:-/tmp}/configurator-release-workflow-test.XXXXXX")"

trap 'rm -rf "${WORK_DIRECTORY}"' EXIT

fail() {
  echo "release-workflow-contract: $*" >&2
  exit 1
}

require_text() {
  local expected="$1"
  local description="$2"
  grep -Fq -- "${expected}" "${WORKFLOW}" || fail "${description}"
}

[[ -f "${WORKFLOW}" ]] || fail 'release workflow is missing'

readonly DRAFT_RELEASE_SCRIPT="${WORK_DIRECTORY}/draft-release.sh"
awk '
  /^      - name: Create or update draft release$/ { in_step = 1; next }
  in_step && /^        run: \|$/ { in_run = 1; next }
  in_run {
    if ($0 ~ /^          /) {
      sub(/^          /, "")
      print
      next
    }
    exit
  }
' "${WORKFLOW}" >"${DRAFT_RELEASE_SCRIPT}"
[[ -s "${DRAFT_RELEASE_SCRIPT}" ]] || fail 'draft release shell block is missing'
bash -n "${DRAFT_RELEASE_SCRIPT}" || fail 'draft release shell block has invalid syntax'

require_text 'tags:' 'tag trigger is missing'
require_text '^v[0-9]+\.[0-9]+\.[0-9]+$' 'stable workflow must reject pre-release tags'
require_text 'git merge-base --is-ancestor "${GITHUB_SHA}" origin/master' 'master ancestry validation is missing'
require_text 'Release tag must be annotated' 'annotated tag validation is missing'
require_text 'APP_IMAGE: ghcr.io/eltgm/configurator-app' 'backend GHCR image is missing'
require_text 'GATEWAY_IMAGE: ghcr.io/eltgm/configurator-web' 'gateway GHCR image is missing'
require_text 'platforms: linux/amd64,linux/arm64' 'multi-platform build is missing'
[[ "$(grep -Fc 'platforms: linux/amd64,linux/arm64' "${WORKFLOW}")" == 2 ]] ||
  fail 'both images must be built for amd64 and arm64'
require_text 'type=raw,value=stable' 'stable tag is missing'
require_text 'type=raw,value=sha-' 'commit traceability tag is missing'
if grep -Eq 'value=latest|(^|[[:space:]])latest([[:space:]]|$)' "${WORKFLOW}"; then
  fail 'latest tag must not be published by the stable workflow'
fi
[[ "$(grep -Fc 'sbom: true' "${WORKFLOW}")" == 2 ]] || fail 'both image builds must create SBOMs'
[[ "$(grep -Fc 'provenance: mode=max' "${WORKFLOW}")" == 2 ]] || fail 'both image builds need max provenance'
[[ "$(grep -Fc 'push-to-registry: true' "${WORKFLOW}")" == 2 ]] || fail 'both image digests must be attested'
require_text 'export DOCKER_CONFIG="$(mktemp -d)"' 'credential-free public image verification is missing'
require_text 'gh attestation verify "oci://' 'image attestation verification is missing'
require_text 'scripts/release/prepare-release-assets.sh' 'release asset assembly is missing'
require_text 'subject-path: release-assets/*' 'downloadable asset attestation is missing'
require_text 'gh release upload "${tag}" release-assets/* --clobber' 'idempotent draft upload is missing'
require_text '--draft' 'draft release flag is missing'
if grep -Fq -- '--prerelease' "${WORKFLOW}"; then
  fail 'stable release must not be marked as a pre-release'
fi
require_text 'notes_file="docs/release/RELEASE_NOTES_${tag}.md"' 'versioned release notes validation is missing'

for permission in 'packages: write' 'id-token: write' 'attestations: write' 'contents: write'; do
  require_text "${permission}" "required scoped permission is missing: ${permission}"
done
[[ "$(grep -Ec '^permissions:$' "${WORKFLOW}")" == 1 ]] || fail 'global permissions block is malformed'
sed -n '/^permissions:$/,/^[^ ]/p' "${WORKFLOW}" | grep -Fq 'contents: read' ||
  fail 'global workflow permissions must remain read-only'

while IFS= read -r uses_line; do
  if [[ ! "${uses_line}" =~ uses:[[:space:]]+[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)+@[0-9a-f]{40}([[:space:]]+#[[:space:]].*)?$ ]]; then
    fail "external action is not pinned to a full commit SHA: ${uses_line}"
  fi
done < <(grep -E '^[[:space:]]*uses:' "${WORKFLOW}")

grep -Eq '^FROM eclipse-temurin:21-jre@sha256:[0-9a-f]{64}$' "${BACKEND_DOCKERFILE}" ||
  fail 'backend runtime base is not digest-pinned'
grep -Fxq 'USER 10001:10001' "${BACKEND_DOCKERFILE}" || fail 'backend image must run as a numeric non-root user'
for dockerfile in "${BACKEND_DOCKERFILE}" "${GATEWAY_DOCKERFILE}"; do
  for label in source revision version created licenses; do
    grep -Fq "org.opencontainers.image.${label}" "${dockerfile}" ||
      fail "${dockerfile}: OCI ${label} label is missing"
  done
done
grep -Fq "'**/delivery/**'" "${PLAYWRIGHT_CONFIG}" ||
  fail 'functional Playwright matrix must exclude the production delivery suite'

echo 'release-workflow-contract: OK'
