# CON1-121 — Transitive Compatibility Mode and Explanations UI

## Overview

Пункт 9.22 добавляет в конфигуратор явно включаемый транзитивный режим и объясняет пользователю, почему два компонента
считаются совместимыми. По умолчанию подбор остаётся строгим и использует только прямые ручные связи и автоматические
правила. После включения режима сервер дополнительно возвращает компоненты, достижимые по графу совместимости, вместе
с одним детерминированным кратчайшим путём.

Транзитивная совместимость помогает продолжить исследование и собрать локальный черновик, но не делает его пригодным
для сохранения: backend создания и обновления конфигурации принимает только попарно напрямую совместимые компоненты.
UI должен показывать это различие явно и никогда не называть транзитивно совместимую сборку полностью готовой к
сохранению.

Реализация остаётся frontend-only и использует существующие OpenAPI-контракты. OpenAPI, backend, Flyway, jOOQ, БД и
generated API client изменять не требуется.

## Context (from discovery)

- **Основа 9.21:** текущая сборка проверяется batch search, один base использует direct endpoint, два и более base —
  server intersection. Все запросы пока жёстко отправляют `includeTransitive=false`.
- **Источники объяснений:** `CompatibilityExplanation.source` принимает `MANUAL`, `AUTOMATIC` или `TRANSITIVE`.
  Ручная причина содержит `linkId/comment`, автоматическая — `ruleSetId/ruleSetName/conditions`, транзитивная — ordered
  `pathComponentIds` длиной не менее трёх.
- **Серверная семантика:** при наличии прямой связи сервер возвращает все её ручные и автоматические причины. Причина
  `TRANSITIVE` возвращается только для кандидата без прямой связи; shortest path вычисляется сервером через BFS и уже
  детерминирован.
- **Пересечение:** один кандидат может быть напрямую совместим с одним base и транзитивно — с другим. Поэтому
  `compatibilityByBase` необходимо сохранить как отдельные группы evidence, а не объединять в один плоский массив.
- **Имена пути:** explanation содержит только IDs. `GET /domains/{id}/compatibility/graph` уже возвращает все активные
  компоненты как nodes с name/type/brand. Эти nodes можно лениво загрузить одним запросом при открытии транзитивного
  объяснения; detail N+1 не нужен. Рёбра graph response для построения пути не используются, потому что этот endpoint
  отражает только ручные связи, тогда как shortest path может включать автоматические.
- **Сохранение:** backend create/update configuration проверяет direct compatibility независимо от frontend.
  Серверное сохранение появится в 9.23, но модель 9.22 должна уже различать direct и transitive-only состояния.
- **Локальное состояние:** файл
  `configurator-integration-tests/src/test/resources/testcontainers.properties` изменён пользователем и не должен
  редактироваться, форматироваться, индексироваться или попадать в будущий commit.

## Proposed Product Contract

1. Переключатель «Учитывать транзитивную совместимость» находится в рабочем пространстве конфигуратора, по умолчанию
   выключен и влияет одновременно на проверку текущей сборки, direct search и intersection search.
2. В первом варианте значение переключателя является состоянием текущего открытого workspace: оно не сохраняется в
   `localStorage` и сбрасывается при reload или смене предметной области.
3. В выключенном режиме сохраняется поведение 9.21: показываются только прямые кандидаты, а transitive-only пара
   считается конфликтной.
4. Во включённом режиме сервер получает `includeTransitive=true`. Кандидат остаётся доступным, если он совместим с
   каждым base напрямую либо транзитивно; для пересечения доказательства показываются отдельно по каждому base.
5. Сборка получает три успешных содержательных состояния: один компонент/попарно напрямую совместима, попарно
   совместима с участием транзитивных путей, несовместима. Существующие pending/blocked/request-error состояния
   сохраняются.
6. Транзитивно совместимая сборка не блокирует дальнейший локальный подбор, но получает заметное предупреждение
   «нельзя сохранить, пока все пары не станут напрямую совместимыми». В 9.22 действие сохранения ещё не добавляется.
7. Если пользователь выключает режим для уже собранного transitive-only черновика, выполняется строгая повторная
   проверка; конфликтные позиции помечаются и обычный подбор блокируется по правилам 9.21.
8. Карточка кандидата показывает итог отношения: «Прямая совместимость» либо «Транзитивная совместимость», краткие
   badges доступных источников и действие «Почему совместим».
9. Объяснения открываются в доступной responsive Drawer: на desktop — боковая панель, на mobile — полноэкранное
   представление. Закрытие возвращает focus на вызвавшую кнопку.
10. Ручная причина показывает комментарий или нейтральное «Комментарий не добавлен». Служебный `linkId` может быть
    показан вторичным текстом, но не является основным объяснением.
11. Автоматическая причина показывает название rule set и все успешно выполненные AND-условия в серверном порядке:
    левый атрибут и значение, локализованный оператор, правый атрибут и значение.
12. Транзитивная причина показывает цепочку имён компонентов от base до candidate в порядке `pathComponentIds`.
    Пока node index загружается, отображается loading state; отсутствующий node не ломает цепочку и показывается как
    «Компонент #id».
13. Для текущей сборки доступно отдельное «Показать проверку»: пары перечисляются детерминированно в порядке draft,
    каждая помечается как direct/transitive/conflict и раскрывает свои evidence. Повторный запрос для объяснений не
    выполняется — используется уже полученный batch response.
14. UI не пересчитывает граф, BFS, automatic rules или intersection самостоятельно и не делает вывод о совместимости
    по содержимому атрибутов. Сервер остаётся единственным источником результата и shortest path.
15. Смена предметной области изолирует query cache, состояние Drawer и переключатель; устаревшие explanations другой
    области не показываются.
16. Все действия доступны с клавиатуры, состояния и изменение режима объявляются screen reader, а ширина 360 px не
    получает горизонтальную прокрутку.

## Considered Approaches

### A. One evidence-aware mode for all compatibility queries — selected

- Один `includeTransitive` передаётся в direct, batch и intersection queries и включается в каждый query key.
- Batch response одновременно валидирует сборку и предоставляет explanations; источник причины позволяет отличить
  direct от transitive-only пары без второго запроса.
- Серверные intersection groups сохраняются по base, поэтому UI честно показывает смешанный direct/transitive
  кандидат.
- Graph nodes загружаются только при открытии explanation с транзитивным путём.
- Недостаток: при переключении режима весь контекстный набор обновляется, поэтому UI должен явно показывать pending и
  не оставлять предыдущий результат как актуальный.

### B. Always request direct and transitive results in parallel

- Сравнение двух ответов делает классификацию очевидной.
- Удваивает дорогие batch/intersection вычисления, усложняет синхронизацию ошибок и создаёт race между режимами.
- Не требуется, потому что transitive response сохраняет direct explanations для непосредственных соседей.

### C. Load graph and calculate transitive compatibility in the browser

- Может мгновенно переключать режим после первой загрузки графа.
- Дублирует backend BFS, не имеет полного automatic graph contract и рискует разойтись с серверным порядком и
  shortest-path semantics.
- Нарушает server-as-source-of-truth и не рассматривается для реализации.

## Solution Overview

```text
workspace includeTransitive (default false)
                |
                +--> batch search ------> pair evidence ------> direct / transitive / conflict
                |
                +--> 0 bases -----------> active component catalog
                +--> 1 base ------------> compatible?includeTransitive=...
                +--> 2+ bases ----------> intersection(includeTransitive=...)
                                                   |
                                                   +--> evidence grouped by base
                                                   +--> candidate relation + source badges

open explanation containing TRANSITIVE
                |
                +--> lazy compatibility graph nodes --> path ID -> name/type index
```

`includeTransitive` живёт в `ConfiguratorWorkspace`, чтобы один режим применялся к текущей сборке и браузеру
кандидатов. Query keys включают режим, поэтому direct и transitive caches не смешиваются. Чистая model-функция строит
для каждой пары отношение `DIRECT | TRANSITIVE | INCOMPATIBLE`, evidence и IDs конфликтных компонентов; итог сборки
является максимальной строгостью её пар.

Кандидат хранит `compatibilityByBase`, даже если base только один. Это даёт UI одну структуру для direct и intersection
responses. Relation кандидата считается `TRANSITIVE`, если хотя бы для одного base evidence имеет только источник
`TRANSITIVE`; иначе она `DIRECT`. Source badges не заменяют relation: ручная и автоматическая причины являются видами
прямой совместимости.

## Technical Details

- Query key shape: `domainId + operation + ordered IDs + includeTransitive`.
- Read-only POST search/intersection остаются TanStack Query queries, а не mutations.
- Переключение режима не использует placeholder data предыдущего key, чтобы direct result не выглядел как актуальный
  transitive result и наоборот.
- Для надёжной pair validation сохраняется текущая двусторонняя проверка batch results. Пара считается direct, только
  если оба направления присутствуют как direct; если оба направления присутствуют, но хотя бы одно классифицировано
  как transitive, итог пары — transitive. Иначе это conflict.
- Evidence для отображения одной пары берётся из одного детерминированного направления draft order, чтобы не
  дублировать симметричные manual/automatic причины.
- `CompatibilityExplanation` не копируется в собственный transport DTO. Model может построить только UI-oriented
  discriminated view model поверх generated type.
- Operator labels переиспользуют существующие локализованные ключи `compatibilityRules.form.operators.*`.
- Graph query расширяется параметром `enabled`; existing graph page продолжает загружать данные немедленно, а
  configurator explanation включает запрос только после открытия transitive path.
- Path строится строго по `pathComponentIds`; graph edges не используются для дополнения или исправления server path.
- Drawer хранит выбранный explanation context в workspace/UI state и очищает его при смене domain или исчезновении
  кандидата.
- Empty state транзитивного поиска отдельно сообщает, что даже с учётом путей общего кандидата нет.
- Runtime-аутентификация, сохранение конфигурации, изменение compatibility rules и визуализация BFS не входят в 9.22.

## What Goes Where

- `features/configurator/api` — параметризованные compatibility query adapters и keys.
- `features/configurator/model` — нормализация per-base evidence, pair/assembly/candidate classification и explanation
  view models.
- `features/configurator/ui` — toggle, badges, assembly summary и responsive explanation Drawer.
- `features/compatibility/api` — переиспользуемый ленивый graph node query без изменения transport contract.
- `pages/ConfiguratorPage.tsx` — orchestration режима и открытого explanation context.
- `shared/i18n/resources.ts` — русские и английские строки, включая source/operator/path states.
- `docs/requirements/epic-9-frontend.md` — окончательные acceptance criteria 9.22.

## Testing Strategy

- **Unit/model:** нормализация direct/intersection evidence, смешанный кандидат по нескольким base, direct/transitive/
  conflict pair classification, assembly aggregation, deterministic order, missing reciprocal result и fallback names.
- **API hooks with MSW:** `includeTransitive=false/true` для single/batch/intersection, mode in query keys, refetch on
  toggle, domain isolation, disabled graph query и lazy node load.
- **Component/workspace:** default-off toggle, pending transition, transitive candidate, source badges, grouped evidence,
  manual comment, automatic conditions, shortest path, transitive assembly warning, toggle-off conflict and replacement.
- **Accessibility:** accessible switch name/state, focus return from Drawer, status announcements, keyboard open/close and
  semantic ordered path.
- **E2E:** включение режима, выбор transitive-only кандидата, просмотр mixed intersection explanation, shortest path,
  предупреждение о невозможности сохранения, выключение режима и восстановление строгого conflict state.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e` (Chromium, Firefox, WebKit), `git diff --check`.
- Backend/external integration не запускаются, если OpenAPI/backend/Docker contract действительно не изменятся.

## Implementation Steps

### Task 1: Finalize the 9.22 product contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260823-transitive-compatibility-explanations-ui.md`

- [x] добавить подробный раздел 9.22 с toggle, status semantics, explanations и saveability warning
- [x] зафиксировать default/persistence scope переключателя и поведение при его выключении
- [x] зафиксировать responsive/accessibility acceptance criteria и non-goals 9.23
- [x] сверить требования со single, batch, intersection и graph contracts

### Task 2: Parameterize compatibility queries and add lazy graph nodes

**Files:**

- Modify: `configurator-web/src/features/configurator/api/configurator-compatibility.ts`
- Modify: `configurator-web/src/features/configurator/api/configurator-compatibility.test.tsx`
- Modify: `configurator-web/src/features/compatibility/api/compatibility-graph.ts`
- Modify/create tests for the graph query if required

- [x] заменить hardcoded direct-only flag явным `includeTransitive` в fetch functions и hooks
- [x] включить mode в direct/batch/intersection query keys
- [x] проверить request body/query serialization для обоих режимов
- [x] добавить `enabled` в graph query без регрессии graph page
- [x] протестировать cache isolation, disabled states, toggle refetch и API errors через MSW

### Task 3: Build evidence-aware compatibility models

**Files:**

- Modify: `configurator-web/src/features/configurator/model/configurator-compatibility.ts`
- Modify: `configurator-web/src/features/configurator/model/configurator-compatibility.test.ts`

- [x] унифицировать direct/intersection candidates через ordered `compatibilityByBase`
- [x] добавить source и candidate relation classification без потери raw explanations
- [x] построить deterministic pair evidence и assembly result `DIRECT | TRANSITIVE | INCOMPATIBLE`
- [x] сохранить conflict pair/component IDs для существующего blocking flow
- [x] добавить explanation view model, operator formatting и path node fallback
- [x] покрыть mixed evidence, asymmetric/missing data, duplicate sources и empty conditions unit tests

### Task 4: Add the transitive mode control and assembly states

**Files:**

- Modify: `configurator-web/src/pages/ConfiguratorPage.tsx`
- Modify: `configurator-web/src/features/configurator/ui/AssemblyCompatibilityStatus.tsx`
- Modify: `configurator-web/src/features/configurator/ui/CurrentAssembly.tsx`
- Modify: `configurator-web/src/features/configurator/ui/configurator-workspace.module.css`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] добавить выключенный по умолчанию accessible switch с коротким пояснением
- [x] передавать один mode в batch validation и candidate browser
- [x] показать отдельный transitive assembly state и предупреждение о невозможности сохранения
- [x] не блокировать локальный подбор для валидной transitive assembly
- [x] при выключении режима строго перепроверить сборку и корректно перейти в conflict
- [x] сохранить pending/blocked/error/retry, live announcements и replacement semantics

### Task 5: Display candidate relation and explanations

**Files:**

- Modify: `configurator-web/src/features/configurator/ui/AvailableComponentBrowser.tsx`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorCandidateCard.tsx`
- Create: `configurator-web/src/features/configurator/ui/CompatibilityExplanationDrawer.tsx`
- Create if useful: focused explanation presentation components under `features/configurator/ui`
- Modify: `configurator-web/src/features/configurator/ui/configurator-workspace.module.css`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] переключать direct/intersection requests и descriptions по выбранному mode
- [x] показывать итоговый direct/transitive badge и badges источников без ложного объединения bases
- [x] открыть explanation Drawer с группировкой по каждому base
- [x] отобразить manual comment, automatic rule/conditions и localized operators
- [x] лениво разрешить shortest path IDs в имена через graph nodes и обработать loading/error/missing node
- [x] сохранить client filters/pagination, replacement and no-N+1 behavior

### Task 6: Explain current assembly pair by pair

**Files:**

- Modify: `configurator-web/src/features/configurator/ui/AssemblyCompatibilityStatus.tsx`
- Modify: `configurator-web/src/features/configurator/ui/CurrentAssembly.tsx`
- Reuse/modify: `configurator-web/src/features/configurator/ui/CompatibilityExplanationDrawer.tsx`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] добавить действие просмотра проверки только при наличии валидного batch result
- [x] перечислить пары в draft order со статусом direct/transitive/conflict
- [x] переиспользовать batch evidence без дополнительного compatibility request
- [x] дать доступ к тем же manual/automatic/transitive details и fallback states
- [x] проверить обновление/закрытие details после изменения draft, mode или domain

### Task 7: Add end-to-end transitive and explanation coverage

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`

- [x] добавить deterministic mocks обоих `includeTransitive` modes и graph nodes
- [x] проверить default direct mode и включение transitive candidate search
- [x] проверить mixed direct/transitive intersection evidence
- [x] проверить manual/automatic explanations и shortest path names
- [x] проверить transitive saveability warning и продолжение локальной сборки
- [x] проверить toggle-off conflict, retry, domain switch и mobile 360 px
- [x] выполнить сценарии в Chromium, Firefox и WebKit

### Task 8: Verify acceptance criteria and repository boundaries

**Files:**

- Modify if required: `docs/plans/20260823-transitive-compatibility-explanations-ui.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run check`
- [x] выполнить `npm run test:coverage` и сверить coverage threshold
- [x] выполнить `npm run test:e2e`
- [x] убедиться, что OpenAPI, backend, Flyway, jOOQ и generated API client не изменены
- [x] проверить `git diff --check` и отсутствие unrelated `testcontainers.properties` в staged diff

## Verification Results

- `npm run api:check` — generated frontend API client соответствует OpenAPI;
- `npm run check` — format, ESLint, Stylelint, 156 unit/component tests, TypeScript и production build прошли;
- `npm run test:coverage` — 156 тестов прошли, line coverage 90.65% при минимуме 90%;
- `npm run test:e2e` — 33 сценария прошли в Chromium, Firefox и WebKit;
- `git diff --check` — ошибок whitespace нет;
- OpenAPI, backend, Flyway, jOOQ, БД и generated API client не изменены;
- пользовательское изменение `configurator-integration-tests/src/test/resources/testcontainers.properties` сохранено
  отдельно и не входит в реализацию 9.22.
