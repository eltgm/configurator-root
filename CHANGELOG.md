# Changelog

Все значимые изменения проекта документируются в этом файле. Формат основан
на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версии
следуют [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

### Planned

- Spring Security, регистрация/login и JWT authorization.
- Удаление определений атрибутов.
- Production configuration и deployment hardening.

## [0.1.0] - 2026-08-24

### Added

- CRUD доменов и типов компонентов.
- Создание, получение списка и обновление определений атрибутов.
- Создание, чтение, обновление, архивирование и поиск компонентов.
- Загрузка и получение изображений компонентов через MinIO.
- Ручные связи совместимости и граф домена.
- CRUD автоматических правил `attribute ↔ attribute` с объяснениями результата.
- Прямой и транзитивный поиск совместимости, batch search и intersection.
- Сохранение, чтение, пагинация и JSON-экспорт конфигураций.
- OpenAPI и jOOQ code generation, Flyway migrations.
- Unit, repository, architecture и общие local/external integration contracts.
- CI, Dependabot, GitHub templates и draft release automation.
- Совместимый со Spring Boot 3 runtime для Swagger UI и `/v3/api-docs`.
- React/Vite интерфейс для предметных областей, каталога, совместимости, конфигуратора, конфигураций и настроек.
- Демонстрационная предметная область «Сборка ПК», светлая/тёмная темы, desktop/mobile UX и WCAG 2.2 AA evidence.
- Frontend unit/component, functional Playwright, accessibility, visual regression и production delivery gates.
- Unprivileged same-origin web gateway с loopback-only entry point и production Docker image.
- Windows/macOS Start, Stop, Update, Backup и Restore packages для локального запуска через Docker Desktop.
- Multi-platform GHCR release automation с SBOM, provenance, OIDC attestations, image digests и checksums.

### Known limitations

- Контракты регистрации/login и Bearer JWT пока не имеют runtime-реализации.
- Текущий пользователь временно представлен системной записью с ID `-1`.
- Runtime поддерживает только локальный однопользовательский preview через `127.0.0.1`; LAN/public deployment не
  поддерживается.
- Публикация draft pre-release и clean-machine Windows/macOS проверка остаются явными действиями владельца.

[Unreleased]: https://github.com/eltgm/configurator-root/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/eltgm/configurator-root/releases/tag/v0.1.0
