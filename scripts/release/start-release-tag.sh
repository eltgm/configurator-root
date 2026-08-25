#!/usr/bin/env bash

set -euo pipefail

readonly VERSION="${1:-}"
readonly TAG="v${VERSION}"

if [[ ! "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Usage: $0 X.Y.Z" >&2
  exit 2
fi

for command in git grep; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command is missing: ${command}" >&2
    exit 3
  }
done

readonly REPOSITORY_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPOSITORY_ROOT}"

[[ "$(git branch --show-current)" == master ]] || {
  echo "Release tag must be created from master" >&2
  exit 4
}

[[ -z "$(git status --porcelain)" ]] || {
  echo "Working tree is not clean; commit or stash every local change before release" >&2
  git status --short >&2
  exit 4
}

git fetch origin master --tags

[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/master)" ]] || {
  echo "Local master must exactly match origin/master" >&2
  exit 4
}

if git rev-parse --verify --quiet "refs/tags/${TAG}" >/dev/null; then
  echo "Tag already exists: ${TAG}" >&2
  exit 4
fi

grep -Fq "## [${VERSION}]" CHANGELOG.md || {
  echo "CHANGELOG.md has no release section for ${VERSION}" >&2
  exit 4
}
[[ -s "docs/release/RELEASE_NOTES_${TAG}.md" ]] || {
  echo "Release notes are missing: docs/release/RELEASE_NOTES_${TAG}.md" >&2
  exit 4
}
grep -Fq "  version: ${VERSION}" specs/configurator-api.yaml || {
  echo "OpenAPI info.version does not match ${VERSION}" >&2
  exit 4
}
grep -Fq "\"version\": \"${VERSION}\"" configurator-web/package.json || {
  echo "Frontend package version does not match ${VERSION}" >&2
  exit 4
}

git tag -a "${TAG}" -m "Configurator ${TAG}"
git push origin "refs/tags/${TAG}"

echo "Release workflow started by ${TAG} at $(git rev-parse HEAD)"
