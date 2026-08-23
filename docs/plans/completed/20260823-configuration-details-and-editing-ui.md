# CON1-123 — Configuration Details and Full Editing UI

## Overview

Пункт 9.24 добавляет завершённый цикл работы с уже сохранённой конфигурацией: пользователь открывает её отдельную
карточку, видит актуальный состав, переходит в режим редактирования и одним атомарным запросом полностью заменяет
название, описание и набор компонентов.

Backend-контракт уже реализован. `GET /configurations/{id}` возвращает принадлежащую текущему пользователю
конфигурацию вместе с упорядоченным составом, включая архивированные компоненты. `PUT /configurations/{id}` сохраняет
полную новую версию изменяемых данных, но оставляет неизменными область, владельца и дату создания. Frontend помогает
собрать корректный состав, однако backend остаётся окончательным источником истины и повторно выполняет строгую
проверку.

Рекомендуемый вариант — отдельные маршруты `/configurations/:configurationId` и
`/configurations/:configurationId/edit`, read-only detail и отдельный in-memory editor. Сохранённая конфигурация не
загружается в localStorage-черновик `/configurator`: это разные пользовательские сценарии с разным жизненным циклом.

Реализация планируется frontend-only. OpenAPI, backend, Flyway, jOOQ, БД и generated API client изменять не требуется.

> План подготовлен по локальным source-of-truth файлам без Spring MCP: Amplicode установлен, но IntelliJ IDEA и
> Amplicode MCP не были подключены к текущей сессии.

## Context (from discovery)

- **Получение:** `GET /configurations/{id}` доступен только владельцу. Отсутствующая или чужая конфигурация возвращает
  `404`. Архивированный после сохранения компонент остаётся в ответе с `archived=true`.
- **Полная замена:** `PUT /configurations/{id}` принимает полный `UpdateConfigurationRequest`; отсутствующее поле
  состава не означает «не менять». `domainId`, owner и `createdAt` клиент не отправляет и изменить не может.
- **Metadata:** имя после trim обязательно, максимум 255 символов; description nullable, максимум 4000 символов;
  blank description нормализуется backend в `null`.
- **Состав:** от 1 до 50 уникальных положительных component IDs; все компоненты активны, принадлежат области
  конфигурации и имеют разные типы; каждая пара должна быть совместима напрямую через ручную связь или включённое
  автоматическое правило.
- **Транзитивность:** transitive-only совместимость недостаточна для сохранения. Редактор 9.24 работает только в
  строгом direct-режиме и не показывает транзитивный переключатель.
- **Архивный состав:** detail показывает исторический состав, но PUT отвергает архивный компонент даже тогда, когда он
  уже входит в конфигурацию. Для сохранения пользователь обязан удалить или заменить все архивные позиции.
- **Атомарность:** при любой ошибке update исходная конфигурация остаётся неизменной. Оптимистическая блокировка,
  version/ETag и merge отсутствуют; действует last-write-wins.
- **Ошибки:** missing/foreign configuration — `404`; отсутствующий component — `404`; некорректный input/чужая область
  — `400`; archive, повтор типа или несовместимость — `409`. Structured details для `name` и `description` можно
  привязать к полям, ошибки состава показываются на уровне редактора.
- **Порядок:** сервер возвращает компоненты в каноническом порядке типов. Пользовательское изменение порядка и drag
  and drop в 9.24 не требуются.
- **Frontend 9.23:** `/configurations` уже загружает страницы по 10 карточек без detail N+1. Карточки пока не содержат
  действий; название конфигурации станет ссылкой на detail в 9.24.
- **Повторное использование:** `AvailableComponentBrowser`, direct/intersection hooks и pure compatibility model можно
  использовать повторно. `useConfiguratorDraft` и `CurrentAssembly` привязаны к localStorage-черновику создания и для
  редактирования сохранённой сущности не подходят.
- **Query isolation:** все configuration query keys должны включать `domainId`, даже если detail endpoint принимает
  только configuration ID.
- **Безопасность:** OpenAPI содержит Bearer scheme, но runtime-аутентификация пока не реализована. UI не должен
  создавать ложное ощущение production-ready авторизации.
- **Локальные изменения:** пользовательский файл
  `configurator-integration-tests/src/test/resources/testcontainers.properties` нельзя изменять или включать в commit.

## Proposed Product Contract

1. Название конфигурации в списке и явное действие «Открыть» ведут на `/configurations/:configurationId`.
2. Detail page показывает название, описание, дату создания, область, число компонентов и серверный ordered состав:
   тип, название, бренд, ссылку на компонент и заметный archived badge.
3. Detail различает loading, background refresh, retryable error, invalid ID и not-found. Чужая конфигурация не
   раскрывается и выглядит как отсутствующая в соответствии с backend `404`.
4. На detail доступны «К списку» и «Редактировать». Copy, export и permanent delete остаются задачей 9.25 и не
   показываются как неработающие действия.
5. Edit page загружает конфигурацию и типы её области, затем создаёт отдельный in-memory draft. LocalStorage-черновик
   `/configurator` не читается, не изменяется и не очищается.
6. Редактор позволяет изменить trimmed name, nullable description и весь состав: добавить, удалить или явно заменить
   компонент. Состав не может быть пустым или превышать 50 позиций; на каждый тип допускается одна позиция.
7. Кандидаты подбираются только в direct-режиме. Для пустой основы используется активный каталог, для одной базовой
   позиции — direct endpoint, для двух и более — intersection. При замене текущая позиция исключается из основы.
8. После каждого изменения состава для двух и более компонентов выполняется batch-проверка в strict direct-режиме.
   Pending, request error, conflict, архивная позиция и неверный размер блокируют PUT с понятным объяснением.
9. Один активный компонент является допустимой конфигурацией и не требует batch-запроса. Backend всё равно повторяет
   все проверки при PUT.
10. Архивная позиция остаётся видимой и помеченной в редакторе. Сохранение блокируется, пока пользователь не удалит
    или не заменит все архивные позиции; metadata-only update при таком составе невозможен из-за server contract.
11. Submit отправляет полный `{ name, description?, componentIds }` snapshot. Blank description не отправляется;
    mutation автоматически не повторяется.
12. Во время PUT повторный submit блокируется. При server error форма и draft сохраняются, field details отображаются
    у `name/description`, а conflict состава — общей ошибкой рядом со сборкой.
13. После успешного PUT UI обновляет detail cache ответом сервера, инвалидирует list family только текущей области,
    показывает success notification и переходит на detail page.
14. Несохранёнными считаются изменения metadata или множества component IDs; различие только серверного порядка не
    делает форму dirty. Навигация, reload и смена предметной области не должны молча терять изменения.
15. Поскольку domain ID не входит в detail URL, открытие deep link при другой выбранной области показывает безопасное
    состояние несоответствия с явным действием «Переключиться на область конфигурации». Автоматическое неожиданное
    переключение не выполняется.
16. При явной смене области из read-only detail пользователь переходит к списку новой области. В edit режиме смена
    области проходит через общий guard несохранённых изменений; после подтверждения открывается список новой области.
17. Detail и editor доступны с клавиатуры, объявляют loading/error/validation состояния и не создают горизонтальную
    прокрутку начиная с 360 px.

## Considered Approaches

### A. Separate detail/edit routes + dedicated in-memory editor — recommended

- Соответствует уже применённому паттерну компонентов: read-only карточка и отдельный edit route.
- Даёт стабильные deep links и естественную семантику browser Back/Refresh.
- Изолирует сохранённую сущность от localStorage draft основного конфигуратора.
- Позволяет повторно использовать низкоуровневые candidate browser и compatibility hooks без копирования всего
  workspace.
- Требует отдельного небольшого assembly editor, но его состояние и границы остаются явными.

### B. Inline edit mode on the detail route

- Уменьшает число маршрутов и страниц.
- Смешивает query, view и mutable draft в одном компоненте; усложняет отмену, refresh, focus restoration и guard.
- Не соответствует существующей навигационной модели CRUD-страниц проекта.

### C. Load a saved configuration into `/configurator`

- Максимально повторно использует основной workspace.
- Смешивает server entity и domain-scoped local create draft, требует неочевидных правил overwrite/clear и может
  уничтожить незавершённую пользовательскую сборку.
- Создаёт риск случайного POST вместо PUT и не рекомендуется.

## Development Approach

- **Testing approach:** regular — реализация небольшими вертикальными частями с unit/component tests в каждой задаче;
  E2E после стабилизации полного view/edit сценария.
- Перед реализацией создать ветку `feature/CON1-123` от актуального `develop` и повторно проверить unrelated changes.
- Сначала актуализировать acceptance criteria 9.24, затем API/model, detail, editor и integration с router/domain
  selector.
- Generated client и transport DTO не редактировать и не дублировать; использовать exports из `src/shared/api`.
- Page components выполняют orchestration; request/cache logic находится в `features/configurations/api`, pure draft
  и validation decisions — в `model`, presentation — в `ui`.
- Сложную strict compatibility orchestration не копировать из `ConfiguratorWorkspace`: вынести или повторно
  использовать общие pure helpers там, где это не меняет поведение 9.20–9.23.
- Не выполнять optimistic update до ответа PUT: атомарный server response становится новым baseline формы.
- Не трогать OpenAPI/backend/schema без отдельного обнаруженного contract gap и предварительного согласования.

## Testing Strategy

- **API/MSW:** detail request, update body, domain-scoped keys, disabled/invalid IDs, cache population, targeted list
  invalidation, no mutation retry, `400/404/409` preservation.
- **Unit/model:** initial draft, trimmed metadata, blank description, dirty comparison independent of order, add/remove,
  explicit replacement, one-per-type, size `1..50`, archived blocker, single/direct/pending/conflict/error eligibility.
- **Detail component:** loading, refresh, retry, invalid/not-found, metadata, ordered composition, archived badge, component
  links, edit/back actions and domain mismatch state.
- **Editor component:** hydration, field validation, active single save, strict direct add/intersection/replace, archived
  remediation, empty/max/type conflicts, batch pending/error/conflict, server field/conflict errors, cancel and success.
- **Navigation:** route guard, `beforeunload`, domain switch confirmation, success transition to detail and list cache
  refresh.
- **Responsive/accessibility:** keyboard selection/replacement, focus after remove/error, live validation status and
  layouts at 360/390 px.
- **E2E:** open saved configuration; edit metadata and replace a component; verify full PUT and updated detail/list;
  reject transitive-only or archived composition; cancel unsaved navigation; exercise mobile layout in Chromium,
  Firefox and WebKit.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e`, `git diff --check`.
- Backend and external integration tests не требуются, пока OpenAPI/backend/Docker contract не меняются.

## Solution Overview

```text
/configurations
      |
      +--> /configurations/:id
                    |
                    +--> GET configuration (domain-scoped cache key)
                    +--> read-only metadata + ordered composition
                    |
                    +--> /configurations/:id/edit
                                  |
                                  +--> in-memory metadata + component draft
                                  +--> strict candidate search
                                  +--> strict batch validation
                                  +--> PUT complete replacement
                                              |
                                              +--> set detail cache
                                              +--> invalidate domain list
                                              +--> navigate back to detail
```

Detail query использует ключ `['domains', domainId, 'configurations', 'detail', configurationId]`. Ответ с другим
`domainId` не показывается внутри текущего контекста без явного переключения области. Это сохраняет domain isolation и
делает deep-link поведение понятным.

Edit page создаёт baseline из GET response и mutable draft в памяти. Metadata управляется React Hook Form + Zod;
состав — отдельной моделью, поскольку это коллекция с replace/remove semantics. Dirty state объединяет
`formState.isDirty` и сравнение множеств component IDs с baseline.

Для подбора переиспользуется `AvailableComponentBrowser` с `includeTransitive=false`. После изменения состава
существующий batch endpoint подтверждает весь результат, а PUT выполняет окончательную атомарную проверку. Таким
образом candidate filtering остаётся удобством UI, а не единственным механизмом обеспечения корректности.

## Technical Details

- Query keys:
  `['domains', domainId, 'configurations', 'detail', configurationId]` и существующая list family под тем же domain
  root.
- `useConfigurationQuery` активен только при валидных положительных `domainId` и `configurationId`.
- `useUpdateConfigurationMutation` принимает `{ domainId, configurationId, body }`; success записывает response в
  текущий detail key и инвалидирует `configurationKeys.byDomain(domainId)` с фильтром list queries.
- PUT body формируется из generated `UpdateConfigurationRequest`; manual transport interfaces не создаются.
- Initial component summaries берутся из `Configuration.components`; detail N+1 не нужен. Новые кандидаты приводятся к
  общей editor selection model с `id`, `componentTypeId`, `name`, `brand` и type name.
- Component IDs сравниваются как множества для dirty state, но текущий UI сохраняет детерминированный display order.
- При replacement target его ID исключается из `baseComponentIds`; тип replacement фиксируется типом заменяемой
  позиции.
- `includeTransitive` всегда `false`. Transitive-only кандидат не должен попадать в доступный выбор.
- Archived component не используется как допустимая база для обычного добавления. Remove остаётся доступен; replace
  строится относительно оставшихся активных компонентов.
- После любого изменения двух и более позиций strict batch state определяет client eligibility. Состояние single active
  валидно без batch; empty всегда невалидно.
- Update success вызывает `form.reset(response metadata)` и заменяет composition baseline server response до
  navigation, чтобы guard не перехватил программный переход.
- Для domain change guard рекомендуется расширить существующий механизм несохранённых изменений общим регистрационным
  контекстом, который сможет использовать `DomainSelector`; route/blocker и `beforeunload` сохраняются.
- 9.24 не добавляет copy, export, delete, reordering, optimistic concurrency, autosave и восстановление edit draft
  после закрытия вкладки.

## What Goes Where

- `features/configurations/api/configurations.ts` — detail/update adapters, keys и cache invalidation.
- `features/configurations/model/configuration-editor.ts` — draft normalization, immutable composition operations,
  dirty и save eligibility.
- `features/configurations/ui/ConfigurationDetails.tsx` — read-only metadata/composition.
- `features/configurations/ui/ConfigurationEditor.tsx` — form, assembly controls, strict validation и submit state.
- `features/configurator/ui/AvailableComponentBrowser.tsx` и compatibility model — только согласованное обобщение для
  повторного использования, без изменения существующего поведения конфигуратора.
- `pages/ConfigurationDetailsPage.tsx` — params, query, domain mismatch and detail orchestration.
- `pages/ConfigurationEditPage.tsx` — query/type loading, editor orchestration and navigation.
- `app/router/routes.tsx` — два новых route objects.
- `features/domains` / shared navigation guard — подтверждаемая защита domain change при dirty edit.
- `shared/i18n/resources.ts` — RU/EN строки detail/editor/errors/notifications.
- `docs/requirements/epic-9-frontend.md` — окончательные acceptance criteria 9.24.

## Implementation Steps

### Task 1: Finalize the 9.24 product contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260823-configuration-details-and-editing-ui.md`

- [x] зафиксировать detail, full replacement, strict direct, archived remediation и non-goals 9.25
- [x] подтвердить отдельные view/edit routes, explicit domain mismatch и общий domain-change guard
- [x] сверить критерии со всеми GET/PUT schemas, statuses и backend integration contracts
- [x] проверить форматирование документации до изменения runtime-кода

### Task 2: Add detail and update API adapters

**Files:**

- Modify: `configurator-web/src/features/configurations/api/configurations.ts`
- Modify: `configurator-web/src/features/configurations/api/configurations.test.tsx`

- [x] добавить domain-scoped detail key и GET adapter
- [x] добавить full PUT mutation без automatic retry
- [x] на success записать detail response и инвалидировать только list queries соответствующей области
- [x] протестировать request/body, keys, disabled state, cache and `400/404/409`
- [x] запустить API tests — они должны пройти до Task 3

### Task 3: Build the configuration editor model

**Files:**

- Create: `configurator-web/src/features/configurations/model/configuration-editor.ts`
- Create: `configurator-web/src/features/configurations/model/configuration-editor.test.ts`
- Modify common configurator model only if a pure helper can be safely shared

- [x] создать baseline/draft normalization из `Configuration`
- [x] реализовать add/remove/replace с one-per-type и deterministic order
- [x] определить dirty comparison независимо от server/display order
- [x] определить strict save eligibility для empty/single/direct/pending/conflict/error/archived/max states
- [x] нормализовать metadata и полный `UpdateConfigurationRequest`
- [x] покрыть model boundaries и immutable operations тестами
- [x] запустить model tests — они должны пройти до Task 4

### Task 4: Implement the read-only configuration detail

**Files:**

- Create: `configurator-web/src/features/configurations/ui/ConfigurationDetails.tsx`
- Create: `configurator-web/src/pages/ConfigurationDetailsPage.tsx`
- Modify: `configurator-web/src/features/configurations/ui/ConfigurationList.tsx`
- Modify: `configurator-web/src/app/router/routes.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Create/modify relevant component tests

- [x] сделать title/action в list доступной ссылкой на detail
- [x] показать metadata и ordered component composition без N+1
- [x] показать archived badges и component links
- [x] обработать loading/refresh/error/invalid/not-found/domain mismatch states
- [x] добавить back/edit navigation и не показывать 9.25 actions
- [x] проверить keyboard and 360 px layout
- [x] запустить detail/list tests — они должны пройти до Task 5

### Task 5: Implement strict in-memory assembly editing

**Files:**

- Create: `configurator-web/src/features/configurations/ui/ConfigurationEditor.tsx`
- Create: `configurator-web/src/features/configurations/ui/ConfigurationAssemblyEditor.tsx`
- Create: `configurator-web/src/pages/ConfigurationEditPage.tsx`
- Modify reusable configurator UI/model only where required
- Modify: `configurator-web/src/app/router/routes.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Create/modify relevant component tests

- [x] гидратировать metadata и composition только после успешного detail GET
- [x] реализовать add/remove/explicit replacement без localStorage и drag and drop
- [x] переиспользовать candidate browser в direct-only режиме
- [x] запускать strict batch validation для двух и более компонентов
- [x] блокировать сохранение при archived/pending/conflict/error/empty/max states с понятной причиной
- [x] покрыть single, direct, intersection, replacement and archived remediation tests
- [x] запустить editor tests — они должны пройти до Task 6

### Task 6: Wire full update, errors and unsaved-change protection

**Files:**

- Modify: `configurator-web/src/features/configurations/ui/ConfigurationEditor.tsx`
- Modify/create: shared unsaved changes guard files
- Modify: `configurator-web/src/features/domains/ui/DomainSelector.tsx`
- Modify: `configurator-web/src/features/domains/model/DomainProvider.tsx` only if required by the selected guard design
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Create/modify navigation and mutation tests

- [x] отправлять полный immutable snapshot через PUT
- [x] привязать backend field details и сохранить draft при любой ошибке
- [x] после success reset baseline, notify, update cache/invalidate list and navigate detail
- [x] защитить route, reload и domain change при dirty edit
- [x] после подтверждённой смены области перейти на её `/configurations`
- [x] проверить cancel/confirm, pending submit and success navigation tests
- [x] запустить configuration/domain navigation tests — они должны пройти до Task 7

### Task 7: Add end-to-end coverage and responsive polish

**Files:**

- Modify: `configurator-web/e2e/configurations.spec.ts` or the existing configuration E2E suite
- Modify: MSW/test fixtures only as needed
- Modify: configuration page/editor CSS modules

- [x] покрыть detail and successful full update with replacement
- [x] покрыть archived/transitive conflict and preserved form state
- [x] покрыть unsaved navigation and explicit domain switch
- [x] проверить desktop and 360/390 px flows во всех Playwright projects
- [x] устранить accessibility/responsive regressions
- [x] запустить targeted E2E before the full verification

### Task 8: Complete verification and documentation

**Files:**

- Modify: `docs/plans/20260823-configuration-details-and-editing-ui.md`
- Move when complete: `docs/plans/completed/20260823-configuration-details-and-editing-ui.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run check`
- [x] выполнить `npm run test:coverage`
- [x] выполнить `npm run test:e2e`
- [x] выполнить `git diff --check` и проверить scope commit
- [x] подтвердить, что OpenAPI/backend/DB/generated code не изменились
- [x] не включать `testcontainers.properties` и другие unrelated files
- [x] заполнить Progress, Solution и реально выполненные проверки
- [x] перенести завершённый план в `docs/plans/completed/`

## Progress

- План подготовлен 2026-08-23 и подтверждён пользователем.
- Реализация завершена в рабочей ветке `feature/CON1-123`; требования 9.24 актуализированы.
- Добавлены domain-scoped detail/update API adapters, read-only detail и отдельный strict in-memory editor.
- Реализованы add/remove/explicit replacement, archived remediation, direct batch validation и полный PUT.
- Общий domain-change guard защищает dirty editor и возвращает пользователя к списку выбранной области.
- `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage` и полный Playwright suite выполнены успешно.
- OpenAPI: без изменений.
- БД/Flyway/jOOQ: без изменений.
- Generated frontend client: без изменений.
- Unrelated `configurator-integration-tests/src/test/resources/testcontainers.properties`: сохранён без изменений.

## Solution

Реализованы маршруты `/configurations/:configurationId` и `/configurations/:configurationId/edit`. Список теперь
содержит явные ссылки на detail; карточка detail показывает metadata, ordered состав, component links и archived state,
обрабатывает invalid/not-found/error/background refresh и требует явного переключения области для cross-domain deep
link.

Редактор хранит отдельный in-memory draft, не затрагивая localStorage основного конфигуратора. Metadata управляется
React Hook Form + Zod, состав — чистой immutable model. Для подбора повторно используется
`AvailableComponentBrowser` только с `includeTransitive=false`; для двух и более активных компонентов весь draft
проверяется существующим batch endpoint. Empty, archived, pending, transitive, conflict, error и limit состояния
блокируют сохранение. Успешный PUT записывает server response в detail cache, инвалидирует только list family текущей
области, показывает notification и возвращает на detail.

Существующий route/beforeunload guard расширен общим регистрационным domain-change guard. При dirty edit выбор другой
области требует подтверждения, разрешает программную навигацию только после явного discard и открывает список новой
области.

Проверки:

- `npm ci` — успешно, 527 пакетов установлены из lockfile;
- `npm run check` — успешно: OpenAPI client up to date, Prettier, ESLint, Stylelint, 192/192 Vitest tests и production
  build;
- `npm run test:coverage` — успешно: 192/192, statements 90.46%, branches 84.01%, functions 89.72%, lines 91.06%;
- `npm run test:e2e -- --workers=1` — успешно: 36/36 в Chromium, Firefox и WebKit;
- `git diff --check` — успешно.

Backend и external integration tests не запускались: OpenAPI, backend, Docker delivery и runtime contract не
изменялись. Vite сохранил неблокирующее предупреждение о основном chunk больше 500 kB; code splitting остаётся общей
точкой улучшения frontend delivery, но не блокирует 9.24.
