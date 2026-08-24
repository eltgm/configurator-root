# Release checklist — v1.0.0

Дата аудита: 2026-08-24. Цель: production-ready trusted-local Windows/macOS release с Docker Desktop.

## Product и scope

- [x] Feature matrix подтверждает все заявленные пользовательские сценарии.
- [x] OpenAPI, backend, frontend и persistence/storage coverage согласованы.
- [x] Поставка ограничена loopback local-only; server deployment исключён из v1.
- [x] Разрешён clean reinstall для данных/backup до v1.0.0.

## Код и качество

- [x] ArchitectureTest фиксирует controller → facade → service → port → infrastructure.
- [x] Stable operationId добавлены в source OpenAPI; generated drift проверяется.
- [x] Actionable compiler, MapStruct, Gradle, lint и browser-console warnings устранены.
- [x] Неиспользуемый RoutePlaceholder удалён; tracked build/IDE/OS artifacts отсутствуют.
- [x] Backend и frontend coverage thresholds выполняются.

## Поставка и supply chain

- [x] Windows/macOS archives воспроизводимы и не содержат source/build tooling.
- [x] Start/Stop/Update/Backup/Restore и strict failure contracts автоматизированы.
- [x] Exact/sha/stable tags публикуются для amd64/arm64; `latest` отсутствует.
- [x] Actions pinned по full SHA, permissions минимальны, images digest-pinned/non-root.
- [x] SBOM, provenance, OIDC attestations, `IMAGE_DIGESTS` и единый `SHA256SUMS` включены.
- [x] GitHub Release создаётся draft и не помечается pre-release.

## Документация и GitHub

- [x] README, CHANGELOG, SECURITY, SUPPORT, release notes и local delivery guide соответствуют v1.0.0.
- [x] Issue/PR templates, CODEOWNERS и Dependabot покрывают поддерживаемые workflows/ecosystems.
- [ ] В GitHub настроены description, topics, social preview, default branch и branch protection.
- [ ] Включены private vulnerability reporting, secret scanning/push protection и Dependabot alerts.
- [ ] GHCR app/web packages доступны для anonymous pull.

## Проверки release candidate

- [x] `./gradlew --no-daemon clean build`.
- [x] External integration contract против production Compose.
- [x] `npm ci`, `npm run check`, `npm run test:coverage`.
- [x] Full Playwright E2E, accessibility, pinned-container visual и production delivery smoke.
- [x] Все шесть delivery contracts, включая real Docker lifecycle.
- [ ] Native Windows PowerShell 5.1 test и clean-machine Windows smoke.
- [ ] Clean-machine macOS Intel/Apple Silicon smoke.

## Публикация владельцем

- [ ] Release PR `develop` → `master` одобрен и CI зелёный.
- [ ] Annotated tag `v1.0.0` создан на commit, достижимом из `master`.
- [ ] Tag workflow завершён; checksums, attestations и anonymous pulls проверены.
- [ ] Draft release и release notes просмотрены и опубликованы вручную.

Результаты локальных проверок и оставшиеся внешние действия фиксируются в
[`RELEASE_AUDIT_v1.0.0.md`](RELEASE_AUDIT_v1.0.0.md).
