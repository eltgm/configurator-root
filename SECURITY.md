# Security Policy

## Поддерживаемые версии

| Версия | Поддержка |
|--------|-----------|
| 1.1.x  | Да        |
| 1.0.x  | Да        |
| < 1.0  | Нет       |

## Граница поддерживаемой эксплуатации

Configurator v1 поддерживает только локальные Windows/macOS пакеты с Docker Desktop. Gateway обязан оставаться на
`127.0.0.1:8080`; backend, PostgreSQL и MinIO не публикуются из внутренней Docker network. LAN, публичная сеть,
server deployment и multi-user эксплуатация не входят в security contract этой версии.

Пакет содержит локальные credentials инфраструктуры, а backups не зашифрованы. Храните распакованный пакет и backups
в каталоге текущего пользователя, не синхронизируйте их в публичные хранилища и не прикладывайте к issues.

## Сообщение об уязвимости

Не создавайте публичный issue. Используйте
[GitHub Private Vulnerability Reporting](https://github.com/eltgm/configurator-root/security/advisories/new) и укажите:

- затронутую версию и ОС;
- точные шаги воспроизведения и ожидаемое влияние;
- минимальный proof of concept без реальных секретов или персональных данных;
- известные обходные меры.

Получение отчёта подтверждается в течение 7 дней. Статус, план исправления и coordinated disclosure согласуются в
приватном advisory. Публичная публикация до выпуска исправления нежелательна.

## Dependency и supply-chain controls

Dependabot отслеживает npm, Gradle, Docker и GitHub Actions. Release workflow использует минимальные permissions,
full-SHA action pins, digest-pinned runtime images, SBOM/provenance и GitHub OIDC attestations. Пользователь должен
проверять `SHA256SUMS` и при необходимости attestations перед установкой.
