# Configurator v1.1.3

Patch release локального Configurator для Windows и macOS. Пользователю нужны Docker Desktop, интернет для первого
запуска и полностью распакованный архив; JDK, Node.js и Git не требуются.

## Исправление

- **Надёжные Windows lifecycle scripts.** Docker Compose может направлять обычный progress, включая состояние
  контейнеров, в stderr. В Windows PowerShell 5.1 эти сообщения больше не останавливают Start, Backup, Restore или
  Update. Скрипты по-прежнему реагируют на реальную ошибку Docker по его exit code и сохраняют прежние коды завершения.

## Обновление

Скачайте пакет `v1.1.3` для своей ОС, проверьте `SHA256SUMS`, полностью распакуйте архив и запустите `Update` из нового
пакета. Стабильное имя Compose project подключит существующие PostgreSQL и MinIO volumes; перед загрузкой новых
образов будет создан обязательный backup. Формат backup v1 и схема базы данных не менялись.

## Проверка происхождения

```bash
gh attestation verify configurator-macos-v1.1.3.tar.gz -R eltgm/configurator-root
gh attestation verify oci://ghcr.io/eltgm/configurator-app:1.1.3 -R eltgm/configurator-root
```

Имена и immutable digests образов находятся в `IMAGE_DIGESTS`; единый список checksum — в `SHA256SUMS`.

## Граница поддержки

Релиз предназначен для одного доверенного пользователя и только для локального loopback-доступа. Не меняйте привязку
`127.0.0.1:8080` и не публикуйте приложение в LAN или интернет. Runtime-аутентификация и авторизация не реализованы;
серверное и multi-user развёртывание не поддерживается. Backups не зашифрованы.
