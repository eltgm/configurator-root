# CON1-126 — E2E, Accessibility and Visual Regression Quality Gates

## Overview

Пункт 9.27 превращает существующие frontend-проверки из локального smoke-набора в воспроизводимую quality platform:
критические пользовательские пути остаются проверенными в Chromium, Firefox и WebKit, автоматически обнаруживаемые
нарушения WCAG 2.2 A/AA блокируют сборку, а стабильные визуальные состояния сравниваются с reviewed baselines.

Задача преимущественно меняет test infrastructure, fixtures, CI и документацию. Runtime UI изменяется только если axe
или новые E2E-сценарии обнаружат реальный дефект; такое исправление выполняется локально вместе с regression test.
OpenAPI, backend, Flyway, jOOQ, БД и generated API client менять не требуется.

## Context (from discovery)

- `develop` уже содержит 9.26 (`0852ff9`, merge PR #58), поэтому 9.27 может начинаться независимой веткой
  `feature/CON1-126` от актуального `develop`.
- Текущий Playwright-набор — один файл `configurator-web/e2e/smoke.spec.ts` на 1308 строк. В нём находятся typed fixtures,
  stateful `page.route` mock backend и 13 функциональных сценариев; три browser projects дают 39 успешных тестов.
- Существующие E2E хорошо покрывают configurator direct/transitive/conflict flows, полный lifecycle configuration,
  gallery, manual compatibility, automatic rules, graph и 320 px reflow.
- Не хватает E2E для первого запуска/demo domain, CRUD предметных областей, CRUD типов/атрибутов, edit/archive/restore
  компонента, unsaved-change guards и детерминированного error/retry flow.
- Общая stateful API fixture встроена в `smoke.spec.ts`, поэтому её нельзя переиспользовать в accessibility/visual suites
  без копирования; unhandled `/api` request сейчас не всегда является явной ошибкой теста.
- `@axe-core/playwright` отсутствует. Текущая latest stable версия — `4.13.0`; пакет имеет встроенные TypeScript types и
  использует `axe-core` соответствующей minor-линии.
- `docs/accessibility/WCAG_2_2_AA_AUDIT.md` уже помечает критерии, которым 9.27 должен добавить automated evidence.
- Playwright visual snapshots отсутствуют; `playwright-report/` и `test-results/` уже игнорируются Git, committed golden
  snapshots нужно хранить отдельно.
- `.github/workflows/ci.yml` запускает backend build и external contracts, но frontend в GitHub CI не устанавливается и
  не проверяется. Dependabot не отслеживает npm в `configurator-web`.
- Playwright рекомендует `@axe-core/playwright` для automated accessibility scans, один worker в CI и одинаковую ОС,
  browser version/settings при создании и сравнении screenshot baselines.
- Пользовательский `configurator-integration-tests/src/test/resources/testcontainers.properties` не относится к задаче
  и не должен редактироваться, форматироваться, индексироваться или попадать в commit.
- Spring planning MCP недоступен, поскольку IntelliJ IDEA с установленным Amplicode не запущена; план подготовлен по
  локальному source/CI/test audit без MCP-модели проекта.

Official references:

- Playwright accessibility testing: https://playwright.dev/docs/accessibility-testing
- Playwright visual comparisons: https://playwright.dev/docs/test-snapshots
- Playwright CI guidance: https://playwright.dev/docs/ci
- Playwright Docker images: https://playwright.dev/docs/docker
- axe-core tags: https://github.com/dequelabs/axe-core/blob/develop/doc/API.md#axe-core-tags

## Proposed Product Contract

1. Functional E2E остаются black-box SPA-тестами с mock HTTP boundary и выполняются в Chromium, Firefox и WebKit.
2. Общий typed stateful mock backend является отдельной fixture. Каждый test получает новое состояние; тест может явно
   переопределить response/latency/error, а необработанный `/api` request завершает тест понятной ошибкой.
3. Монолитный `smoke.spec.ts` разделяется по предметным journeys без изменения бизнес-семантики и без `waitForTimeout`:
   shell/domains, components, compatibility, configurator и configurations.
4. E2E покрывают критический путь от первого запуска/demo domain до создания/подбора/сохранения/редактирования/экспорта
   конфигурации, включая destructive confirmation, domain isolation, archive/restore и recoverable request error.
5. `@axe-core/playwright` запускается отдельной Chromium suite на desktop и mobile. Gate использует WCAG tags
   `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa`.
6. Accessibility scan выполняется после полного settle конкретного состояния: не только initial routes, но также menu,
   modal, Drawer, validation errors, mobile navigation, light/dark, catalog cards/table и graph details.
7. Axe violations всегда падают с rule/help/selector summary и прикладывают полный JSON к Playwright result. `incomplete`
   прикладываются для ручного review, но сами по себе не считаются доказанным нарушением.
8. Blanket `exclude`, глобальное отключение rules и committed allowlist известных нарушений не используются. Реальные
   нарушения исправляются. Исключение допускается только для подтверждённого third-party false positive с точным rule,
   selector, объяснением и отдельным follow-up.
9. Axe не объявляется полной проверкой WCAG: keyboard, VoiceOver, zoom, text spacing и meaning остаются в manual matrix.
10. Visual regression использует Playwright `toHaveScreenshot` и committed lossless PNG baselines для фиксированных
    representative desktop/mobile, light/dark состояний.
11. Visual gate выполняется только в Chromium внутри pinned official Playwright Linux image той же версии, что и
    `@playwright/test`; functional browser matrix не сокращается.
12. Baselines создаются и обновляются в том же Docker environment через cross-platform npm command, чтобы macOS/Windows
    rendering и host `node_modules` не влияли на результат.
13. Screenshots детерминированы: fixed viewport/locale/timezone/mock dates/data/theme, loaded fonts, hidden caret,
    disabled animations и отсутствие live network. Маскирование допускается только для доказанно динамического региона.
14. Visual comparison строгий: широкие `maxDiffPixelRatio`/`maxDiffPixels` не используются для сокрытия расхождений;
    изменение baseline всегда является отдельным reviewed diff в PR.
15. Representative baseline inventory включает AppShell, configurator workspace, catalog cards/table, component details
    and gallery, types/attributes, rules, graph, configurations и один overlay state. Не создаётся snapshot каждого
    шага каждого E2E.
16. GitHub CI получает frontend quality job и browser quality job. Проверяются API drift, format/lint/styles,
    typecheck/build, unit coverage thresholds, functional E2E, axe и visual baselines.
17. Playwright report/test-results загружаются только при failure, с коротким retention; тесты используют только mock
    data и не включают credentials/tokens.
18. Frontend coverage получает явные минимумы не выше текущего baseline: lines/statements 90%, functions 85%, branches
    80%. Gate не подменяет содержательные E2E/accessibility tests.
19. Dependabot отслеживает npm dependencies в `/configurator-web`; PR template/CONTRIBUTING/AGENTS описывают новые
    обязательные команды и безопасное обновление visual baselines.

## Considered Approaches

### A. Layered deterministic Playwright suites with a Linux visual container — selected

- Functional, accessibility and visual checks имеют общий mock backend, но отдельные configs/scripts/reports.
- Functional E2E остаются cross-browser; axe выполняется в Chromium desktop/mobile; visual — в одном pinned Linux
  Chromium environment.
- Baseline update запускается через Docker Desktop одинаково на Windows и macOS.
- Плюсы: воспроизводимые diffs, ясная причина падения, разумное CI-время, отсутствие внешнего SaaS/account.
- Минусы: нужен небольшой Docker wrapper и review committed binary baselines.

### B. One Playwright config and screenshots for every browser/platform

- Все functional, axe и visual tests запускаются одним `playwright test`.
- Плюсы: меньше конфигурационных файлов.
- Минусы: screenshots зависят от ОС/fonts/browser, baseline inventory растёт втрое, локальные macOS/Windows и Linux CI
  создают разные goldens, а axe без пользы повторяется одной engine во всех браузерах.
- Не выбран из-за flakiness и стоимости сопровождения.

### C. Hosted visual service (Percy, Chromatic or equivalent)

- Внешний сервис хранит baselines, показывает web diff и может нормализовать rendering.
- Плюсы: удобный review UI и history.
- Минусы: отдельный account/token, внешний vendor, ограничения тарифа и усложнение локального single-user проекта.
- Не выбран для первого релиза; может быть рассмотрен при появлении команды и бюджета.

## Development Approach

- **Testing approach:** regular — сначала extraction/configuration, затем tests и найденные UI fixes в рамках каждой
  задачи; каждый этап должен оставлять предыдущие suites зелёными.
- Делать небольшие изменения и не смешивать refactor fixture с расширением product scenarios в одном шаге.
- Каждый новый helper/config/wrapper получает contract coverage через фактический запуск соответствующей suite и
  negative/self-test, где это применимо.
- Не ослаблять existing assertions ради прохождения новых gates; любой найденный runtime defect исправлять вместе с
  targeted regression test.
- После каждого этапа обновлять этот план и WCAG evidence matrix.

## Solution Overview

```text
typed deterministic mock API fixture
              |
              +--> functional specs ------> Chromium + Firefox + WebKit
              |
              +--> accessibility specs ---> Chromium desktop/mobile + axe WCAG gate
              |
              +--> visual specs ----------> pinned Linux Chromium + committed PNG baselines
                                                   |
                                                   +--> same Docker image locally and in CI

frontend-quality CI --> api/check/lint/unit/build/coverage
frontend-browser CI --> functional/accessibility/visual + failure artifacts
```

## Technical Details

### Test fixture

- `e2e/fixtures/test.ts` расширяет Playwright base test и экспортирует `test`, `expect`, `mockApi`.
- `mockApi` хранит domains, types, attributes, components/images, manual links, rules, configurations и counters.
- Route registration извлекается из `smoke.spec.ts`; transport models импортируются через `src/shared/api` либо
  generated types только в boundary fixture, без handwritten duplicates.
- Fixture предоставляет точечные methods для response override/failure, initial localStorage и inspection captured
  requests; тесты не копируют `page.route` blocks.
- Unhandled `/api` request возвращает explicit 501/error и регистрируется как test failure; external network блокируется.
- Fixed timestamps, locale `ru-RU` и timezone `Europe/Moscow` обеспечивают одинаковые тексты/snapshots.

### Accessibility

- Общий `makeAxeBuilder(page)` задаёт только WCAG A/AA tags, без exclusions.
- `expectNoAxeViolations` прикладывает JSON и формирует компактное сообщение `rule -> impact -> selectors -> helpUrl`.
- Route matrix сканирует settled main states; interaction matrix отдельно открывает overlays/errors и повторяет scan.
- Desktop/light, desktop/dark и mobile representative coverage проверяют в том числе color contrast разных themes.
- Axe results обновляют evidence в `WCAG_2_2_AA_AUDIT.md`, но ручные критерии не переводятся в `covered` без ручной
  проверки.

### Visual regression

- Отдельный `playwright.visual.config.ts` запускает только `*.visual.spec.ts` в Chromium Linux.
- Baselines хранятся через explicit `snapshotPathTemplate` в `e2e/__screenshots__`, с понятными именами и project name.
- `toHaveScreenshot(..., { animations: 'disabled', caret: 'hide', scale: 'css' })`; before screenshot ожидаются fonts и
  конкретный ready UI state.
- Основной формат — lossless PNG. Full-page capture используется только когда нужен весь layout; для overlays и
  отдельных сложных blocks предпочтителен locator screenshot.
- Docker wrapper сверяет версию image с locked `@playwright/test`, копирует source в изолированный container workspace,
  не заменяет host `node_modules` Linux-артефактами и возвращает только baseline/result artifacts.

### CI and reports

- `frontend-quality`: Node 24/npm 11, `npm ci`, `npm run check`, `npm run test:coverage`.
- `frontend-browser`: `needs: frontend-quality`, pinned Playwright Noble image matching lockfile, `npm ci`, functional,
  accessibility и visual scripts, один worker в CI.
- Functional/accessibility/visual configs пишут в разные report/output folders, чтобы последовательные suites не
  перезаписывали диагностику.
- CI reporter добавляет GitHub annotations; HTML reports, traces, screenshots and JSON accessibility results сохраняются
  только при failure на 7 дней.
- Existing backend build/external jobs не меняют семантику и могут выполняться параллельно с frontend quality.

## What Goes Where

- `configurator-web/e2e/fixtures` — deterministic mock backend, base test и helpers.
- `configurator-web/e2e/*.spec.ts` — functional journeys по предметным областям.
- `configurator-web/e2e/accessibility` — axe route/interaction matrices и result formatter.
- `configurator-web/e2e/visual` — representative screenshot scenarios.
- `configurator-web/e2e/__screenshots__` — reviewed Linux Chromium PNG baselines.
- `configurator-web/playwright*.config.ts` — separate functional/accessibility/visual projects and report paths.
- `configurator-web/scripts` — cross-platform visual Docker runner/version guard.
- `.github/workflows/ci.yml` — frontend quality gates and failure artifacts.
- `docs/accessibility` — automated/manual evidence and baseline policy.

## Testing Strategy

- **Fixture contract:** state isolation, request capture, error override and unhandled route failure.
- **Functional E2E:** three browsers, all critical successful journeys plus selected recovery/destructive/guard paths.
- **Accessibility:** Chromium desktop/mobile axe scans for route and interactive states, light/dark, zero violations.
- **Visual:** Chromium Linux only, strict committed baselines and a self-test that fails against an intentionally altered
  screenshot during development (not committed as a failing test).
- **Unit coverage:** explicit thresholds lines/statements 90, functions 85, branches 80.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e`, `npm run test:accessibility`, `npm run test:visual`, Docker baseline command, `git diff --check`.
- **CI validation:** workflow syntax/review locally where possible; definitive container/action result проверяется после
  push/PR и не выдаётся за локально выполненный.
- Backend/external integration не запускаются, если runtime/OpenAPI/Docker delivery действительно не меняются.

## Implementation Steps

### Task 1: Finalize the 9.27 quality contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Create: `docs/testing/FRONTEND_TESTING.md`
- Modify: `docs/plans/20260823-e2e-accessibility-visual-regression.md`

- [x] добавить подробные acceptance criteria для functional E2E, axe, visual baselines и CI
- [x] зафиксировать browser/viewport/theme matrices и отсутствие blanket exclusions/tolerances
- [x] определить critical journey inventory и manual-vs-automated WCAG boundary
- [x] проверить требования на непротиворечивость с 9.26 и 9.28
- [x] выполнить format check документации перед Task 2

### Task 2: Extract a deterministic typed Playwright fixture

**Files:**

- Create: `configurator-web/e2e/fixtures/test.ts`
- Create: `configurator-web/e2e/fixtures/mock-api.ts`
- Create: `configurator-web/e2e/fixtures/data.ts`
- Create as needed: focused fixture helpers/tests under `configurator-web/e2e/fixtures`
- Modify: `configurator-web/e2e/smoke.spec.ts`

- [x] перенести test data/stateful routes из monolithic spec без изменения существующих assertions
- [x] добавить state reset, typed request capture, initial-state and response-error override API
- [x] блокировать external network и делать unhandled `/api` request явной ошибкой
- [x] добавить fixture isolation/error contract scenario
- [x] прогнать все 39 existing cross-browser scenarios до дальнейшего split

### Task 3: Split and complete functional E2E journeys

**Files:**

- Create: `configurator-web/e2e/shell-domains.spec.ts`
- Create: `configurator-web/e2e/components.spec.ts`
- Create: `configurator-web/e2e/compatibility.spec.ts`
- Create: `configurator-web/e2e/configurator.spec.ts`
- Create: `configurator-web/e2e/configurations.spec.ts`
- Remove after migration: `configurator-web/e2e/smoke.spec.ts`

- [x] распределить existing tests по предметным specs без дублирования fixture/setup
- [x] добавить first-run/demo и domain create/edit/delete/switch guard journeys
- [x] добавить component type/attribute CRUD и component edit/archive/restore journeys
- [x] добавить deterministic request failure/retry и unsaved-change guard paths
- [x] сохранить configurator/configuration/compatibility lifecycle coverage и 320 px route sweep
- [x] прогнать functional suite в Chromium, Firefox и WebKit; все tests должны пройти

### Task 4: Add accessibility test infrastructure

**Files:**

- Modify: `configurator-web/package.json`
- Modify: `configurator-web/package-lock.json`
- Create: `configurator-web/playwright.accessibility.config.ts`
- Create: `configurator-web/e2e/accessibility/axe-test.ts`
- Create: `configurator-web/e2e/accessibility/axe-results.ts`
- Create tests as appropriate for formatter/config behavior

- [x] добавить pinned `@axe-core/playwright` и отдельные accessibility scripts/config
- [x] реализовать WCAG 2.0/2.1/2.2 A/AA builder без blanket exclusions
- [x] реализовать readable violation summary и JSON attachment включая incomplete results
- [x] проверить helper на known accessible и intentionally invalid local fixture/state
- [x] прогнать lint/typecheck и focused accessibility infrastructure tests

### Task 5: Gate route and interaction accessibility states

**Files:**

- Create: `configurator-web/e2e/accessibility/routes.accessibility.spec.ts`
- Create: `configurator-web/e2e/accessibility/interactions.accessibility.spec.ts`
- Modify as needed: runtime UI/CSS and co-located component tests for real violations
- Modify: `docs/accessibility/WCAG_2_2_AA_AUDIT.md`

- [x] сканировать все top-level routes после settled loading state на desktop и representative mobile
- [x] сканировать light/dark contrast-sensitive states
- [x] сканировать preferences/domain menus, forms with validation, destructive modal, explanation Drawer and graph details
- [x] исправить обнаруженные violations без global rule suppression и добавить targeted regression tests
- [x] обновить automated evidence/status matrix, не закрывая manual-only criteria
- [x] прогнать accessibility suite до zero violations

### Task 6: Create deterministic visual test infrastructure

**Files:**

- Create: `configurator-web/playwright.visual.config.ts`
- Create: `configurator-web/e2e/visual/visual-test.ts`
- Create: `configurator-web/e2e/visual/visual-stability.css`
- Create: `configurator-web/scripts/run-visual-tests.mjs`
- Modify: `configurator-web/package.json`
- Modify: `configurator-web/playwright.config.ts`

- [x] отделить functional/accessibility/visual testMatch, output and report paths
- [x] настроить fixed Chromium Linux viewport/locale/timezone/theme and strict screenshot comparison
- [x] отключить animations/caret и ожидать fonts/settled server state без arbitrary sleeps
- [x] реализовать cross-platform Docker runner с exact Playwright image/version guard и защитой host node_modules
- [x] проверить normal/update modes и намеренное visual mismatch до добавления production baselines

### Task 7: Add reviewed representative visual baselines

**Files:**

- Create: `configurator-web/e2e/visual/application.visual.spec.ts`
- Create: `configurator-web/e2e/__screenshots__/*.png`
- Modify: `docs/testing/FRONTEND_TESTING.md`

- [x] добавить desktop light snapshots для AppShell/configurator/catalog/settings/graph
- [x] добавить mobile dark snapshots для navigation/catalog/details/configurations
- [x] добавить scoped overlay snapshot для destructive modal или explanation Drawer
- [x] исключить non-determinism без broad masks/tolerances и повторно получить идентичные baselines
- [x] проверить, что intentional UI change создаёт readable diff artifact
- [x] прогнать visual suite в pinned Docker environment до zero diffs

### Task 8: Add frontend quality gates to GitHub CI

**Files:**

- Modify: `.github/workflows/ci.yml`
- Modify: `.github/dependabot.yml`
- Modify: `configurator-web/vite.config.ts`
- Modify: `.github/PULL_REQUEST_TEMPLATE.md`

- [x] добавить frontend-quality job с Node/npm verification, npm ci, check and coverage thresholds
- [x] добавить dependent frontend-browser job в pinned Playwright image для functional/axe/visual suites
- [x] сохранить minimal permissions, one CI worker and pinned action SHAs
- [x] загружать separated reports/traces/diffs только при failure с retention 7 days
- [x] добавить npm Dependabot для `/configurator-web` и frontend checklist в PR template
- [x] проверить workflow formatting/security rules и локально прогнать все команды job

### Task 9: Verify acceptance criteria and document the workflow

**Files:**

- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `AGENTS.md`
- Modify: `docs/testing/FRONTEND_TESTING.md`
- Modify: `docs/accessibility/WCAG_2_2_AA_AUDIT.md`
- Move: `docs/plans/20260823-e2e-accessibility-visual-regression.md` to `docs/plans/completed/`

- [x] документировать local functional/axe/visual commands и controlled baseline review/update
- [x] обновить AI/PR Definition of Done без требования browser download во время `npm ci`
- [x] выполнить `npm ci`, api check, full check and coverage thresholds
- [x] выполнить functional E2E во всех трёх browsers, accessibility zero-violation suite and visual zero-diff suite
- [x] проверить package/workflow drift, generated files, binary baseline inventory and `git diff --check`
- [x] зафиксировать фактические test counts, coverage, CI-local limitation and remaining manual WCAG checks

## Non-Goals

- Формальная внешняя WCAG certification или замена ручного VoiceOver/zoom/text-spacing testing.
- Screenshot каждого route/state/browser/OS; visual coverage остаётся representative и reviewable.
- Hosted visual SaaS, cloud browser grid или credentials/secrets.
- Browser testing реального backend/MinIO/PostgreSQL: frontend E2E продолжает проверять HTTP boundary через deterministic
  mocks; end-to-end delivery появится вместе с 9.28–9.30.
- Performance/load tests и Core Web Vitals budget.
- Runtime authentication/authorization и auth accessibility.
- OpenAPI/backend/БД/generated client changes.

## Risks and Mitigations

- **Flaky screenshots:** одинаковый pinned Linux image, fixed environment/data, fonts ready, animations disabled, strict
  reviewed baselines; не расширять tolerance как быстрый workaround.
- **Разрастание snapshots:** representative inventory и PNG; новый baseline требует обоснованного layout/state coverage.
- **Долгий CI:** functional three-browser matrix выполняется один раз, axe только Chromium desktop/mobile, visual только
  Chromium; browser job идёт после fast frontend quality.
- **Axe false sense of security:** audit matrix продолжает разделять automated/manual evidence; docs явно повторяют
  ограничение automated detection.
- **Known violations pressure:** не создавать blanket allowlist; исправлять source. Third-party false positive требует
  точного documented exception.
- **Fixture becomes a second backend:** mock реализует только transport behavior, нужный UI journeys; request payloads и
  responses остаются typed, API drift проверяется generated client check.
- **CI artifacts may leak data:** suite использует только deterministic fake data; reports загружаются лишь при failure и
  хранятся 7 дней.
- **User changes:** `testcontainers.properties` остаётся unstaged и не участвует ни в одном frontend command/commit.

## Decision Gate Before Implementation

Перед созданием `feature/CON1-126` и изменением test toolchain нужно подтвердить:

1. functional E2E остаются в Chromium/Firefox/WebKit, axe запускается в Chromium desktop/mobile, visual snapshots —
   только в pinned Linux Chromium;
2. visual baselines обновляются через Docker-команду в одинаковом environment на Windows/macOS и коммитятся как PNG;
3. axe gate не имеет общего allowlist/exclusions: найденные нарушения исправляются, third-party false positive требует
   точечного документированного исключения;
4. текущий `smoke.spec.ts` разделяется на предметные specs, а missing critical journeys добавляются в 9.27;
5. GitHub CI получает два frontend jobs и npm Dependabot; runtime/backend/OpenAPI при отсутствии найденных defects не
   меняются.

## Post-Completion

После push/PR необходимо дождаться фактического GitHub Actions результата для обоих новых frontend jobs. Локальный
workflow audit не доказывает работоспособность GitHub runner/container/artifact permissions.

Ручной WCAG checklist (VoiceOver, 200% browser zoom, text spacing, полный keyboard walkthrough и meaningful content
review) остаётся отдельным human verification и не заменяется успешным axe gate.

## Final Verification — 2026-08-23

- `npm ci` — выполнен успешно, exact dependencies из lockfile установлены.
- `npm run check` — выполнен успешно: generated API client без drift, Prettier, ESLint, Stylelint, 207 unit tests,
  TypeScript typecheck и production build.
- `npm run test:coverage` — выполнен успешно: statements 90,25%, branches 83,94%, functions 88,79%, lines 90,84%;
  все установленные thresholds выполнены.
- `npm run test:e2e` — 69/69 tests в Chromium, Firefox и WebKit.
- `npm run test:accessibility` — 34/34 desktop/mobile checks, zero automatically detected violations выбранного
  WCAG A/AA ruleset; positive harness test подтверждает способность gate обнаруживать нарушение.
- `npm run test:visual` — 7/7 scenarios и 12 reviewed PNG baselines в pinned
  `mcr.microsoft.com/playwright:v1.62.1-noble`, zero diffs.
- Visual negative self-test выполнен во время разработки: намеренный сдвиг UI на 12 px корректно создал failure и
  readable expected/actual/diff artifacts с 20 601 отличающимся pixel; временное изменение удалено.
- OpenAPI, backend, Flyway, jOOQ, БД и generated API client не изменялись.
- Workflow/action/container pins, npm Dependabot, committed PNG inventory и отсутствие generated drift проверены
  локально; окончательный результат GitHub Actions остаётся проверить после push/PR.
- Ручные VoiceOver, 200% zoom, text spacing, полный keyboard walkthrough и semantic content review остаются human
  verification согласно `docs/accessibility/WCAG_2_2_AA_AUDIT.md`.
