# Changelog

Все значимые изменения документируются в этом файле. Формат основан на
[Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версии следуют
[Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

## [1.1.3] - 2026-08-26

### Fixed

- Windows Update, Backup и Start больше не принимают штатный Docker Compose progress в stderr за ошибку в Windows
  PowerShell 5.1. Реальные сбои Docker по-прежнему определяются по его exit code.

## [1.1.2] - 2026-08-26

### Fixed

- Редактор сохранённой конфигурации использует ту же assembly-aware проверку, что и создание: связная сборка с
  `UNKNOWN`-парами сохраняется, `DENIED` и `DISCONNECTED` различаются, а добавление компонента может восстановить
  связность состава.

## [1.1.1] - 2026-08-25

### Fixed

- macOS Backup, Restore и обязательный backup перед Update больше не требуют добавлять каталог пакета в Docker
  Desktop File Sharing: maintenance-артефакты передаются через внутренний Docker volume без host bind mount.

## [1.1.0] - 2026-08-25

### Added

- Добавлен переиспользуемый каталог атрибутов предметной области: определение можно создавать независимо от типа,
  подключать к нескольким типам компонентов и настраивать обязательность и порядок отдельно для каждой связи.
- Добавлена интуитивная совместимость сборки: кандидаты оцениваются относительно уже выбранных компонентов,
  несовместимые варианты блокируются с объяснением, а сохранённой конфигурации достаточно связного графа разрешённых
  отношений вместо полной совместимости каждой пары.

## [1.0.0] - 2026-08-24

### Added

- Полный CRUD предметных областей, типов, атрибутов и компонентов с архивированием и фильтрацией.
- Хранение, сортировка и выдача изображений компонентов через MinIO.
- Ручные связи, граф и автоматические правила совместимости с объяснением результата.
- Прямой, транзитивный, множественный поиск и пересечение совместимых компонентов.
- Сохранение, копирование, пагинация и JSON-экспорт конфигураций.
- Адаптивный React-интерфейс, демонстрационная область, темы, desktop/mobile navigation.
- Единые local/external integration contracts, frontend unit/E2E/accessibility/visual/delivery gates.
- Воспроизводимые Windows/macOS пакеты для Docker Desktop с Start, Stop, Update, Backup и Restore.
- Public multi-platform GHCR images, checksums, digests, SBOM, provenance и OIDC attestations.

### Changed

- Пользовательская поставка переведена на стабильный канал образов `stable`; тег `latest` не публикуется.
- REST-операции получили стабильные `operationId`; API формализован как OpenAPI 3.0.3.
- Frontend routes загружаются по требованию, а browser gates выполняются против production build server и отклоняют
  ошибки приложения в консоли.
- Обновлены поддерживаемые frontend dependencies и зафиксирована opt-in policy для install script MSW.
- Версии Gradle, frontend package и release automation синхронизированы с `1.0.0`.

### Fixed

- Устранены actionable MapStruct, unchecked compiler и Gradle/JUnit launcher warnings.
- Удалён неиспользуемый route placeholder и устранён oversized entry chunk frontend.

### Support boundary

- Поддерживаются только локальные пакеты Windows 10/11 x86-64 и macOS Intel/Apple Silicon с Docker Desktop.
- Единственная поддерживаемая точка входа — `http://127.0.0.1:8080`; LAN/public/server deployment не входит в v1.0.0.
- Совместимость с данными и backup формата до `v1.0.0` не гарантируется; разрешена чистая переустановка.

[Unreleased]: https://github.com/eltgm/configurator-root/compare/v1.1.3...HEAD

[1.1.3]: https://github.com/eltgm/configurator-root/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/eltgm/configurator-root/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/eltgm/configurator-root/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/eltgm/configurator-root/releases/tag/v1.1.0
[1.0.0]: https://github.com/eltgm/configurator-root/releases/tag/v1.0.0
