# First release checklist — v0.1.0

Дата аудита: 2026-08-24. Целевой статус: локальный MVP preview, не production-ready.

## Функциональная готовность

- [x] Domains, component types и attributes.
- [x] Components: create/get/list/update/archive.
- [x] Component images через MinIO.
- [x] Manual compatibility links и graph.
- [x] Attribute-to-attribute automatic rules и explanations.
- [x] Direct/transitive/batch/intersection configurator search.
- [x] Saved configurations и JSON export.
- [ ] Authentication/authorization — осознанно перенесены после первого preview release.
- [x] React frontend: catalog, configurator, compatibility, configurations and settings UX.

## Репозиторий

- [x] README соответствует runtime-функционалу.
- [x] CONTRIBUTING и pull request template.
- [x] SECURITY policy и private-reporting link.
- [x] Code of Conduct.
- [x] CODEOWNERS.
- [x] Issue forms.
- [x] Dependabot для Gradle, Docker и GitHub Actions.
- [x] CI и rerunnable draft release workflow с multi-platform publishing/attestations.
- [x] CHANGELOG.
- [x] Выбрана и добавлена лицензия MIT.
- [ ] Настроены repository description, topics и social preview.

Рекомендуемое описание GitHub:

> Backend-first component configurator with attribute-based compatibility rules, graph search, saved configurations,
> Spring Boot, PostgreSQL and MinIO.

Рекомендуемые topics:

`java`, `spring-boot`, `postgresql`, `jooq`, `openapi`, `testcontainers`, `minio`, `component-configurator`,
`hexagonal-architecture`

## GitHub settings

- [ ] Default branch осознанно выбрана (`master` остаётся stable; разработка идёт через `develop`).
- [ ] Branch protection/ruleset для `develop`: PR required, CI required, no force push/delete.
- [ ] Branch protection/ruleset для `master`: PR required, CI required, no force push/delete.
- [ ] Actions default `GITHUB_TOKEN` permission = read-only.
- [ ] Dependency graph, Dependabot alerts и security updates включены.
- [ ] Secret scanning и push protection включены, если доступны.
- [ ] Private vulnerability reporting включён.
- [ ] Issues включены; Wiki/Projects — только если действительно используются.

## Проверка кандидата

- [x] `./gradlew clean build`.
- [x] `docker compose up -d --build`.
- [x] `./gradlew :configurator-integration-tests:externalIntegrationTest`.
- [x] Swagger UI и `/v3/api-docs` доступны.
- [x] MinIO upload/read проверены external contract.
- [ ] В git отсутствуют secrets, `.DS_Store`, IDE и host-specific settings.
- [x] JAR запускается в пересобранном runtime container.

## Локальная пользовательская поставка

9.29 реализует package lifecycle, а 9.30 — multi-platform publishing, assets и supply-chain automation. Не считать
workflow готовым пользовательским релизом до фактического trusted tag run и ручной публикации проверенного draft.

- [x] Image-only Compose без `build`, с единственным bind `127.0.0.1:8080`.
- [x] Windows `.cmd`/PowerShell 5.1 и macOS `.command`/Bash 3.2 launchers.
- [x] Start/Stop/Update/Backup/Restore package contracts и versioned archive builder.
- [x] PostgreSQL + MinIO backup format v1, checksums и pre-restore safety backup.
- [x] Real Docker lifecycle: destructive mutation/recovery, preview Update success и strict failure.
- [x] 9.30 automation: exact/sha/preview tags без `latest`, minimum permissions и full-SHA action pins.
- [x] 9.30 automation: BuildKit SPDX SBOM/max provenance и GitHub OIDC attestations для images/assets.
- [x] 9.30 automation: versioned JAR/OpenAPI/Windows/macOS/`IMAGE_DIGESTS`/unified `SHA256SUMS` asset contract.
- [ ] Trusted tag run: public `linux/amd64` + `linux/arm64` app/gateway manifests доступны без registry login.
- [ ] Trusted tag run: Windows/macOS assets и `SHA256SUMS` прикреплены к draft GitHub Release.
- [ ] Trusted tag run: image/download attestations фактически проверены через `gh attestation verify`.
- [ ] При первом push оба GHCR packages переведены в public, связаны с repository, затем workflow повторно запущен.
- [ ] Чистая Windows 10/11 x86-64: download, extract, Start/repeated Start, Stop, Update, Backup, Restore.
- [ ] Чистая macOS Intel: download, extract, right-click/Open, полный lifecycle и browser opening.
- [ ] Чистая macOS Apple Silicon: download, extract, right-click/Open, полный lifecycle и browser opening.
- [ ] Проверено, что архивы не содержат source/build tools/secrets/host paths/IDE/OS metadata.
- [ ] Проверено предупреждение local-only/no-auth и отсутствие LAN/public exposure.

## Выпуск

1. [x] Обновить `CHANGELOG.md`, версию и дату.
2. [ ] Открыть и проверить PR `develop -> master`.
3. [ ] После merge убедиться, что CI на `master` зелёный.
4. [ ] Создать annotated tag `v0.1.0` на commit из `master` и push tag.
5. [ ] Дождаться release workflow: он повторно проверит проект и создаст draft release.
6. [ ] Проверить JAR, OpenAPI, пользовательские архивы, `IMAGE_DIGESTS`, `SHA256SUMS`, image manifests, attestations и
       generated notes.
7. [ ] Опубликовать draft как pre-release.
8. [ ] Выполнить smoke test из release artifact.

## Блокеры production-ready v1.0.0

- Spring Security/JWT и реальный `CurrentUserProvider`.
- Authorization policy и negative security tests.
- Production secrets/configuration, TLS и deployment strategy.
- Production backup/restore, retention/encryption и миграционный rollback/runbook.
- Observability, health/readiness probes и operational SLO.
- Формализованная support policy и release maintenance window.
