# Контрольный список выпуска v1.3.0

Дата подготовки: 2026-09-08. Цель: выпуск локального продукта для одного доверенного пользователя на Windows и macOS
с изменениями CON1-144 и CON1-145.

## Возможности и границы

- [x] Компоненты имеют общее, занятое и доступное количество; занятость определяется только конфигурациями с явно
      включённым учётом остатков.
- [x] Сборка поддерживает количество экземпляров и несколько моделей одного типа, сохраняя assembly-aware проверку
      связности и блокирующих правил.
- [x] Создание и обновление конфигурации проверяет остатки атомарно, а удаление или выключение режима освобождает их.
- [x] Старые `componentIds`, draft v1 и существующие данные мигрируют с безопасными значениями по умолчанию.
- [x] Формат резервной копии v1, канал `stable`, работа только через локальный адрес и строгая остановка после ошибки
      `Update` или `Restore` сохранены.
- [x] Runtime-аутентификация не реализована; LAN/public/server deployment не входит в поддерживаемый контур.

## Источники истины и версии

- [x] Версия Gradle по умолчанию — `1.3.0-SNAPSHOT`; сборка по тегу использует `-PreleaseVersion=1.3.0`.
- [x] `configurator-web/package.json` и lockfile — `1.3.0`.
- [x] `info.version` в OpenAPI — `1.3.0`; generated frontend client получен из спецификации, generated backend code не
      редактировался вручную.
- [x] Добавлена только новая миграция V10; выпущенные миграции не изменялись, jOOQ создаётся в Gradle lifecycle.
- [x] Архитектурная цепочка `controller → facade → service → port → infrastructure` сохранена.
- [x] Local и external integration используют единый контракт.

## Документация

- [x] README, пользовательское руководство, frontend README, требования, CHANGELOG, политика поддержки, руководство
      по поставке и шаблон сообщения об ошибке синхронизированы с v1.3.0.
- [x] Заметки, аудит и Git runbook созданы для v1.3.0; исторические документы выпусков сохранены.

## Проверки кандидата на выпуск

- [x] `./gradlew --no-daemon clean build -PreleaseVersion=1.3.0 -PspotlessRatchetFrom=origin/develop`.
- [x] `npm ci`, `npm run check`, `npm run test:coverage` и `npm audit --audit-level=high`.
- [x] `npm run test:e2e`, `npm run test:accessibility` и pinned `npm run test:visual`.
- [x] Пять локальных контрактов package/macOS/archive/release-assets/release-workflow.
- [x] Полный Compose, `externalIntegrationTest`, `npm run test:delivery` и `docker-lifecycle-contract.sh`.
- [ ] Проверка в настоящем Windows PowerShell 5.1 и пробный запуск на чистых Windows и macOS.

## GitHub и публикация владельцем

- [ ] Ветка `codex/release-1.3.0` отправлена и PR в `develop` прошёл CI и review.
- [ ] Релизный PR `develop` → `master` прошёл CI и влит без прямого push.
- [ ] Аннотированный тег `v1.3.0` создан guarded-скриптом на точном `origin/master`.
- [ ] Tag workflow завершён; образы exact/SHA/stable, суммы, attestations и anonymous pulls проверены.
- [ ] Черновик выпуска просмотрен, clean-machine smoke завершён, выпуск опубликован вручную.

Неподтверждённые действия остаются незакрытыми. Фактические результаты и препятствия:
[`RELEASE_AUDIT_v1.3.0.md`](RELEASE_AUDIT_v1.3.0.md). Команды Git:
[`GIT_RELEASE_v1.3.0.md`](GIT_RELEASE_v1.3.0.md).
