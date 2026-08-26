# Release checklist — v1.1.2

Дата аудита: 2026-08-26. Цель: patch release trusted-local Windows/macOS продукта с исправлением редактора
конфигураций.

## Product и scope

- [x] Release scope ограничен исправлением CON1-136 и синхронизацией release metadata.
- [x] Создание и редактирование используют одинаковую assembly-aware семантику совместимости.
- [x] `UNKNOWN` не блокирует связную сборку; `DENIED` и `DISCONNECTED` различаются.
- [x] Добавление, замена и удаление компонентов поддерживают восстановление корректного состава.
- [x] Backup format v1, stable update channel и strict Update/Restore failure contracts сохранены.
- [x] Поставка остаётся loopback local-only; LAN/public/server и multi-user deployment исключены.
- [x] Выпущенные теги и immutable exact assets/images не перемещаются и не перезаписываются.

## Source of truth и версии

- [x] OpenAPI, frontend package и release metadata синхронизированы с `1.1.2`.
- [x] OpenAPI assembly semantics уточнена без изменения endpoint/schema; frontend SDK regenerated.
- [x] Schema/Flyway/jOOQ не изменены.
- [x] Gradle default обновлён до `1.1.2-SNAPSHOT`; release build использует `-PreleaseVersion=1.1.2`.
- [x] Architecture boundary `controller → facade → service → port → infrastructure` не затронут.

## Документация

- [x] README, CHANGELOG и local delivery guide соответствуют `v1.1.2`.
- [x] Release notes объясняют assembly-aware исправление и безопасное обновление.
- [x] Release audit и Git runbook созданы отдельно для `v1.1.2`; документы предыдущих версий сохранены как история.

## Проверки release candidate

- [x] `./gradlew --no-daemon clean build -PreleaseVersion=1.1.2 -PspotlessRatchetFrom=origin/develop`.
- [x] `npm ci`, API drift, frontend check и coverage.
- [x] Functional Playwright E2E, accessibility и pinned-container visual regression.
- [x] Все delivery/release/lifecycle contracts.
- [x] External integration contract.
- [ ] Native Windows PowerShell 5.1 test и clean-machine Windows smoke.
- [ ] Clean-machine macOS Intel/Apple Silicon smoke.

## GitHub и публикация владельцем

- [ ] PR `bugfix/CON1-136` → `develop` влит с зелёным CI.
- [ ] Release PR `develop` → `master` влит с зелёным CI.
- [ ] Annotated tag `v1.1.2` создан guarded-скриптом на commit, совпадающем с `origin/master`.
- [ ] Tag workflow завершён; exact/sha/stable images, checksums, attestations и anonymous pulls проверены.
- [ ] Draft release просмотрен, clean-machine smoke завершён и draft опубликован вручную.

Результаты и блокеры: [`RELEASE_AUDIT_v1.1.2.md`](RELEASE_AUDIT_v1.1.2.md). Git-команды:
[`GIT_RELEASE_v1.1.2.md`](GIT_RELEASE_v1.1.2.md).
