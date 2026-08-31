# Git runbook — Configurator v1.2.0

Команды выполняются из корня репозитория. Не перемещайте существующие теги и не перезаписывайте immutable exact assets
или images.

## 1. Влить подготовительную ветку

```bash
git switch feature/CON1-143
git status --short
git diff --check
git push -u origin feature/CON1-143
```

Откройте PR `feature/CON1-143` → `develop`, дождитесь зелёного CI и merge. Затем откройте release PR
`develop` → `master` и снова дождитесь зелёного CI. Direct push в `develop` и `master` не использовать.

## 2. Создать release tag

После merge release PR:

```bash
git switch master
git fetch origin master --tags
git pull --ff-only origin master
git status --short
scripts/release/start-release-tag.sh 1.2.0
```

Скрипт требует чистое дерево, точное совпадение с `origin/master`, section `1.2.0` в CHANGELOG, versioned release notes,
OpenAPI и frontend package metadata. Он создаёт annotated tag `v1.2.0` и отправляет только tag.

## 3. Завершить draft release

Workflow `Prepare GitHub release` повторно запускает backend/frontend/delivery/external matrix, публикует public app и
gateway images для `linux/amd64` и `linux/arm64`, прикладывает `SHA256SUMS`/`IMAGE_DIGESTS` и создаёт draft. Перед
публикацией вручную проверьте anonymous pull, attestations и чистую установку Windows/macOS с Start, Stop, Update,
Backup и Restore.

Если проверка не пройдена, исправляйте новый commit и выпускайте новый version/tag; `v1.2.0` не удаляйте и не двигайте.
