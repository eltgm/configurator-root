# CON1-122 — Create and List Saved Configurations UI

## Overview

Пункт 9.23 завершает первый пользовательский цикл конфигуратора: напрямую совместимый локальный черновик можно
сохранить на сервере с названием и необязательным описанием, после чего он появляется в отдельном постраничном списке
конфигураций выбранной предметной области.

Backend-контракт уже реализован и остаётся источником истины. `POST /domains/{id}/configurations` атомарно повторно
проверяет состав перед сохранением; `GET /domains/{id}/configurations` возвращает принадлежащие текущему пользователю
конфигурации в порядке `createdAt DESC, id DESC`. Frontend-проверка управляет доступностью действия и даёт быстрый
feedback, но не заменяет серверную валидацию.

Реализация планируется frontend-only. OpenAPI, backend, Flyway, jOOQ, БД и generated API client изменять не требуется.

## Context (from discovery)

- **Backend create contract:** непустой состав до 50 компонентов; положительные уникальные IDs; только активные
  компоненты выбранной области; не более одного компонента каждого типа; каждая пара совместима напрямую через ручную
  связь или включённое автоматическое правило. Transitive-only состав получает `409 CONFIGURATION_CONFLICT`.
- **Metadata:** `name` после trim обязателен и ограничен 255 символами; nullable `description` ограничен 4000 символами,
  blank нормализуется backend в `null`; одинаковые названия конфигураций разрешены.
- **Backend list contract:** zero-based `page`, `size` от 1 до 100, default 10. Каждый item уже является полной
  `Configuration` с ordered `components`, поэтому list UI не требует detail N+1.
- **Archived state:** создать конфигурацию с архивным компонентом нельзя, но компонент, архивированный после сохранения,
  остаётся в list response с `archived=true` и должен быть виден пользователю.
- **Frontend 9.20–9.22:** `/configurator` хранит domain-scoped local draft, гидратирует компоненты и различает состояния
  `empty/pending/valid/transitive/conflict/blocked/error`. Только `valid` и непустой состав соответствуют требованиям
  сохранения; один активный компонент является допустимой неполной конфигурацией.
- **Транзитивный режим:** включённый toggle не запрещает сохранение сам по себе. Если итоговая сборка всё равно
  классифицирована как direct, её можно сохранить; transitive state всегда блокирует сохранение.
- **Frontend routes:** `/configurations` пока является placeholder. Маршрут detail появится в 9.24, а copy/export/delete
  — в 9.25.
- **Существующие паттерны:** TanStack Query domain-scoped keys, generated SDK через `src/shared/api`, React Hook Form +
  Zod, `getFieldErrors`, глобальная нормализация ошибок, explicit success notifications, shared loading/error/empty UI.
- **Безопасность:** OpenAPI описывает owner/authentication, но runtime использует временного системного пользователя.
  UI 9.23 не добавляет фиктивную авторизацию и не описывается как production-ready.
- **Локальное состояние:** пользовательское изменение
  `configurator-integration-tests/src/test/resources/testcontainers.properties` должно остаться незатронутым и не
  попадать в commit.

## Proposed Product Contract

1. Сохранение запускается только из текущей сборки на `/configurator`; отдельный повторный выбор компонентов в форме
   создания не добавляется.
2. Действие «Сохранить конфигурацию» показывается для непустого черновика и доступно только в состоянии прямой
   совместимости. Pending, transitive, conflict, blocked и request-error показывают понятную причину недоступности.
3. Один активный компонент можно сохранить: backend прямо допускает неполную, но непустую конфигурацию.
4. Нажатие открывает modal с обязательным названием и необязательным описанием. Состав показывается как read-only
   summary и отправляется в текущем draft order; backend response самостоятельно возвращает канонический порядок.
5. Клиент валидирует trimmed name `1..255` и description до 4000 символов. Blank description не отправляется либо
   отправляется как `undefined`, что соответствует server normalization в `null`.
6. Во время POST форма и закрытие modal блокируются. Mutation автоматически не повторяется.
7. Structured backend details для `name` и `description` привязываются к полям. Ошибка состава, concurrent archive,
   изменение rules или другой `409` показываются общей нормализованной ошибкой; modal, metadata и local draft
   сохраняются для исправления и повторной попытки.
8. После успешного `201` рекомендуется очистить локальный черновик выбранной области, показать success notification и
   перейти на первую страницу `/configurations`, где новая запись будет первой после refetch.
9. `/configurations` показывает cards сохранённых конфигураций: name, description, formatted creation date, количество
   компонентов и ordered краткий состав `type · name · brand`; архивный компонент получает явный badge.
10. Карточка в 9.23 не притворяется detail/edit экраном и не показывает неработающие edit/copy/export/delete actions.
    Название компонента может вести на уже существующую карточку компонента.
11. List page использует server pagination по 10 элементов, не сортирует ответ повторно и сбрасывается на page 0 при
    смене предметной области.
12. Search, filters, cards/table preference и server-side name search не входят в 9.23: текущий endpoint предоставляет
    только pagination, а первый список должен оставаться простым и воспроизводимым.
13. Loading, background refresh, empty, error/retry и successful list states различаются. Empty state ведёт обратно в
    конфигуратор.
14. Domain ID входит во все query keys; успешная mutation инвалидирует только configuration list family сохранённой
    области и не смешивает данные разных областей.
15. UI поддерживает клавиатуру, focus management modal, live feedback и минимальную ширину 360 px без горизонтальной
    прокрутки.

## Considered Approaches

### A. Save modal inside Configurator + dedicated read-only list — selected

- Пользователь сохраняет уже собранный и проверенный draft, не повторяя выбор компонентов.
- Eligibility непосредственно использует существующее состояние 9.22.
- `/configurations` остаётся простым server-driven списком и подготавливает основу для detail route 9.24.
- Недостаток: create flow связан с configurator workspace, но это соответствует пользовательскому сценарию и не
  дублирует сложную assembly UI.

### B. Standalone `/configurations/new` page reading localStorage draft

- Даёт отдельный URL и больше места под metadata.
- Создаёт вторую orchestration boundary над тем же draft, требует повторной hydration/validation и усложняет stale
  state при переходах между страницами.
- Для двух полей metadata отдельный экран избыточен.

### C. Create configurations directly from the list with a new component picker

- Все операции с сохранёнными сущностями находятся в одном разделе.
- Дублирует почти весь конфигуратор, compatibility search, replacement и validation; повышает риск расхождения двух
  сборщиков.
- Не соответствует разделению эпика и не рассматривается.

## Development Approach

- **Testing approach:** regular — небольшая реализация, затем unit/component tests в рамках той же задачи; E2E после
  стабилизации пользовательского сценария.
- Выполнять задачи последовательно и полностью; релевантные тесты должны пройти до перехода к следующей задаче.
- Transport DTO не дублировать и generated code не редактировать вручную.
- Сохранить architecture/frontend boundaries из `AGENTS.md`: API в feature adapter, pure decisions в model, UI в
  feature components, page только orchestration.
- При изменении scope сразу актуализировать этот план и требования эпика.
- Не трогать unrelated пользовательские изменения.

## Testing Strategy

- **API hooks with MSW:** list query shape, zero-based pagination, domain/page/size keys, create request, success/error,
  targeted invalidation and disabled domain state.
- **Unit/model:** save eligibility for every assembly state, request normalization, name/description boundaries,
  component summary helpers and date-independent presentation data.
- **Form/component:** modal validation, pending close lock, field errors, server conflict preservation, direct/single
  save, transitive/conflict/error disabled states, success clear + navigation.
- **List page:** loading, background refresh, retryable error, empty, cards, descriptions, ordered components, archived
  badges, pagination and domain switch reset.
- **E2E:** create a directly compatible configuration, verify POST payload, redirect/list result and cleared draft;
  verify transitive-only draft cannot save; verify mobile 360/390 px layout in Chromium, Firefox and WebKit.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e`, `git diff --check`.
- Backend/external integration не требуются, если OpenAPI/backend/Docker contract действительно не изменятся.

## Solution Overview

```text
local configurator draft + hydration + compatibility state
                         |
                         +--> save eligibility (non-empty + DIRECT)
                                      |
                                      +--> metadata modal
                                      +--> POST /domains/{id}/configurations
                                                   |
                                                   +--> server strict revalidation
                                                   +--> clear local draft
                                                   +--> invalidate domain list
                                                   +--> navigate /configurations?page 0

/configurations --> GET domain page --> cards with embedded ordered components
```

`ConfiguratorWorkspace` остаётся владельцем local draft и вычисленного compatibility state. Он передаёт в
`CurrentAssembly` только eligibility/action и открывает `CreateConfigurationModal`. Modal владеет metadata form, но не
копирует состав в собственное состояние: POST получает стабильный component ID snapshot на момент открытия.

`features/configurations/api` предоставляет одну domain-scoped key family и create/list adapters. List page получает
готовую `ConfigurationPage`; server ordering и pagination не пересчитываются клиентом. После успешного create mutation
list family инвалидируется, а redirect приводит пользователя на page 0.

## Technical Details

- Query keys: `['domains', domainId, 'configurations', 'list', page, size]`; root используется для targeted
  invalidation.
- `useConfigurationsQuery` не запускается без выбранного domain. Page size первого варианта — 10.
- POST body формируется только из `{ name: trimmedName, description?, componentIds }` и generated
  `CreateConfigurationRequest`.
- Save eligibility является pure-функцией/явным derived state, а не повторным client-side compatibility алгоритмом.
- Для одного ready active component batch request не требуется, и save разрешён.
- Для двух и более компонентов save разрешён только при `validation.relation === 'direct'`; значение toggle само по
  себе не участвует в решении.
- Modal snapshot предотвращает неявное изменение отправляемого состава. UI за modal недоступен, а после закрытия новый
  open берёт актуальный draft.
- Неуспешный POST не очищает localStorage и не закрывает modal. Успешный POST вызывает существующий `draft.clear()`.
- Configuration cards используют только list response; изображения, attributes и component details дополнительно не
  загружаются.
- Archived badge отражает исторически сохранённый состав и не является ошибкой list page.
- Route `/configurations/:id` сознательно не добавляется до 9.24.

## What Goes Where

- `features/configurations/api` — generated SDK adapters, domain-scoped query keys/hooks и cache invalidation.
- `features/configurations/model` — metadata normalization, save eligibility и небольшие presentation helpers.
- `features/configurations/ui` — create modal и list/card presentation.
- `features/configurator/ui/CurrentAssembly.tsx` — доступное save action рядом с текущей сборкой.
- `pages/ConfiguratorPage.tsx` — create orchestration, clear and redirect after success.
- `pages/ConfigurationsPage.tsx` — domain/list/pagination orchestration.
- `app/router/routes.tsx` — замена placeholder реальной list page.
- `shared/i18n/resources.ts` — RU/EN strings.
- `docs/requirements/epic-9-frontend.md` — окончательные acceptance criteria 9.23.

## Implementation Steps

### Task 1: Finalize the 9.23 product contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260823-create-and-list-configurations-ui.md`

- [x] добавить подробный раздел 9.23 с save eligibility, form, post-success и list semantics
- [x] зафиксировать поведение local draft после success и после server error
- [x] зафиксировать pagination, archived components, responsive/accessibility и non-goals 9.24–9.25
- [x] сверить критерии со всеми существующими create/list schemas и statuses
- [x] проверить форматирование документации до следующей задачи

### Task 2: Add configuration list and create API adapters

**Files:**

- Create: `configurator-web/src/features/configurations/api/configurations.ts`
- Create: `configurator-web/src/features/configurations/api/configurations.test.tsx`

- [x] создать domain-scoped root/list keys с page и size
- [x] добавить paged GET query через generated SDK
- [x] добавить POST mutation без automatic retry
- [x] после success инвалидировать только list family исходной области
- [x] протестировать request shape, success/error, pagination keys, domain isolation и disabled state через MSW
- [x] запустить API tests — они должны пройти до Task 3

### Task 3: Build metadata form and save eligibility model

**Files:**

- Create: `configurator-web/src/features/configurations/model/configuration-create.ts`
- Create: `configurator-web/src/features/configurations/model/configuration-create.test.ts`
- Create: `configurator-web/src/features/configurations/ui/CreateConfigurationModal.tsx`
- Create: `configurator-web/src/features/configurations/ui/CreateConfigurationModal.test.tsx` or cover through workspace tests

- [x] определить pure save eligibility для empty/pending/direct/transitive/conflict/blocked/error states
- [x] нормализовать trimmed name, blank description и immutable component ID snapshot
- [x] реализовать React Hook Form + localized Zod constraints 255/4000
- [x] показать read-only count/summary состава и заблокировать close/submit during mutation
- [x] привязать structured `name/description` details и сохранить form state при server error
- [x] протестировать boundaries, normalization, field errors, pending and conflict/error flows
- [x] запустить model/form tests — они должны пройти до Task 4

### Task 4: Integrate server save into the configurator workspace

**Files:**

- Modify: `configurator-web/src/pages/ConfiguratorPage.tsx`
- Modify: `configurator-web/src/features/configurator/ui/CurrentAssembly.tsx`
- Modify: `configurator-web/src/features/configurator/ui/AssemblyCompatibilityStatus.tsx` if required
- Modify: `configurator-web/src/features/configurator/ui/configurator-workspace.module.css`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] показать save action для непустой сборки и корректно вычислить disabled state/reason
- [x] разрешить single-component и direct assembly независимо от transitive toggle
- [x] открыть modal со snapshot актуальных component IDs
- [x] при success показать notification, очистить domain draft и перейти на `/configurations`
- [x] при failure оставить draft, modal metadata и current workspace без изменений
- [x] проверить pending/transitive/conflict/blocked/error, retry and keyboard/modal flows
- [x] запустить workspace tests — они должны пройти до Task 5

### Task 5: Replace the configurations placeholder with a paged list

**Files:**

- Create: `configurator-web/src/pages/ConfigurationsPage.tsx`
- Create: `configurator-web/src/pages/configurations-page.module.css`
- Create: `configurator-web/src/features/configurations/ui/ConfigurationList.tsx`
- Modify: `configurator-web/src/app/router/routes.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Create: `configurator-web/src/features/configurations/ui/ConfigurationsPage.test.tsx`

- [x] заменить placeholder реальной domain-aware page
- [x] показать loading, background refresh, error/retry и empty states
- [x] отобразить server-ordered cards с metadata, component count and ordered composition
- [x] явно пометить archived components без detail N+1
- [x] добавить zero-based server pagination и reset page при domain switch
- [x] не показывать actions, зарезервированные для 9.24–9.25
- [x] протестировать all states, pagination, domain isolation, ordering and mobile-friendly markup
- [x] запустить page/component tests — они должны пройти до Task 6

### Task 6: Complete integration-level frontend scenarios

**Files:**

- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`
- Modify: `configurator-web/src/features/configurations/ui/ConfigurationsPage.test.tsx`

- [x] проверить create request из restored и нового direct draft
- [x] проверить single-component save
- [x] проверить blocked transitive/conflict/unavailable/API-error states
- [x] проверить успешный clear, list invalidation, redirect and newest item render
- [x] проверить server `400/409/5xx` с сохранением draft/form
- [x] проверить смену области во время list lifecycle
- [x] запустить все configurator/configurations feature tests до Task 7

### Task 7: Add end-to-end create and list coverage

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`

- [x] добавить deterministic configuration GET/POST mocks
- [x] сохранить direct assembly и проверить точный POST payload
- [x] проверить success notification, redirect, новый первый card и очищенный local draft
- [x] проверить, что transitive-only assembly не позволяет открыть create modal
- [x] проверить empty/list/pagination and archived component presentation
- [x] проверить keyboard flow и mobile 390 px без горизонтальной прокрутки
- [x] выполнить Chromium, Firefox и WebKit scenarios — все должны пройти

### Task 8: Verify acceptance criteria and repository boundaries

**Files:**

- Modify if required: `docs/plans/20260823-create-and-list-configurations-ui.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run check`
- [x] выполнить `npm run test:coverage` и сверить minimum 90% lines
- [x] выполнить `npm run test:e2e`
- [x] убедиться, что OpenAPI, backend, Flyway, jOOQ, БД и generated API client не изменены
- [x] проверить `git diff --check` и отсутствие unrelated `testcontainers.properties` в staged diff
- [x] перенести завершённый план в `docs/plans/completed/`

## Post-Completion

- Route просмотра/полного редактирования появится в 9.24; list cards 9.23 не должны создавать временный competing
  detail UI.
- Copy, JSON export and permanent delete остаются в 9.25.
- Runtime authentication/authorization остаётся общим release blocker и не реализуется в рамках 9.23.

## Completion

Реализация завершена 2026-08-23. OpenAPI, backend, Flyway, jOOQ и БД не изменялись. Полностью прошли `npm ci`,
`npm run check`, `npm run test:coverage` (90.93% lines) и последовательный полный
`npm run test:e2e -- --workers=1 --timeout=60000` (36/36 в Chromium, Firefox и WebKit). Ограничение Vitest workers и увеличенный timeout зафиксированы
для воспроизводимого запуска полного UI-suite на локальных компьютерах.
