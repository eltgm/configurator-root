# Release checklist — v1.1.0

Дата аудита: 2026-08-25. Цель: minor release trusted-local Windows/macOS продукта с Docker Desktop.

## Product и scope

- [x] Release scope ограничен задачами CON1-132 и CON1-133.
- [x] Каталог атрибутов предметной области переиспользуется между типами компонентов.
- [x] Совместимость сборки использует связность разрешённых отношений и блокирующие решения с объяснениями.
- [x] Поставка остаётся loopback local-only; LAN/public/server и multi-user deployment исключены.
- [x] Backup format v1, stable update channel и strict Update/Restore failure contracts не изменены.

## Source of truth и совместимость

- [x] OpenAPI version обновлена до `1.1.0`; новый candidates contract и attribute catalog API находятся в source spec.
- [x] Flyway `V7` переносит атрибуты в доменный каталог с сохранением ID и связей; jOOQ сгенерирован lifecycle-задачами.
- [x] Backend, frontend package, Docker/Compose JAR defaults и release metadata синхронизированы с `1.1.0`.
- [ ] Миграция `V7` проверена на копии непустой базы `v1.0.0` с пользовательскими данными.
- [x] Architecture boundary `controller → facade → service → port → infrastructure` сохранён.

## Документация

- [x] README, CHANGELOG, SECURITY, SUPPORT и local delivery guide соответствуют `v1.1.0`.
- [x] Release notes содержат два изменения из CON1-132 и CON1-133.
- [x] Release audit фиксирует фактические проверки и failed-tag recovery.
- [x] Git runbook и guarded tag script не включают unrelated `testcontainers.properties` в release commit.

## Проверки release candidate

- [x] `./gradlew --no-daemon clean build -PreleaseVersion=1.1.0 -PspotlessRatchetFrom=origin/develop`.
- [x] `npm ci`, `npm run check`, `npm run test:coverage`.
- [x] Full Playwright E2E, accessibility и pinned-container visual regression.
- [ ] External integration contract через production Compose и `npm run test:delivery`.
- [x] Пять delivery contracts без real Docker lifecycle.
- [ ] Real Docker lifecycle contract.
- [ ] Native Windows PowerShell 5.1 test и clean-machine Windows smoke.
- [ ] Clean-machine macOS Intel/Apple Silicon smoke.

## GitHub и публикация владельцем

- [ ] Visual-baseline fix влит через `develop`, затем release PR `develop` → `master` одобрен и CI зелёный.
- [ ] Failed tag `v1.1.0` проверен в GitHub; подтверждено отсутствие опубликованного release и immutable
      image tag `1.1.0` либо выбрана новая версия.
- [ ] Annotated tag `v1.1.0` создан на окончательном release commit из `master` guarded-скриптом.
- [ ] Tag workflow завершён; exact/sha/stable images, checksums, attestations и anonymous pulls проверены.
- [ ] Draft release просмотрен, clean-machine smoke завершён и draft опубликован вручную.

Результаты и блокеры: [`RELEASE_AUDIT_v1.1.0.md`](RELEASE_AUDIT_v1.1.0.md). Git-команды:
[`GIT_RELEASE_v1.1.0.md`](GIT_RELEASE_v1.1.0.md).
