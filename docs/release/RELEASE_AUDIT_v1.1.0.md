# Release audit — v1.1.0

Дата аудита: 2026-08-25. Исходный release scope: CON1-132 и CON1-133.

Вердикт: **release candidate исправлен локально, публикация пока заблокирована повторным merge/tag workflow**.
Annotated tag `v1.1.0` указывает на commit `6b032888`, где functional E2E и accessibility прошли, но pinned visual
regression остановился на устаревшем `configurator-light.png`: 28 853 пикселя отличались одинаково во всех трёх
попытках. Downstream jobs публикации не запускались, поскольку зависят от успешного `Verify release candidate`.

Diff подтвердил ожидаемое изменение assembly-aware workspace из CON1-133: новые тексты политики и меньшая высота
информационных блоков сдвинули содержимое страницы. Baseline просмотрен, обновлён только для этого экрана и повторно
проверен в pinned Playwright container — 8/8 visual tests прошли.

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
| -------- | --------------------------------------------------------------------------------------- |
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

## Проверки release preparation и visual fix

| Проверка                                                                                        | Результат                                      |
| ----------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `./gradlew --no-daemon clean build -PreleaseVersion=1.1.0 -PspotlessRatchetFrom=origin/develop` | PASS, 24 tasks                                 |
| `npm ci && npm run check`                                                                       | PASS, 43 suites / 213 tests и production build |
| `npm run test:coverage`                                                                         | PASS, statements 90.11%, lines 90.66%          |
| Functional Playwright в tag workflow                                                            | PASS, 72/72 Chromium/Firefox/WebKit            |
| Accessibility в tag workflow                                                                    | PASS, 36/36 desktop/mobile checks              |
| Пять non-Docker-lifecycle delivery contracts                                                    | PASS                                           |
| `npm run test:visual:update` после review diff                                                  | PASS, 8/8; обновлён один baseline              |
| Повторный `npm run test:visual`                                                                 | PASS, 8/8 в pinned Playwright container        |

Предшествующие task results и эти проверки не заменяют оставшийся external/delivery matrix на окончательном release
commit.

## Release blockers

1. Влить visual-baseline fix через `develop`, затем release PR `develop` → `master`; direct push запрещён.
2. После проверки отсутствия draft release и exact images удалить failed remote/local tag по owner-approved процедуре
   из `GIT_RELEASE_v1.1.0.md`, затем создать tag на окончательном release commit.
3. Повторить полный tag workflow, включая external integration, production delivery smoke и Docker lifecycle.
4. Проверить migration `V7` на копии непустой базы `v1.0.0` и выполнить clean-machine Windows/macOS smoke.
5. Проверить anonymous pull, checksums, attestations и вручную опубликовать только проверенный draft.

Локальное изменение `configurator-integration-tests/src/test/resources/testcontainers.properties` принадлежит
пользователю и не входит в release commit.
