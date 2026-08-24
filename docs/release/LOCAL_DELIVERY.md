# Локальная поставка Configurator

Этот документ описывает эксплуатационный контракт пользовательских архивов и release automation 9.29–9.30. Tag
workflow публикует public multi-platform app/gateway images, формирует Windows/macOS assets и создаёт draft
pre-release. До явной публикации draft владельцем локально собранные архивы не считаются готовым пользовательским
релизом.

## Скачивание и запуск

1. Откройте <https://github.com/eltgm/configurator-root/releases>.
2. Скачайте `configurator-windows-vX.Y.Z.zip` или `configurator-macos-vX.Y.Z.tar.gz` и полностью распакуйте архив.
3. Запустите `Start.cmd` либо `Start.command`. На macOS при первом предупреждении используйте правый клик → «Открыть».
4. Дождитесь открытия <http://127.0.0.1:8080>.

Registry login не требуется: `configurator-app` и `configurator-web` публикуются в GHCR как public manifests для
`linux/amd64` и `linux/arm64`. Пакет использует mutable `preview` channel для Update, а `IMAGE_DIGESTS` в GitHub Release
фиксирует immutable digest конкретной версии.

## Поддерживаемое окружение

- Windows 10/11 x86-64 с Docker Desktop в режиме Linux containers;
- macOS Intel или Apple Silicon с поддерживаемой версией Docker Desktop;
- доступ в интернет для первого Start и каждого Update;
- свободный локальный порт `127.0.0.1:8080`.

Пакет не устанавливает Docker Desktop и не принимает его лицензию. JDK, Gradle, Node.js, npm и Git не нужны.

## Операции

| Команда   | Поведение                                                                                                    |
| --------- | ------------------------------------------------------------------------------------------------------------ |
| `Start`   | Проверяет Docker/Compose и порт, запускает stack, ждёт UI и API, открывает браузер.                          |
| `Stop`    | Останавливает project containers, сохраняя volumes, images и backups.                                        |
| `Backup`  | Останавливает запись, сохраняет PostgreSQL и MinIO, проверяет снимок и возвращает прежнее состояние.         |
| `Restore` | Проверяет выбранный backup, запрашивает подтверждение, создаёт страховочный backup и заменяет оба хранилища. |
| `Update`  | Создаёт обязательный backup, загружает app/gateway канала `preview` и проверяет readiness.                   |

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

## Проверка скачанных файлов

`SHA256SUMS` проверяет целостность JAR, OpenAPI, обоих пользовательских архивов и `IMAGE_DIGESTS`. Linux/macOS:

```bash
shasum -a 256 -c SHA256SUMS
```

Windows PowerShell для отдельного файла:

```powershell
Get-FileHash -Algorithm SHA256 .\configurator-windows-v0.1.0.zip
```

Технический пользователь с GitHub CLI может дополнительно проверить подписанное provenance:

```bash
gh attestation verify configurator-windows-v0.1.0.zip -R eltgm/configurator-root
gh attestation verify oci://ghcr.io/eltgm/configurator-app:0.1.0 -R eltgm/configurator-root
```

Checksum обнаруживает изменение байтов, а attestation связывает artifact с GitHub workflow/repository. Они не
гарантируют отсутствие уязвимостей и не заменяют security review.

## Проверка и сборка для разработчика

```bash
delivery/tests/package-contract.sh
delivery/tests/macos-scripts-test.sh
delivery/tests/archive-contract.sh
delivery/tests/release-assets-contract.sh
delivery/tests/release-workflow-contract.sh
delivery/tests/docker-lifecycle-contract.sh
scripts/release/build-delivery-packages.sh 0.1.0
```

Windows PowerShell 5.1 contract выполняется в Windows CI. Реальный lifecycle-тест использует изолированный Compose
project и локальный OCI registry, проверяет Start/Stop, оба хранилища, Update success и строгий Update failure.
Фактическая GHCR visibility, anonymous multi-platform pull, OIDC attestations и draft assets проверяются только trusted
tag workflow после merge в `master`; чистые Windows/macOS машины остаются обязательной ручной release-проверкой.

При первом image push GitHub может создать packages как private. Тогда anonymous gate ожидаемо остановит workflow:
владелец переводит `configurator-app` и `configurator-web` в public, связывает packages с repository и запускает тот же
workflow повторно. Персональный токен и автоматическое расширение visibility для этого не требуются.
