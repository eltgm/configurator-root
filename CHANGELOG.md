# Changelog

Все значимые изменения проекта документируются в этом файле. Формат основан
на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версии
следуют [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

### Planned

- Spring Security, регистрация/login и JWT authorization.
- Удаление определений атрибутов.
- Production configuration и deployment hardening.

## [0.1.0] - 2026-07-31

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

### Known limitations

- Контракты регистрации/login и Bearer JWT пока не имеют runtime-реализации.
- Текущий пользователь временно представлен системной записью с ID `-1`.
- Frontend и production deployment в репозиторий не входят.

[Unreleased]: https://github.com/eltgm/configurator-root/compare/v0.1.0...HEAD

[0.1.0]: https://github.com/eltgm/configurator-root/releases/tag/v0.1.0
