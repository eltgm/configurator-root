# First release checklist — v0.1.0

Дата аудита: 2026-07-31. Целевой статус: MVP preview, не production-ready.

## Функциональная готовность

- [x] Domains, component types и attributes.
- [x] Components: create/get/list/update/archive.
- [x] Component images через MinIO.
- [x] Manual compatibility links и graph.
- [x] Attribute-to-attribute automatic rules и explanations.
- [x] Direct/transitive/batch/intersection configurator search.
- [x] Saved configurations и JSON export.
- [ ] Authentication/authorization — осознанно перенесены после первого preview release.
- [ ] Frontend — вне scope backend repository.

## Репозиторий

- [x] README соответствует runtime-функционалу.
- [x] CONTRIBUTING и pull request template.
- [x] SECURITY policy и private-reporting link.
- [x] Code of Conduct.
- [x] CODEOWNERS.
- [x] Issue forms.
- [x] Dependabot для Gradle, Docker и GitHub Actions.
- [x] CI и draft release workflow.
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

## Выпуск

1. [ ] Обновить `CHANGELOG.md`, версию и дату.
2. [ ] Открыть и проверить PR `develop -> master`.
3. [ ] После merge убедиться, что CI на `master` зелёный.
4. [ ] Создать annotated tag `v0.1.0` на commit из `master` и push tag.
5. [ ] Дождаться release workflow: он повторно проверит проект и создаст draft release.
6. [ ] Проверить JAR, OpenAPI, `SHA256SUMS` и generated notes.
7. [ ] Опубликовать draft как pre-release.
8. [ ] Выполнить smoke test из release artifact.

## Блокеры production-ready v1.0.0

- Spring Security/JWT и реальный `CurrentUserProvider`.
- Authorization policy и negative security tests.
- Production secrets/configuration, TLS и deployment strategy.
- Backup/restore и миграционный rollback/runbook.
- Observability, health/readiness probes и operational SLO.
- Формализованная support policy и release maintenance window.
