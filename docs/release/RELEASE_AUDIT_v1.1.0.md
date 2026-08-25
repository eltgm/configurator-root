# Release audit — v1.1.0

Дата аудита: 2026-08-25. Исходный release scope: CON1-132 и CON1-133.

Вердикт: **release metadata подготовлены, публикация пока заблокирована**. Код двух задач уже находится в `master`,
но annotated tag `v1.1.0` был создан на commit `ce6c4a6` до добавления секции changelog, release notes и синхронизации
версий. Такой commit не проходит шаг `Validate tag and release commit` текущего workflow. До проверки GitHub Actions,
Release и GHCR tag нельзя удалять, перемещать или повторно использовать.

## Состав обновления

1. CON1-132 — доменный каталог определений атрибутов, повторное использование между типами и отдельные настройки
   связей; Flyway `V7`, API, backend, frontend и тестовые контракты обновлены согласованно.
2. CON1-133 — assembly-aware candidates, трёхсоставное решение `ALLOWED`/`DENIED`/`UNKNOWN`, связность разрешённого
   графа для сохранения и объяснения недоступных кандидатов в UI.

## Source-of-truth impact

- OpenAPI: изменён в обеих задачах; release metadata version обновлена до `1.1.0`.
- Database/Flyway/jOOQ: добавлена migration `V7__create-domain-attribute-catalog.sql`; выпущенные миграции не менялись.
- Generated code: backend генерируется Gradle lifecycle; frontend SDK был регенерирован из OpenAPI в задачах.
- Architecture: boundary `controller → facade → service → port → infrastructure` не изменён.
- Security: без изменений; runtime auth отсутствует, поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область  | Состояние                                                                               |
|----------|-----------------------------------------------------------------------------------------|
| Backend  | Spring Boot 3.4.11; Gradle default `1.1.0-SNAPSHOT`; tag build `-PreleaseVersion=1.1.0` |
| Frontend | package/lock version `1.1.0`; Node 24 / npm 11 contract                                 |
| REST     | OpenAPI 3.0.3, info version `1.1.0`                                                     |
| Database | Flyway V1–V7; upgrade-path review на непустой базе pending                              |
| Delivery | Windows/macOS packages, backup format v1, channel `stable` без `latest`                 |

## Предшествующие проверки задач

- CON1-132: Gradle build, external integration, frontend check/coverage, 72 Playwright E2E, 36 accessibility,
  visual update и production delivery smoke прошли; ручная проверка migration `V7` на копии непустой базы осталась.
- CON1-133: Gradle unit/local/build/external, frontend check/coverage, 69 Playwright E2E и 34 accessibility checks
  прошли; visual regression и delivery e2e не запускались, поскольку delivery-контракт не менялся.

Эти результаты подтверждают соответствующие task commits, но не заменяют повторный полный прогон окончательного
release commit. Фактические проверки текущей подготовки будут добавлены после запуска.

## Release blockers

1. В GitHub проверить состояние workflow, draft release и GHCR exact tag `1.1.0`, запущенных существующим tag.
2. Если tag не был опубликован и exact images отсутствуют, удалить premature remote/local tag по owner-approved
   процедуре
   из `GIT_RELEASE_v1.1.0.md`; если release или immutable images уже появились — не перемещать tag и выбрать новую
   версию.
3. Провести release-preparation PR через `develop`, затем release PR `develop` → `master`; direct push запрещён.
4. Повторить полный backend/frontend/browser/external/delivery matrix на окончательном release commit.
5. Проверить migration `V7` на копии непустой базы `v1.0.0` и выполнить clean-machine Windows/macOS smoke.
6. Проверить anonymous pull, checksums, attestations и вручную опубликовать только проверенный draft.

Локальное изменение `configurator-integration-tests/src/test/resources/testcontainers.properties` принадлежит
пользователю и не входит в release commit.
