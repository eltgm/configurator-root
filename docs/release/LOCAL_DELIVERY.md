# Локальная поставка Configurator v1

## Поддерживаемый сценарий

Пользователь получает только готовый пакет Windows 10/11 x86-64 или macOS Intel/Apple Silicon и запускает его через
Docker Desktop. JDK, Gradle, Node.js, npm и Git не нужны. Приложение доступно только на
<http://127.0.0.1:8080>; серверная установка и публикация порта вне loopback не поддерживаются.

Пакеты используют public multi-platform images:

- `ghcr.io/eltgm/configurator-app:stable`;
- `ghcr.io/eltgm/configurator-web:stable`.

Exact version и digest фиксируются в `IMAGE_DIGESTS`. `stable` — единственный mutable update channel; `latest` не
публикуется.

## Установка пользователем

1. Установить и запустить актуальный Docker Desktop.
2. Скачать архив своей ОС, `SHA256SUMS` и при необходимости `IMAGE_DIGESTS` из GitHub Release `v1.1.0`.
3. Проверить checksum архива.
4. Полностью распаковать папку `Configurator` в каталог с правом записи. Не запускать файлы внутри ZIP.
5. Windows: двойной клик `Start.cmd`. macOS: двойной клик `Start.command`; при первом Gatekeeper prompt использовать
   правый клик → «Открыть».
6. Дождаться readiness и открытия <http://127.0.0.1:8080>.

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 .\configurator-windows-v1.1.0.zip
```

macOS:

```bash
shasum -a 256 configurator-macos-v1.1.0.tar.gz
```

## Команды пакета

| Команда   | Назначение |
| --------- | ---------- |
| `Start`   | Проверяет Docker, загружает images, запускает приложение и ждёт readiness. |
| `Stop`    | Останавливает контейнеры без удаления данных. |
| `Update`  | Создаёт обязательный backup, загружает `stable` images и проверяет readiness. |
| `Backup`  | Создаёт checksum-protected PostgreSQL/MinIO backup формата v1. |
| `Restore` | Проверяет backup, создаёт страховочную копию и восстанавливает состояние. |

После неуспешного Update или Restore app/gateway намеренно остаются остановленными. Исправьте причину, затем повторите
операцию или восстановите последний заведомо рабочий backup. Backups не зашифрованы.

## Чистая переустановка и удаление

1. При необходимости создать `Backup` и скопировать папку `backups` отдельно.
2. Выполнить `Stop`.
3. Для полного сброса данных открыть терминал в папке `Configurator` и выполнить:

```bash
docker compose --env-file configurator.env -f compose.yaml down --volumes --remove-orphans
```

4. Удалить распакованную папку. Для переустановки распаковать новый архив в пустой каталог и выполнить `Start`.

Сброс необратимо удаляет локальные volumes. Backup до `v1.0.0` может быть несовместим; в этом случае используйте
чистую установку.

## Проверка происхождения

```bash
gh attestation verify configurator-macos-v1.1.0.tar.gz -R eltgm/configurator-root
gh attestation verify oci://ghcr.io/eltgm/configurator-web:1.1.0 -R eltgm/configurator-root
```

## Доставка владельцем

1. Убедиться, что checklist и release audit актуальны, CI зелёный, GHCR packages public.
2. Влить release PR `develop` → `master`.
3. Создать annotated tag на commit из `master`: `scripts/release/start-release-tag.sh 1.1.0`.
4. Дождаться workflow `Prepare GitHub release`: он повторно запускает полный test matrix, публикует exact/sha/stable
   images, attestations и draft release assets.
5. Проверить anonymous pull и clean-machine установку на Windows и macOS, checksum, Start/Stop/Update/Backup/Restore.
6. Просмотреть release notes и вручную опубликовать draft.

Локальная сборка структуры пакетов для проверки:

```bash
scripts/release/build-delivery-packages.sh 1.1.0
```
