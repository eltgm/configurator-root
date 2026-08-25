# Git runbook — Configurator v1.1.0

Команды выполняются из корня репозитория. Они намеренно добавляют release-файлы явным списком и не затрагивают
пользовательское изменение `configurator-integration-tests/src/test/resources/testcontainers.properties`.

## 1. Влить исправление visual baseline

Tag workflow на commit `6b032888` подтвердил 72 functional E2E и 36 accessibility checks, но обнаружил устаревший
`configurator-light.png`. Исправление подготовлено в `bugfix/CON1-134`; если в трекере назначен другой номер,
замените его в имени ветки и commit message.

```bash
git switch bugfix/CON1-134

git add \
  configurator-web/e2e/__screenshots__/linux-chromium/application.visual.spec.ts/configurator-light.png \
  docs/release/RELEASE_CHECKLIST.md docs/release/RELEASE_AUDIT_v1.1.0.md \
  docs/release/GIT_RELEASE_v1.1.0.md

git diff --cached --check
git diff --cached --stat
git commit -m "CON1-134 Updated configurator visual baseline"
git push -u origin bugfix/CON1-134
```

Откройте PR `bugfix/CON1-134` → `develop`, дождитесь CI и merge. Затем откройте и влейте release PR
`develop` → `master`. Так release metadata и visual fix попадут в обе постоянные ветки; direct push не используется.

## 2. Разобраться с уже существующим tag

Annotated tag `v1.1.0` указывает на commit `6b032888`. Workflow остановился в `Verify release candidate` на visual
gate, а downstream publish jobs не запускались. Перед удалением всё равно проверьте отсутствие draft Release и exact
packages `configurator-app:1.1.0`/`configurator-web:1.1.0`.

Read-only проверка remote tag:

```bash
git ls-remote --tags origin refs/tags/v1.1.0 refs/tags/v1.1.0^{}
```

После merge visual fix в `master`, если draft release отсутствует и exact image tags не созданы, владелец может
удалить failed tag перед повторным запуском. Это destructive действие и оно недопустимо без проверки перечисленных
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
