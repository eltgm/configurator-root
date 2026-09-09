# Контрольный список выпуска v1.3.1

Дата подготовки: 2026-09-09. Цель: выпуск локального продукта для одного доверенного пользователя на Windows и macOS
с изменениями CON1-146, CON1-147 и CON1-148.

## Возможности и границы

- [x] Сохранённые конфигурации поддерживают серверный поиск по имени и стабильную сортировку с сохранением границ
      области, владельца, пагинации и фильтра учёта остатков.
- [x] Каталог и конфигуратор получили быстрый просмотр, сохранение фильтров, корректный возврат и удобную мобильную
      раскладку; встроенная страница помощи описывает подготовку каталога и сборки.
- [x] Формы защищают несохранённые изменения, ошибки остаются в контексте операции, а интерактивные элементы и
      навигация получили проверяемую accessibility-поддержку.
- [x] Добавлено самостоятельное руководство Windows по виртуализации, WSL 2 и Docker Desktop.
- [x] Формат резервной копии v1, канал `stable`, работа только через локальный адрес и строгая остановка после ошибки
      `Update` или `Restore` сохранены.
- [x] Runtime-аутентификация не реализована; LAN/public/server deployment не входит в поддерживаемый контур.

## Источники истины и версии

- [x] Версия Gradle по умолчанию — `1.3.1-SNAPSHOT`; сборка по тегу использует `-PreleaseVersion=1.3.1`.
- [x] `configurator-web/package.json` и lockfile — `1.3.1`.
- [x] `info.version` в OpenAPI — `1.3.1`; generated frontend client получен из спецификации, generated backend code не
      редактировался вручную.
- [x] Схема БД, Flyway и generated jOOQ не менялись; jOOQ создаётся в Gradle lifecycle.
- [x] Архитектурная цепочка `controller → facade → service → port → infrastructure` сохранена.
- [x] Local и external integration используют единый контракт.

## Документация

- [x] README, пользовательское руководство, frontend README, требования, CHANGELOG, руководство по поставке и Windows
      setup guide синхронизированы с v1.3.1.
- [x] Заметки, аудит и Git runbook созданы для v1.3.1; исторические документы выпусков сохранены.

## Проверки кандидата на выпуск

- [x] `./gradlew --no-daemon build -PreleaseVersion=1.3.1 -PspotlessRatchetFrom=origin/develop`.
- [x] `npm ci`, `npm run check`, `npm run test:coverage` и `npm audit --audit-level=high`.
- [x] `npm run test:e2e`, `npm run test:accessibility` и pinned `npm run test:visual`.
- [x] Пять локальных контрактов package/macOS/archive/release-assets/release-workflow.
- [x] Полный Compose, `externalIntegrationTest`, `npm run test:delivery` и `docker-lifecycle-contract.sh`.
- [ ] Проверка в настоящем Windows PowerShell 5.1 и пробный запуск на чистых Windows и macOS.

## GitHub и публикация владельцем

- [ ] Ветка `codex/release-1.3.1` отправлена и PR в `develop` прошёл CI и review.
- [ ] Релизный PR `develop` → `master` прошёл CI и влит без прямого push.
- [ ] Аннотированный тег `v1.3.1` создан guarded-скриптом на точном `origin/master`.
- [ ] Tag workflow завершён; образы exact/SHA/stable, суммы, attestations и anonymous pulls проверены.
- [ ] Черновик выпуска просмотрен, clean-machine smoke завершён, выпуск опубликован вручную.

Неподтверждённые действия остаются незакрытыми. Фактические результаты и препятствия:
[`RELEASE_AUDIT_v1.3.1.md`](RELEASE_AUDIT_v1.3.1.md). Команды Git:
[`GIT_RELEASE_v1.3.1.md`](GIT_RELEASE_v1.3.1.md).
