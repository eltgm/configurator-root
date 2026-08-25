# Release checklist — v1.1.1

Дата аудита: 2026-08-25. Цель: patch release trusted-local Windows/macOS продукта с исправлением macOS delivery.

## Product и scope

- [x] Release scope ограничен исправлением CON1-135.
- [x] macOS maintenance больше не монтирует каталог пользовательского пакета в Docker.
- [x] Backup format v1, stable update channel и strict Update/Restore failure contracts сохранены.
- [x] Поставка остаётся loopback local-only; LAN/public/server и multi-user deployment исключены.
- [x] Тег и immutable exact assets/images `v1.1.0` не перемещаются и не перезаписываются.

## Source of truth и версии

- [x] OpenAPI, frontend package и release metadata синхронизированы с `1.1.1`.
- [x] OpenAPI contract и schema/Flyway/jOOQ не изменены, кроме `info.version`.
- [x] Gradle default обновлён до `1.1.1-SNAPSHOT`; release build использует `-PreleaseVersion=1.1.1`.
- [x] Новый macOS Compose override включается только в macOS package; Windows package не изменяет storage contract.
- [x] Architecture boundary `controller → facade → service → port → infrastructure` не затронут.

## Документация

- [x] README, CHANGELOG и local delivery guide соответствуют `v1.1.1`.
- [x] Release notes объясняют необходимость нового macOS-архива и recovery после failed `v1.1.0` Update.
- [x] Release audit и Git runbook созданы отдельно для `v1.1.1`; документы `v1.1.0` сохранены как история.

## Проверки release candidate

- [x] `./gradlew --no-daemon clean build -PreleaseVersion=1.1.1 -PspotlessRatchetFrom=origin/develop`.
- [x] `npm ci`, API drift, frontend check и coverage.
- [x] Functional Playwright E2E, accessibility и pinned-container visual regression.
- [x] Пять non-Docker-lifecycle delivery contracts.
- [x] Real Docker lifecycle: PostgreSQL/MinIO Backup и Restore, успешный и strict-failure Update.
- [x] External integration contract и production gateway delivery smoke.
- [ ] Native Windows PowerShell 5.1 test и clean-machine Windows smoke.
- [ ] Clean-machine macOS Intel/Apple Silicon smoke из каталога `Downloads` без Docker File Sharing.

## GitHub и публикация владельцем

- [ ] PR `bugfix/CON1-135` → `develop` влит с зелёным CI.
- [ ] Release PR `develop` → `master` влит с зелёным CI.
- [ ] Annotated tag `v1.1.1` создан guarded-скриптом на commit, совпадающем с `origin/master`.
- [ ] Tag workflow завершён; exact/sha/stable images, checksums, attestations и anonymous pulls проверены.
- [ ] Draft release просмотрен, clean-machine smoke завершён и draft опубликован вручную.

Результаты и блокеры: [`RELEASE_AUDIT_v1.1.1.md`](RELEASE_AUDIT_v1.1.1.md). Git-команды:
[`GIT_RELEASE_v1.1.1.md`](GIT_RELEASE_v1.1.1.md).
