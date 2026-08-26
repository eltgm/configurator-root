# Git runbook — Configurator v1.1.3

Команды выполняются из корня репозитория. Выпущенные теги не удаляются и не перемещаются: patch release получает
новый annotated tag и новые immutable exact assets/images.

## 1. Влить bugfix

```bash
git switch bugfix/CON1-137
git status --short
git diff --check
git push -u origin bugfix/CON1-137
```

Открыть PR `bugfix/CON1-137` → `develop`, дождаться зелёного CI и merge. Затем открыть release PR
`develop` → `master`, повторно дождаться CI и merge. Direct push в постоянные ветки не использовать.

## 2. Создать release tag

```bash
git switch master
git fetch origin master --tags
git merge --ff-only origin/master
git status --short
scripts/release/start-release-tag.sh 1.1.3
```

Guarded script проверяет чистоту дерева, точное совпадение с `origin/master`, changelog, release notes, OpenAPI и
frontend version, создаёт annotated tag `v1.1.3` и отправляет только его.

## 3. Проверить и опубликовать draft

Дождаться полного workflow `Prepare GitHub release`. Проверить exact/sha/stable image tags, `IMAGE_DIGESTS`,
`SHA256SUMS`, attestations, anonymous pulls и Windows/macOS clean-machine smoke. На Windows отдельно проверить, что
Docker Compose progress не прерывает Start и Update при exit code `0`.

Draft публикуется владельцем вручную. Если после публикации exact assets/images требуется изменение кода, тег
`v1.1.3` не перемещать и артефакты не перезаписывать — готовить следующий patch release.
