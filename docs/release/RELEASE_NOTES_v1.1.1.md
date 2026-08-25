# Configurator v1.1.1

Patch release локального Configurator для Windows и macOS. Пользователю нужны Docker Desktop, интернет для первого
запуска и полностью распакованный архив; JDK, Node.js и Git не требуются.

## Исправление

- **Надёжное обновление на macOS.** Backup, Restore и обязательный backup перед Update теперь используют внутренний
  Docker volume и передают архив между контейнерами и host без bind mount. Папку пакета, включая `Downloads`, больше
  не требуется добавлять в Docker Desktop File Sharing.

## Обновление

Исправление находится в новом пользовательском архиве, поэтому установленный пакет `v1.1.0` не может получить его
только через обновление `stable`-образов. Скачайте `configurator-macos-v1.1.1.tar.gz`, проверьте `SHA256SUMS`, полностью
распакуйте архив и запускайте `Update.command` из нового пакета. Стабильное имя Compose project подключит существующие
PostgreSQL и MinIO volumes; перед загрузкой новых образов будет создан обязательный backup.

Если Update из пакета `v1.1.0` уже завершился ошибкой `mounts denied`, данные не изменены: ошибка произошла до загрузки
образов. Для временного восстановления работы можно запустить `Start.command` из старого пакета.

Windows-пользователи получают обычный patch-пакет без изменения формата backup или runtime-контракта.

## Проверка происхождения

```bash
gh attestation verify configurator-macos-v1.1.1.tar.gz -R eltgm/configurator-root
gh attestation verify oci://ghcr.io/eltgm/configurator-app:1.1.1 -R eltgm/configurator-root
```

Имена и immutable digests образов находятся в `IMAGE_DIGESTS`; единый список checksum — в `SHA256SUMS`.

## Граница поддержки

Релиз предназначен для одного доверенного пользователя и только для локального loopback-доступа. Не меняйте привязку
`127.0.0.1:8080` и не публикуйте приложение в LAN или интернет. Runtime-аутентификация и авторизация не реализованы;
серверное и multi-user развёртывание не поддерживается. Backups не зашифрованы.
