#!/bin/bash

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
TEMP_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/configurator-macos-test.XXXXXX")
PACKAGE_ROOT="$TEMP_ROOT/Папка с пробелами/Configurator"
FAKE_BIN="$TEMP_ROOT/fake-bin"
FAKE_DOCKER_LOG="$TEMP_ROOT/docker.log"

cleanup() {
  rm -rf "$TEMP_ROOT"
}
trap cleanup EXIT HUP INT TERM

fail() {
  echo "macos-scripts-test: $1" >&2
  exit 1
}

mkdir -p "$PACKAGE_ROOT/scripts" "$PACKAGE_ROOT/backups" "$PACKAGE_ROOT/logs" "$FAKE_BIN"
cp "$REPOSITORY_ROOT/delivery/common/compose.yaml" "$PACKAGE_ROOT/compose.yaml"
cp "$REPOSITORY_ROOT/delivery/common/configurator.env" "$PACKAGE_ROOT/configurator.env"
cp "$REPOSITORY_ROOT/delivery/macos/scripts/configurator.sh" "$PACKAGE_ROOT/scripts/configurator.sh"
chmod +x "$PACKAGE_ROOT/scripts/configurator.sh"

cat >"$FAKE_BIN/docker" <<'EOF'
#!/bin/bash
set -u
printf '%s\n' "$*" >>"$FAKE_DOCKER_LOG"
if [ "${1:-}" = "compose" ] && printf '%s\n' "$*" | grep -Fq -- '--project-directory'; then
  printf '%s\n' "${CONFIGURATOR_MAINTENANCE_USER:-missing}" >>"$FAKE_DOCKER_USER_LOG"
fi

if [ "${1:-}" = "info" ]; then
  [ "${FAKE_DAEMON_DOWN:-0}" -eq 0 ]
  exit $?
fi

if [ "${1:-}" = "inspect" ]; then
  echo "sha256:fake-image"
  exit 0
fi

if [ "${1:-}" = "compose" ] && [ "${2:-}" = "version" ]; then
  [ "${FAKE_COMPOSE_DOWN:-0}" -eq 0 ]
  exit $?
fi

args=" $* "
case "$args" in
  *" compose "*" ps "*" --quiet "*)
    service=${args##* --quiet }
    service=${service%% *}
    echo "container-$service"
    exit 0
    ;;
  *" compose "*" pull "*)
    [ "${FAKE_FAIL_PULL:-0}" -eq 0 ]
    exit $?
    ;;
  *" pg_dump "*)
    mkdir -p "$CONFIGURATOR_MAINTENANCE_DIR"
    printf 'fake database dump\n' >"$CONFIGURATOR_MAINTENANCE_DIR/database.dump"
    exit 0
    ;;
  *" mirror "*" /backup/minio "*)
    mkdir -p "$CONFIGURATOR_MAINTENANCE_DIR/minio"
    printf 'fake image bytes\n' >"$CONFIGURATOR_MAINTENANCE_DIR/minio/object.bin"
    exit 0
    ;;
esac

exit 0
EOF

cat >"$FAKE_BIN/curl" <<'EOF'
#!/bin/bash
[ "${FAKE_READINESS_DOWN:-0}" -eq 0 ]
EOF

cat >"$FAKE_BIN/lsof" <<'EOF'
#!/bin/bash
exit 1
EOF

cat >"$FAKE_BIN/open" <<'EOF'
#!/bin/bash
exit 0
EOF

chmod +x "$FAKE_BIN/docker" "$FAKE_BIN/curl" "$FAKE_BIN/lsof" "$FAKE_BIN/open"

export PATH="$FAKE_BIN:$PATH"
export FAKE_DOCKER_LOG
export FAKE_DOCKER_USER_LOG="$TEMP_ROOT/docker-user.log"
export CONFIGURATOR_DOCKER_WAIT_SECONDS=0
export CONFIGURATOR_READINESS_WAIT_SECONDS=2

run_operation() {
  : >"$FAKE_DOCKER_LOG"
  "$PACKAGE_ROOT/scripts/configurator.sh" "$@" --non-interactive --no-open
}

run_operation start || fail "start failed"
grep -Fq 'up -d --remove-orphans' "$FAKE_DOCKER_LOG" || fail "start did not run compose up"
if grep -Fxv "$(id -u):$(id -g)" "$FAKE_DOCKER_USER_LOG" | grep -q .; then
  fail "compose did not receive the host UID:GID"
fi

run_operation stop || fail "stop failed"
grep -Fq 'stop gateway app minio postgres' "$FAKE_DOCKER_LOG" || fail "stop command is incorrect"

run_operation backup || fail "backup failed"
backup_dir=$(find "$PACKAGE_ROOT/backups" -mindepth 1 -maxdepth 1 -type d ! -name '*.partial' | sed -n '1p')
[ -n "$backup_dir" ] || fail "backup directory was not created"
[ -f "$backup_dir/database.dump" ] || fail "database dump is missing"
[ -f "$backup_dir/minio/object.bin" ] || fail "MinIO object is missing"
[ -f "$backup_dir/manifest.properties" ] || fail "manifest is missing"
[ -f "$backup_dir/SHA256SUMS" ] || fail "checksums are missing"

run_operation restore --yes --backup "$backup_dir" || fail "restore failed"
grep -Fq 'pg_restore --exit-on-error' "$FAKE_DOCKER_LOG" || fail "restore did not invoke pg_restore"
grep -Fq 'mirror --overwrite --remove' "$FAKE_DOCKER_LOG" || fail "restore did not replace MinIO contents"

set +e
FAKE_FAIL_PULL=1 run_operation update
update_status=$?
set -e
[ "$update_status" -eq 60 ] || fail "failed update returned $update_status instead of 60"
grep -Fq 'pull app gateway' "$FAKE_DOCKER_LOG" || fail "update did not pull app/gateway"
grep -Fq 'stop gateway app' "$FAKE_DOCKER_LOG" || fail "failed update did not stop app/gateway"
find "$PACKAGE_ROOT/backups" -mindepth 1 -maxdepth 1 -type d -name 'pre-update-*' | grep -q . ||
  fail "failed update did not retain pre-update backup"

mkdir "$PACKAGE_ROOT/.configurator-operation.lock"
set +e
run_operation stop
lock_status=$?
set -e
[ "$lock_status" -eq 80 ] || fail "lock contention returned $lock_status instead of 80"
rmdir "$PACKAGE_ROOT/.configurator-operation.lock"

set +e
FAKE_COMPOSE_DOWN=1 run_operation start
compose_status=$?
set -e
[ "$compose_status" -eq 20 ] || fail "missing Compose returned $compose_status instead of 20"

if find "$PACKAGE_ROOT/backups" -type d -name '*.partial' | grep -q .; then
  fail "partial backup directory leaked"
fi
if grep -R -Fq 'configurator-local-v1' "$PACKAGE_ROOT/logs"; then
  fail "local credentials leaked into operation logs"
fi

echo "macos-scripts-test: OK"
