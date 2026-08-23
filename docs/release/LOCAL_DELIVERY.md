# Локальная поставка Configurator

Этот документ описывает эксплуатационный контракт пользовательских архивов 9.29. Фактическая публикация
multi-platform app/gateway images и GitHub Release assets выполняется в 9.30.

## Поддерживаемое окружение

- Windows 10/11 x86-64 с Docker Desktop в режиме Linux containers;
- macOS Intel или Apple Silicon с поддерживаемой версией Docker Desktop;
- доступ в интернет для первого Start и каждого Update;
- свободный локальный порт `127.0.0.1:8080`.

Пакет не устанавливает Docker Desktop и не принимает его лицензию. JDK, Gradle, Node.js, npm и Git не нужны.

## Операции

| Команда   | Поведение |
| --------- | --------- |
| `Start`   | Проверяет Docker/Compose и порт, запускает stack, ждёт UI и API, открывает браузер. |
| `Stop`    | Останавливает project containers, сохраняя volumes, images и backups. |
| `Backup`  | Останавливает запись, сохраняет PostgreSQL и MinIO, проверяет снимок и возвращает прежнее состояние. |
| `Restore` | Проверяет выбранный backup, запрашивает подтверждение, создаёт страховочный backup и заменяет оба хранилища. |
| `Update`  | Создаёт обязательный backup, загружает app/gateway канала `preview` и проверяет readiness. |

Все команды защищены общим lock. Логи создаются в `logs/`, backups — в `backups/`. Переименование и перенос
распакованной папки не меняют Compose project и не теряют именованные volumes.

## Backup и Restore

Формат v1 содержит:

```text
backups/YYYYMMDD-HHMMSS/
  database.dump
  minio/
  manifest.properties
  SHA256SUMS
```

Backup логический и переносимый: PostgreSQL хранится в custom dump, MinIO — как текущий набор объектов. Снимки не
шифруются; храните их в доверенном месте. Restore поддерживает только неизменённые backups, созданные Configurator.
Не используйте dump или архив из неизвестного источника.

Перед Restore создаётся `pre-restore-*`. Перед Update — `pre-update-*`. Автоматическое удаление снимков не
выполняется. После успешного Restore текущие preview-images могут применить Flyway migration вперёд; downgrade на
старые images не поддерживается.

## Ошибка Update или Restore

При ошибке app и gateway остаются остановленными. Это строгое поведение: автоматический rollback image запрещён,
поскольку новый backend мог уже изменить схему БД. PostgreSQL, MinIO, выбранный backup и страховочный backup
сохраняются.

1. Откройте последний файл в `logs/` и сохраните его для диагностики.
2. Не удаляйте папку `backups` и Docker volumes.
3. Для возврата данных запустите Restore и выберите указанный в сообщении pre-update/pre-restore backup.
4. Если Restore снова завершается ошибкой, оставьте приложение остановленным и передайте log без изменения backups.

## Типовые проблемы

- «Docker CLI не найден» — установите/обновите Docker Desktop и повторите Start.
- «Docker Desktop не готов» — откройте Docker Desktop, завершите первичную настройку и дождитесь статуса Running.
- «Порт 8080 занят» — остановите другое локальное приложение на 8080; wildcard/LAN bind не поддерживается.
- macOS блокирует `.command` — правый клик по файлу → «Открыть». Не отключайте Gatekeeper глобально.
- Операция уже выполняется — дождитесь её окончания; не удаляйте lock во время работающей команды.

Runtime-аутентификация отсутствует. Поставка local-only preview и не является production-ready.

## Проверка и сборка для разработчика

```bash
delivery/tests/package-contract.sh
delivery/tests/macos-scripts-test.sh
delivery/tests/archive-contract.sh
delivery/tests/docker-lifecycle-contract.sh
scripts/release/build-delivery-packages.sh 0.1.0
```

Windows PowerShell 5.1 contract выполняется в Windows CI. Реальный lifecycle-тест использует изолированный Compose
project и локальный OCI registry, проверяет Start/Stop, оба хранилища, Update success и строгий Update failure.
