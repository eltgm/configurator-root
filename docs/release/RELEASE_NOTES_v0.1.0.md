# Configurator v0.1.0 — local MVP preview

Первый preview объединяет Spring Boot backend и адаптивный React-интерфейс в локальное приложение для Windows 10/11
x86-64 и macOS Intel/Apple Silicon.

> Runtime-аутентификация и авторизация ещё не реализованы. Используйте Configurator только на личном компьютере через
> `http://127.0.0.1:8080`; не публикуйте порт в LAN или интернет.

## Быстрый запуск

1. Установите Docker Desktop.
2. Скачайте и полностью распакуйте архив для своей ОС.
3. Запустите `Start.cmd` либо `Start.command` двойным кликом.
4. Дождитесь автоматического открытия браузера.

JDK, Gradle, Node.js, npm, Git, registry login и терминал не требуются. Рядом со Start находятся Stop, Update, Backup и
Restore. Update сначала создаёт backup; Restore проверяет checksums и создаёт страховочный backup.

## Основные возможности

- предметные области, типы, атрибуты, компоненты и изображения;
- ручная совместимость, граф и автоматические attribute-to-attribute rules;
- прямой, транзитивный, batch и intersection поиск с объяснениями;
- сохранение, копирование, JSON-экспорт и удаление конфигураций;
- демонстрационная область «Сборка ПК»;
- светлая/тёмная тема, карточки/таблица, desktop/mobile layout;
- локальные Windows/macOS packages с Update, Backup и Restore.

## Проверка поставки

Release содержит `SHA256SUMS` и `IMAGE_DIGESTS`. App/gateway images публикуются в GHCR для `linux/amd64` и
`linux/arm64` с BuildKit SPDX SBOM/max provenance и GitHub OIDC attestations. Проверка provenance:

```bash
gh attestation verify configurator-windows-v0.1.0.zip -R eltgm/configurator-root
gh attestation verify oci://ghcr.io/eltgm/configurator-app:0.1.0 -R eltgm/configurator-root
```

Attestation подтверждает происхождение artifact, но не является гарантией отсутствия уязвимостей.

## Известные ограничения

- отсутствуют runtime authentication/authorization и разделение пользователей;
- поддерживается только loopback local preview, без TLS/LAN/public deployment;
- backups не шифруются;
- automatic downgrade после несовместимой Flyway migration не поддерживается;
- clean-machine compatibility проверяется владельцем перед публикацией draft pre-release.
