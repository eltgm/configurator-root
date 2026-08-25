# Git runbook — Configurator v1.1.0

Команды выполняются из корня репозитория. Они намеренно добавляют release-файлы явным списком и не затрагивают
пользовательское изменение `configurator-integration-tests/src/test/resources/testcontainers.properties`.

## 1. Подготовить release commit

В примере CON1-134 — отдельная задача подготовки релиза. Если в трекере назначен другой номер, замените его в имени
ветки и commit message.

```bash
git switch -c feature/CON1-134 origin/develop

git add \
  build.gradle Dockerfile docker-compose.yml specs/configurator-api.yaml \
  configurator-web/package.json configurator-web/package-lock.json \
  CHANGELOG.md README.md SECURITY.md SUPPORT.md \
  docs/release/LOCAL_DELIVERY.md docs/release/RELEASE_CHECKLIST.md \
  docs/release/RELEASE_NOTES_v1.1.0.md docs/release/RELEASE_AUDIT_v1.1.0.md \
  docs/release/GIT_RELEASE_v1.1.0.md scripts/release/start-release-tag.sh

git diff --cached --check
git diff --cached --stat
git commit -m "CON1-134 Prepared repository for v1.1.0 release"
git push -u origin feature/CON1-134
```

Откройте PR `feature/CON1-134` → `develop`, дождитесь CI и merge. Затем откройте и влейте release PR
`develop` → `master`. Не переносите release commit прямым push или прямым commit в `master`.

## 2. Разобраться с уже существующим tag

Локально обнаружен annotated tag `v1.1.0` на commit `ce6c4a6`, где ещё нет release metadata. Сначала проверьте GitHub
Actions, Releases и packages `configurator-app:1.1.0`/`configurator-web:1.1.0`.

Read-only проверка remote tag:

```bash
git ls-remote --tags origin refs/tags/v1.1.0 refs/tags/v1.1.0^{}
```

Если workflow остановился до публикации, draft release отсутствует и exact image tags не созданы, владелец может
удалить прежний tag перед повторным запуском. Это destructive действие и оно недопустимо без проверки перечисленных
условий:

```bash
git push origin :refs/tags/v1.1.0
git tag -d v1.1.0
```

Если release или exact images уже опубликованы, tag не перемещать: выбрать новую SemVer и повторно актуализировать
release metadata.

## 3. Запустить проверенный tag workflow

После merge release PR и разрешения конфликта со старым tag:

```bash
git switch master
git fetch origin master --tags
git merge --ff-only origin/master
scripts/release/start-release-tag.sh 1.1.0
```

Скрипт проверит чистоту дерева, точное совпадение с `origin/master`, changelog, release notes и версии source metadata,
создаст annotated tag и отправит только его. Push запускает workflow `Prepare GitHub release`; публикация созданного
draft остаётся ручным действием владельца.
