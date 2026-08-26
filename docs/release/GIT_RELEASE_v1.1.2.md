# Git runbook — Configurator v1.1.2

Команды выполняются из корня репозитория. Выпущенные теги не удаляются и не перемещаются: patch release получает
новый annotated tag и новые immutable exact assets/images.

## 1. Влить bugfix

```bash
git switch bugfix/CON1-136
git status --short
git diff --check
git push -u origin bugfix/CON1-136
```

Открыть PR `bugfix/CON1-136` → `develop`, дождаться зелёного CI и merge. Затем открыть release PR
`develop` → `master`, повторно дождаться CI и merge. Direct push в постоянные ветки не использовать.

## 2. Создать release tag

```bash
git switch master
git fetch origin master --tags
git merge --ff-only origin/master
git status --short
scripts/release/start-release-tag.sh 1.1.2
```

Guarded script проверяет чистоту дерева, точное совпадение с `origin/master`, changelog, release notes, OpenAPI и
frontend version, создаёт annotated tag `v1.1.2` и отправляет только его.

## 3. Проверить и опубликовать draft

Дождаться полного workflow `Prepare GitHub release`. Проверить exact/sha/stable image tags, `IMAGE_DIGESTS`,
`SHA256SUMS`, attestations, anonymous pulls и Windows/macOS clean-machine smoke. Редактирование сохранённой
конфигурации проверить на сборке, где часть пар имеет статус `UNKNOWN`, но все компоненты связаны разрешёнными
отношениями.

Draft публикуется владельцем вручную. Если после публикации exact assets/images требуется изменение кода, тег
`v1.1.2` не перемещать и артефакты не перезаписывать — готовить следующий patch release.
