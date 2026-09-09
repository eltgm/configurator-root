# Git runbook — Configurator v1.3.1

Команды выполняются из корня репозитория. Не используйте прямой push в `develop`/`master`, не перемещайте существующие
теги и не перезаписывайте immutable exact assets или images.

## 1. Отправить подготовительную ветку

```bash
git switch codex/release-1.3.1
git status --short
git diff --check
git push -u origin codex/release-1.3.1
```

Откройте PR `codex/release-1.3.1` → `develop`, дождитесь зелёного CI, review и merge. Затем откройте release PR
`develop` → `master` и снова дождитесь зелёного CI.

## 2. Создать release tag

После merge release PR:

```bash
git switch master
git fetch origin master --tags
git pull --ff-only origin master
git status --short
scripts/release/start-release-tag.sh 1.3.1
```

Скрипт требует чистое дерево, точное совпадение с `origin/master`, section 1.3.1 в CHANGELOG, versioned release notes,
OpenAPI и frontend package metadata. Он создаёт annotated tag `v1.3.1` и отправляет только tag.

## 3. Завершить draft release

Workflow `Prepare GitHub release` повторно запускает backend/frontend/delivery/external matrix, публикует public app и
gateway images для `linux/amd64` и `linux/arm64`, прикладывает `SHA256SUMS`/`IMAGE_DIGESTS` и создаёт draft. Перед
публикацией вручную проверьте anonymous pull, attestations и чистую установку Windows/macOS с Start, Stop, Update,
Backup и Restore.

Если проверка не пройдена, исправляйте новым commit и выпускайте новую версию/tag; `v1.3.1` не удаляйте и не двигайте.
