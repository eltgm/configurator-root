#!/bin/bash

set -u
umask 077

EXIT_USAGE=2
EXIT_PREREQUISITE=10
EXIT_DOCKER=20
EXIT_CONFLICT=30
EXIT_BACKUP=40
EXIT_RESTORE=50
EXIT_UPDATE=60
EXIT_READINESS=70
EXIT_LOCKED=80

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PACKAGE_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$PACKAGE_ROOT/compose.yaml"
COMPOSE_OVERRIDE_FILE="$PACKAGE_ROOT/compose.macos.yaml"
ENV_FILE="$PACKAGE_ROOT/configurator.env"
BACKUPS_DIR="$PACKAGE_ROOT/backups"
LOGS_DIR="$PACKAGE_ROOT/logs"
LOCK_DIR="$PACKAGE_ROOT/.configurator-operation.lock"

OPERATION=${1:-}
if [ -n "$OPERATION" ]; then
  shift
fi

NON_INTERACTIVE=0
NO_OPEN=0
ASSUME_YES=0
BACKUP_ARGUMENT=""
LOCK_ACQUIRED=0
LOG_FILE=""
LAST_BACKUP_DIR=""
MAINTENANCE_ARCHIVE=""
MAINTENANCE_VOLUME_ACTIVE=0
PARTIAL_BACKUP_DIR=""
DOCKER_WAIT_SECONDS=${CONFIGURATOR_DOCKER_WAIT_SECONDS:-180}
READINESS_WAIT_SECONDS=${CONFIGURATOR_READINESS_WAIT_SECONDS:-180}
MAINTENANCE_USER="$(id -u):$(id -g)"

usage() {
  echo "Usage: configurator.sh start|stop|update|backup|restore [--non-interactive] [--no-open] [--yes] [--backup PATH]" >&2
  exit "$EXIT_USAGE"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --non-interactive)
      NON_INTERACTIVE=1
      ;;
    --no-open)
      NO_OPEN=1
      ;;
    --yes)
      ASSUME_YES=1
      ;;
    --backup)
      shift
      [ "$#" -gt 0 ] || usage
      BACKUP_ARGUMENT=$1
      ;;
    *)
      usage
      ;;
  esac
  shift
done

case "$OPERATION" in
  start | stop | update | backup | restore) ;;
  *) usage ;;
esac

timestamp_utc() {
  date -u +%Y%m%d-%H%M%S
}

initialize_log() {
  mkdir -p "$LOGS_DIR"
  LOG_FILE="$LOGS_DIR/$OPERATION-$(timestamp_utc).log"
  : >"$LOG_FILE"
}

log() {
  message=$1
  printf '%s\n' "$message"
  if [ -n "$LOG_FILE" ]; then
    printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$message" >>"$LOG_FILE"
  fi
}

fail() {
  code=$1
  shift
  log "ОШИБКА: $*"
  if [ -n "$LOG_FILE" ]; then
    log "Диагностика: $LOG_FILE"
  fi
  exit "$code"
}

run_logged() {
  "$@" >>"$LOG_FILE" 2>&1
}

compose() {
  CONFIGURATOR_MAINTENANCE_USER="$MAINTENANCE_USER" \
    docker compose --project-directory "$PACKAGE_ROOT" --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" -f "$COMPOSE_OVERRIDE_FILE" "$@"
}

release_lock() {
  if [ "$LOCK_ACQUIRED" -eq 1 ]; then
    rmdir "$LOCK_DIR" 2>/dev/null || true
    LOCK_ACQUIRED=0
  fi
}

cleanup_maintenance_archive() {
  if [ -n "$MAINTENANCE_ARCHIVE" ] && [ -f "$MAINTENANCE_ARCHIVE" ] && [ ! -L "$MAINTENANCE_ARCHIVE" ]; then
    rm -f -- "$MAINTENANCE_ARCHIVE"
  fi
  MAINTENANCE_ARCHIVE=""
}

cleanup_maintenance_volume() {
  if [ "$MAINTENANCE_VOLUME_ACTIVE" -eq 1 ]; then
    compose run --rm --no-deps --user 0:0 postgres-maintenance \
      sh -c 'find /backup -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +' >/dev/null 2>&1 || true
    MAINTENANCE_VOLUME_ACTIVE=0
  fi
}

cleanup_partial_backup() {
  if [ -n "$PARTIAL_BACKUP_DIR" ] && [ -d "$PARTIAL_BACKUP_DIR" ] && [ ! -L "$PARTIAL_BACKUP_DIR" ]; then
    rm -rf -- "$PARTIAL_BACKUP_DIR"
  fi
  PARTIAL_BACKUP_DIR=""
}

cleanup() {
  cleanup_maintenance_archive
  cleanup_maintenance_volume
  cleanup_partial_backup
  release_lock
}
trap cleanup EXIT HUP INT TERM

acquire_lock() {
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    fail "$EXIT_LOCKED" "Другая операция Configurator уже выполняется. Дождитесь её завершения."
  fi
  LOCK_ACQUIRED=1
}

validate_package() {
  [ -f "$COMPOSE_FILE" ] || fail "$EXIT_PREREQUISITE" "Не найден compose.yaml. Распакуйте архив полностью."
  [ -f "$COMPOSE_OVERRIDE_FILE" ] || fail "$EXIT_PREREQUISITE" "Не найден compose.macos.yaml. Распакуйте архив полностью."
  [ -f "$ENV_FILE" ] || fail "$EXIT_PREREQUISITE" "Не найден configurator.env. Распакуйте архив полностью."
  mkdir -p "$BACKUPS_DIR" "$LOGS_DIR"
}

validate_platform() {
  os_name=$(uname -s 2>/dev/null || echo unknown)
  architecture=$(uname -m 2>/dev/null || echo unknown)
  case "$os_name" in
    Darwin | Linux) ;;
    *) fail "$EXIT_PREREQUISITE" "Неподдерживаемая операционная система: $os_name." ;;
  esac
  case "$architecture" in
    x86_64 | amd64 | arm64 | aarch64) ;;
    *) fail "$EXIT_PREREQUISITE" "Неподдерживаемая архитектура: $architecture." ;;
  esac
}

docker_daemon_ready() {
  docker info >/dev/null 2>&1
}

try_start_docker_desktop() {
  if [ "$(uname -s 2>/dev/null || true)" = "Darwin" ] && command -v open >/dev/null 2>&1; then
    log "Docker Desktop не запущен. Пытаюсь открыть Docker.app…"
    open -gja Docker >>"$LOG_FILE" 2>&1 || true
  fi
}

wait_for_docker() {
  elapsed=0
  while [ "$elapsed" -lt "$DOCKER_WAIT_SECONDS" ]; do
    if docker_daemon_ready; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  return 1
}

validate_docker() {
  command -v docker >/dev/null 2>&1 || fail "$EXIT_DOCKER" "Docker CLI не найден. Установите Docker Desktop."
  docker compose version >/dev/null 2>&1 || fail "$EXIT_DOCKER" "Docker Compose v2 недоступен. Обновите Docker Desktop."
  if ! docker_daemon_ready; then
    try_start_docker_desktop
    wait_for_docker || fail "$EXIT_DOCKER" "Docker Desktop не готов. Запустите его, примите лицензию и повторите."
  fi
}

validate_compose() {
  run_logged compose config --quiet || fail "$EXIT_CONFLICT" "Некорректная конфигурация пакета."
}

gateway_is_running() {
  [ -n "$(compose ps --status running --quiet gateway 2>/dev/null)" ]
}

validate_port() {
  if gateway_is_running; then
    return 0
  fi
  if command -v lsof >/dev/null 2>&1 && lsof -n -P -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    fail "$EXIT_CONFLICT" "Порт 8080 занят другим приложением. Освободите порт и повторите Start."
  fi
}

http_ok() {
  url=$1
  curl --fail --silent --show-error --max-time 3 "$url" >/dev/null 2>&1
}

wait_for_url() {
  url=$1
  elapsed=0
  while [ "$elapsed" -lt "$READINESS_WAIT_SECONDS" ]; do
    if http_ok "$url"; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  return 1
}

wait_for_application() {
  wait_for_url "http://127.0.0.1:8080/healthz" || return 1
  wait_for_url "http://127.0.0.1:8080/api/v3/api-docs" || return 1
}

write_safe_diagnostics() {
  compose ps >>"$LOG_FILE" 2>&1 || true
  compose logs --no-color --tail 80 app gateway >>"$LOG_FILE" 2>&1 || true
}

open_browser() {
  [ "$NO_OPEN" -eq 0 ] || return 0
  case "$(uname -s 2>/dev/null || true)" in
    Darwin)
      open "http://127.0.0.1:8080" >>"$LOG_FILE" 2>&1 || true
      ;;
    Linux)
      if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "http://127.0.0.1:8080" >>"$LOG_FILE" 2>&1 || true
      fi
      ;;
  esac
}

service_running() {
  service_name=$1
  [ -n "$(compose ps --status running --quiet "$service_name" 2>/dev/null)" ]
}

read_env_value() {
  key=$1
  value=$(sed -n "s/^${key}=//p" "$ENV_FILE" | sed -n '1p')
  [ -n "$value" ] || fail "$EXIT_PREREQUISITE" "В configurator.env отсутствует $key."
  printf '%s' "$value"
}

sha256_file() {
  file=$1
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  else
    return 1
  fi
}

generate_checksums() {
  directory=$1
  checksum_file="$directory/SHA256SUMS"
  : >"$checksum_file"
  find "$directory" -type f ! -name SHA256SUMS -print | LC_ALL=C sort | while IFS= read -r file; do
    relative=${file#"$directory"/}
    digest=$(sha256_file "$file") || exit 1
    printf '%s  %s\n' "$digest" "$relative"
  done >"$checksum_file"
}

verify_checksums() {
  directory=$1
  checksum_file="$directory/SHA256SUMS"
  [ -s "$checksum_file" ] || return 1
  while IFS= read -r line; do
    expected=${line%%  *}
    relative=${line#*  }
    [ "$relative" != "$line" ] || return 1
    case "$relative" in
      /* | ../* | */../*) return 1 ;;
    esac
    file="$directory/$relative"
    [ -f "$file" ] || return 1
    actual=$(sha256_file "$file") || return 1
    [ "$actual" = "$expected" ] || return 1
  done <"$checksum_file"
}

resolved_image_id() {
  service_name=$1
  container_id=$(compose ps --all --quiet "$service_name" 2>/dev/null || true)
  if [ -n "$container_id" ]; then
    docker inspect --format '{{.Image}}' "$container_id" 2>/dev/null || echo unknown
  else
    echo unknown
  fi
}

reset_maintenance_volume() {
  cleanup_maintenance_volume
  if ! run_logged compose run --rm --no-deps --user 0:0 postgres-maintenance \
    sh -c 'find /backup -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +; chmod 0777 /backup'; then
    return 1
  fi
  MAINTENANCE_VOLUME_ACTIVE=1
}

create_maintenance_archive() {
  cleanup_maintenance_archive
  archive_root=${TMPDIR:-/tmp}
  [ -d "$archive_root" ] || return 1
  MAINTENANCE_ARCHIVE=$(mktemp "$archive_root/configurator-maintenance.XXXXXX.tar") || return 1
  [ -f "$MAINTENANCE_ARCHIVE" ] && [ ! -L "$MAINTENANCE_ARCHIVE" ] || return 1
}

export_maintenance_volume() {
  destination=$1
  create_maintenance_archive || return 1
  if ! compose run --rm --no-deps postgres-maintenance tar -C /backup -cf - . \
    >"$MAINTENANCE_ARCHIVE" 2>>"$LOG_FILE"; then
    return 1
  fi
  COPYFILE_DISABLE=1 tar -C "$destination" -xf "$MAINTENANCE_ARCHIVE" >>"$LOG_FILE" 2>&1 || return 1
  cleanup_maintenance_archive
}

import_maintenance_volume() {
  source_directory=$1
  create_maintenance_archive || return 1
  COPYFILE_DISABLE=1 tar -C "$source_directory" -cf "$MAINTENANCE_ARCHIVE" . >>"$LOG_FILE" 2>&1 || return 1
  reset_maintenance_volume || return 1
  if ! compose run --rm --no-deps --user 0:0 postgres-maintenance tar -C /backup -xf - \
    <"$MAINTENANCE_ARCHIVE" >>"$LOG_FILE" 2>&1; then
    return 1
  fi
  cleanup_maintenance_archive
}

secure_maintenance_directory() {
  directory=$1
  chmod 700 "$directory" 2>/dev/null || true
  if [ -d "$directory/minio" ]; then
    chmod 700 "$directory/minio" 2>/dev/null || true
  fi
}

restore_service_state() {
  app_was_running=$1
  gateway_was_running=$2
  postgres_was_running=$3
  minio_was_running=$4

  if [ "$app_was_running" -eq 1 ]; then
    run_logged compose up -d app || return 1
  else
    run_logged compose stop app || true
  fi
  if [ "$gateway_was_running" -eq 1 ]; then
    run_logged compose up -d gateway || return 1
  else
    run_logged compose stop gateway || true
  fi
  if [ "$minio_was_running" -eq 0 ] && [ "$app_was_running" -eq 0 ] && [ "$gateway_was_running" -eq 0 ]; then
    run_logged compose stop minio || true
  fi
  if [ "$postgres_was_running" -eq 0 ] && [ "$app_was_running" -eq 0 ] && [ "$gateway_was_running" -eq 0 ]; then
    run_logged compose stop postgres || true
  fi
}

create_backup() {
  prefix=${1:-backup}
  app_was_running=0
  gateway_was_running=0
  postgres_was_running=0
  minio_was_running=0
  service_running app && app_was_running=1
  service_running gateway && gateway_was_running=1
  service_running postgres && postgres_was_running=1
  service_running minio && minio_was_running=1

  backup_name="$(timestamp_utc)"
  if [ "$prefix" != "backup" ]; then
    backup_name="$prefix-$backup_name"
  fi
  final_dir="$BACKUPS_DIR/$backup_name"
  suffix=1
  while [ -e "$final_dir" ] || [ -e "$final_dir.partial" ]; do
    final_dir="$BACKUPS_DIR/$backup_name-$suffix"
    suffix=$((suffix + 1))
  done
  partial_dir="$final_dir.partial"
  mkdir -p "$partial_dir" || return 1
  PARTIAL_BACKUP_DIR=$partial_dir
  if ! reset_maintenance_volume; then
    cleanup_partial_backup
    return 1
  fi

  log "Подготавливаю PostgreSQL и MinIO для backup…"
  if ! run_logged compose up -d --wait --wait-timeout "$READINESS_WAIT_SECONDS" postgres minio; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi
  run_logged compose stop gateway app || true

  db_name=$(read_env_value CONFIGURATOR_DB_NAME)
  bucket=$(read_env_value CONFIGURATOR_MINIO_BUCKET)
  package_version=$(read_env_value CONFIGURATOR_PACKAGE_VERSION)
  channel=$(read_env_value CONFIGURATOR_CHANNEL)

  log "Сохраняю базу данных…"
  if ! run_logged compose run --rm --no-deps postgres-maintenance \
    pg_dump --format=custom --no-owner --no-privileges --file=/backup/database.dump "$db_name"; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi

  if ! run_logged compose run --rm --no-deps postgres-maintenance \
    pg_restore --list /backup/database.dump; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi

  log "Сохраняю изображения…"
  if ! run_logged compose run --rm --no-deps minio-maintenance \
    mb --ignore-existing "configurator/$bucket"; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi
  if ! run_logged compose run --rm --no-deps minio-maintenance \
    mirror --overwrite "configurator/$bucket" /backup/minio; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi

  if ! export_maintenance_volume "$partial_dir"; then
    cleanup_maintenance_volume
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi
  cleanup_maintenance_volume
  secure_maintenance_directory "$partial_dir"

  cat >"$partial_dir/manifest.properties" <<EOF
formatVersion=1
createdAt=$(date -u +%Y-%m-%dT%H:%M:%SZ)
packageVersion=$package_version
channel=$channel
composeProject=configurator
appImage=$(resolved_image_id app)
gatewayImage=$(resolved_image_id gateway)
databaseArtifact=database.dump
minioArtifact=minio
EOF

  if ! generate_checksums "$partial_dir" || ! verify_checksums "$partial_dir"; then
    cleanup_partial_backup
    restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running" || true
    return 1
  fi
  if ! restore_service_state "$app_was_running" "$gateway_was_running" "$postgres_was_running" "$minio_was_running"; then
    cleanup_partial_backup
    return 1
  fi
  if [ "$app_was_running" -eq 1 ] && [ "$gateway_was_running" -eq 1 ] && ! wait_for_application; then
    write_safe_diagnostics
    cleanup_partial_backup
    return 1
  fi
  mv "$partial_dir" "$final_dir"
  PARTIAL_BACKUP_DIR=""
  LAST_BACKUP_DIR=$final_dir
  log "Backup создан: $final_dir"
  return 0
}

validate_backup() {
  directory=$1
  [ -d "$directory" ] || return 1
  [ ! -L "$directory" ] || return 1
  case "$(basename "$directory")" in
    *.partial) return 1 ;;
  esac
  [ -f "$directory/database.dump" ] || return 1
  [ -d "$directory/minio" ] || return 1
  [ -f "$directory/manifest.properties" ] || return 1
  [ -f "$directory/SHA256SUMS" ] || return 1
  grep -Fxq 'formatVersion=1' "$directory/manifest.properties" || return 1
  verify_checksums "$directory"
}

select_backup() {
  if [ -n "$BACKUP_ARGUMENT" ]; then
    case "$BACKUP_ARGUMENT" in
      /*) selected=$BACKUP_ARGUMENT ;;
      *) selected="$PACKAGE_ROOT/$BACKUP_ARGUMENT" ;;
    esac
    [ -d "$selected" ] || return 1
    SELECTED_BACKUP=$(CDPATH= cd -- "$selected" && pwd -P)
    return 0
  fi

  backups=""
  for candidate in "$BACKUPS_DIR"/*; do
    [ -d "$candidate" ] || continue
    case "$(basename "$candidate")" in
      *.partial) continue ;;
    esac
    backups="$candidate
$backups"
  done
  [ -n "$backups" ] || return 1

  if [ "$NON_INTERACTIVE" -eq 1 ]; then
    SELECTED_BACKUP=$(printf '%s' "$backups" | sed -n '1p')
    return 0
  fi

  log "Доступные backups:"
  index=1
  printf '%s' "$backups" | while IFS= read -r candidate; do
    [ -n "$candidate" ] || continue
    printf '  %s. %s\n' "$index" "$(basename "$candidate")"
    index=$((index + 1))
  done
  printf 'Введите номер backup: '
  IFS= read -r selected_index
  case "$selected_index" in
    '' | *[!0-9]*) return 1 ;;
  esac
  SELECTED_BACKUP=$(printf '%s' "$backups" | sed -n "${selected_index}p")
  [ -n "$SELECTED_BACKUP" ]
}

confirm_restore() {
  [ "$ASSUME_YES" -eq 1 ] && return 0
  [ "$NON_INTERACTIVE" -eq 0 ] || return 1
  log "Restore заменит текущую базу данных и изображения. Перед этим будет создан страховочный backup."
  printf 'Для продолжения введите RESTORE: '
  IFS= read -r confirmation
  [ "$confirmation" = "RESTORE" ]
}

perform_restore() {
  selected_backup=$1
  db_name=$(read_env_value CONFIGURATOR_DB_NAME)
  bucket=$(read_env_value CONFIGURATOR_MINIO_BUCKET)

  log "Создаю страховочный backup перед Restore…"
  create_backup pre-restore || return 1
  safety_backup=$LAST_BACKUP_DIR

  import_maintenance_volume "$selected_backup" || return 1

  if ! run_logged compose up -d --wait --wait-timeout "$READINESS_WAIT_SECONDS" postgres minio; then
    cleanup_maintenance_volume
    return 1
  fi
  run_logged compose stop gateway app || true

  log "Восстанавливаю базу данных…"
  if ! run_logged compose run --rm --no-deps postgres-maintenance \
    dropdb --maintenance-db=postgres --if-exists --force "$db_name"; then
    cleanup_maintenance_volume
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi
  if ! run_logged compose run --rm --no-deps postgres-maintenance \
    createdb --maintenance-db=postgres --template=template0 "$db_name"; then
    cleanup_maintenance_volume
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi
  if ! run_logged compose run --rm --no-deps postgres-maintenance \
    pg_restore --exit-on-error --no-owner --no-privileges --dbname="$db_name" /backup/database.dump; then
    cleanup_maintenance_volume
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi

  log "Восстанавливаю изображения…"
  if ! run_logged compose run --rm --no-deps minio-maintenance \
    mb --ignore-existing "configurator/$bucket"; then
    cleanup_maintenance_volume
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi
  if ! run_logged compose run --rm --no-deps minio-maintenance \
    mirror --overwrite --remove /backup/minio "configurator/$bucket"; then
    cleanup_maintenance_volume
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi

  cleanup_maintenance_volume

  if ! run_logged compose up -d --remove-orphans; then
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi
  if ! wait_for_application; then
    write_safe_diagnostics
    run_logged compose stop gateway app || true
    LAST_BACKUP_DIR=$safety_backup
    return 1
  fi
  LAST_BACKUP_DIR=$safety_backup
  return 0
}

operation_start() {
  validate_port
  log "Запускаю Configurator…"
  run_logged compose up -d --remove-orphans || fail "$EXIT_CONFLICT" "Не удалось запустить контейнеры. Проверьте порт 8080 и Docker Desktop."
  if ! wait_for_application; then
    write_safe_diagnostics
    fail "$EXIT_READINESS" "Приложение не стало готово за отведённое время."
  fi
  log "Configurator готов: http://127.0.0.1:8080"
  open_browser
}

operation_stop() {
  log "Останавливаю Configurator без удаления данных…"
  run_logged compose stop gateway app minio postgres || fail "$EXIT_DOCKER" "Не удалось остановить контейнеры."
  log "Configurator остановлен. Данные и backups сохранены."
}

operation_backup() {
  log "Создаю полный backup…"
  create_backup backup || fail "$EXIT_BACKUP" "Backup не создан. Текущее состояние сервисов восстановлено."
}

operation_restore() {
  SELECTED_BACKUP=""
  select_backup || fail "$EXIT_RESTORE" "Не удалось выбрать backup."
  validate_backup "$SELECTED_BACKUP" || fail "$EXIT_RESTORE" "Backup повреждён, неполон или имеет неподдерживаемый формат."
  confirm_restore || fail "$EXIT_RESTORE" "Restore отменён: подтверждение не получено."
  log "Начинаю Restore из ${SELECTED_BACKUP}…"
  if ! perform_restore "$SELECTED_BACKUP"; then
    run_logged compose stop gateway app || true
    fail "$EXIT_RESTORE" "Restore завершился ошибкой. App/gateway остановлены. Страховочный backup: $LAST_BACKUP_DIR"
  fi
  log "Restore завершён. Страховочный backup: $LAST_BACKUP_DIR"
}

operation_update() {
  log "Перед Update создаю обязательный backup…"
  if ! create_backup pre-update; then
    fail "$EXIT_UPDATE" "Update отменён: обязательный backup не создан."
  fi
  update_backup=$LAST_BACKUP_DIR
  log "Загружаю новые stable-образы…"
  if ! run_logged compose pull app gateway; then
    run_logged compose stop gateway app || true
    fail "$EXIT_UPDATE" "Не удалось загрузить stable-образы. App/gateway остановлены. Backup: $update_backup"
  fi
  log "Запускаю обновлённый Configurator…"
  if ! run_logged compose up -d --remove-orphans || ! wait_for_application; then
    write_safe_diagnostics
    run_logged compose stop gateway app || true
    fail "$EXIT_UPDATE" "Update не прошёл readiness. App/gateway остановлены; автоматический rollback запрещён. Backup: $update_backup"
  fi
  log "Update завершён. Backup перед обновлением: $update_backup"
}

initialize_log
validate_package
validate_platform
acquire_lock
validate_docker
validate_compose

case "$OPERATION" in
  start) operation_start ;;
  stop) operation_stop ;;
  backup) operation_backup ;;
  restore) operation_restore ;;
  update) operation_update ;;
esac

exit 0
