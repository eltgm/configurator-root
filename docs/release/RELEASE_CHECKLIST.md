# Release checklist — v1.2.0

Дата подготовки: 2026-08-31. Цель: minor release trusted-local Windows/macOS продукта с изменениями CON1-138…142.

## Product и scope

- [x] Системные имена атрибутов уникальны в пределах области, а V8 безопасно объединяет только совместимые прежние дубликаты.
- [x] Компоненты получили заглавное изображение и PNG-превью; удаление изображений переживает временную недоступность MinIO.
- [x] Редактор конфигураций сохраняет контекст при замене и остаётся доступным на узких экранах.
- [x] Область можно рекурсивно удалить только без сохранённых конфигураций; иначе API возвращает `409 DOMAIN_HAS_CONFIGURATIONS`.
- [x] Backup format v1, канал `stable`, loopback-only delivery и строгая остановка после failed Update/Restore сохранены.
- [x] Выпущенные теги, exact images и assets не перемещаются и не перезаписываются.

## Source of truth и версии

- [x] Gradle default — `1.2.0-SNAPSHOT`; tag build использует `-PreleaseVersion=1.2.0`.
- [x] `configurator-web/package.json` и lockfile — `1.2.0`.
- [x] OpenAPI `info.version` — `1.2.0`; generated backend/frontend files не редактировались вручную.
- [x] Schema/Flyway изменены новыми V8 и V9; jOOQ generation выполняется Gradle lifecycle.
- [x] Архитектурная цепочка `controller → facade → service → port → infrastructure` сохранена.

## Документация

- [x] README, CHANGELOG, local delivery guide и issue template синхронизированы с `v1.2.0`.
- [x] Release notes, audit и Git runbook созданы для `v1.2.0`; исторические документы сохранены.

## Проверки release candidate

- [x] `./gradlew --no-daemon clean build -PreleaseVersion=1.2.0 -PspotlessRatchetFrom=origin/develop`.
- [x] `./gradlew --no-daemon :configurator-integration-tests:externalIntegrationTest -PreleaseVersion=1.2.0` against full Compose.
- [x] `npm ci`, `npm run check` и `npm run test:coverage`.
- [x] `npm run test:e2e`, `npm run test:accessibility`, `npm run test:visual` и `npm run test:delivery`.
- [x] Шесть delivery/release/lifecycle contracts.
- [ ] Native Windows PowerShell 5.1 и clean-machine Windows/macOS smoke.

## GitHub и публикация владельцем

- [ ] PR `feature/CON1-143` → `develop` влит с зелёным CI.
- [ ] Release PR `develop` → `master` влит с зелёным CI.
- [ ] Annotated tag `v1.2.0` создан guarded-скриптом на commit, совпадающем с `origin/master`.
- [ ] Tag workflow завершён; exact/sha/stable images, checksums, attestations и anonymous pulls проверены.
- [ ] Draft release просмотрен, clean-machine smoke завершён и draft опубликован вручную.

Результаты и блокеры: [`RELEASE_AUDIT_v1.2.0.md`](RELEASE_AUDIT_v1.2.0.md). Git-команды:
[`GIT_RELEASE_v1.2.0.md`](GIT_RELEASE_v1.2.0.md).
