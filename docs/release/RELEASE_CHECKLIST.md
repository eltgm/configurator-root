# Release checklist — v1.1.3

Дата аудита: 2026-08-26. Цель: patch release trusted-local Windows/macOS продукта с исправлением обработки Docker
Compose progress в Windows PowerShell 5.1.

## Product и scope

- [x] Release scope ограничен исправлением CON1-137 и синхронизацией release metadata.
- [x] Успешный Docker Compose progress в stderr не прерывает Windows Start, Backup, Restore или Update.
- [x] Неуспешный Docker command по-прежнему определяется его exit code и приводит к прежним публичным exit codes.
- [x] Windows PowerShell 5.1, CRLF и UTF-8 BOM delivery contracts сохранены.
- [x] Backup format v1, stable update channel и strict Update/Restore failure contracts сохранены.
- [x] Поставка остаётся loopback local-only; LAN/public/server и multi-user deployment исключены.
- [x] Выпущенные теги и immutable exact assets/images не перемещаются и не перезаписываются.

## Source of truth и версии

- [x] OpenAPI, frontend package и release metadata синхронизированы с `1.1.3`.
- [x] OpenAPI endpoint/schema contract не изменён; изменён только `info.version`.
- [x] Schema/Flyway/jOOQ не изменены.
- [x] Gradle default обновлён до `1.1.3-SNAPSHOT`; release build использует `-PreleaseVersion=1.1.3`.
- [x] Architecture boundary `controller → facade → service → port → infrastructure` не затронут.

## Документация

- [x] README, CHANGELOG и local delivery guide соответствуют `v1.1.3`.
- [x] Release notes объясняют Windows PowerShell 5.1 исправление и безопасное обновление.
- [x] Release audit и Git runbook созданы отдельно для `v1.1.3`; документы предыдущих версий сохранены как история.

## Проверки release candidate

- [ ] `./gradlew --no-daemon clean build -PreleaseVersion=1.1.3 -PspotlessRatchetFrom=origin/develop`.
- [ ] `npm ci`, API drift, frontend check и coverage.
- [ ] Functional Playwright E2E, accessibility и pinned-container visual regression.
- [x] Все delivery/release/lifecycle contracts.
- [x] External integration contract.
- [ ] Native Windows PowerShell 5.1 test и clean-machine Windows smoke.
- [ ] Clean-machine macOS Intel/Apple Silicon smoke.

## GitHub и публикация владельцем

- [ ] PR `bugfix/CON1-137` → `develop` влит с зелёным CI.
- [ ] Release PR `develop` → `master` влит с зелёным CI.
- [ ] Annotated tag `v1.1.3` создан guarded-скриптом на commit, совпадающем с `origin/master`.
- [ ] Tag workflow завершён; exact/sha/stable images, checksums, attestations и anonymous pulls проверены.
- [ ] Draft release просмотрен, clean-machine smoke завершён и draft опубликован вручную.

Результаты и блокеры: [`RELEASE_AUDIT_v1.1.3.md`](RELEASE_AUDIT_v1.1.3.md). Git-команды:
[`GIT_RELEASE_v1.1.3.md`](GIT_RELEASE_v1.1.3.md).
