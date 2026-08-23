# CON1-120 — Direct Compatibility Search and Intersection UI

## Overview

Пункт 9.21 превращает браузер компонентов конфигуратора из общего каталога в контекстный подбор: после выбора первого
компонента пользователь видит только напрямую совместимые варианты, а для двух и более выбранных компонентов —
пересечение их наборов совместимости. Текущая локальная сборка дополнительно проверяется сервером, чтобы черновики,
созданные в 9.20, не считались совместимыми без фактической валидации.

Реализация остаётся frontend-only и использует существующие OpenAPI-контракты:

- `GET /domains/{id}/configurator/compatible` для одного базового компонента;
- `POST /domains/{id}/configurator/compatible/search` для проверки текущего состава;
- `POST /domains/{id}/configurator/compatible/intersection` для двух и более базовых компонентов.

Транзитивный поиск и подробное отображение объяснений остаются в 9.22; во всех запросах 9.21 применяется
`includeTransitive=false`.

## Context (from discovery)

- **Основные файлы:**
  - `configurator-web/src/pages/ConfiguratorPage.tsx`;
  - `configurator-web/src/features/configurator/model/use-configurator-draft.ts`;
  - `configurator-web/src/features/configurator/ui/AvailableComponentBrowser.tsx`;
  - `configurator-web/src/features/configurator/ui/CurrentAssembly.tsx`;
  - `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`;
  - `configurator-web/src/shared/i18n/resources.ts`;
  - `configurator-web/e2e/smoke.spec.ts`;
  - `docs/requirements/epic-9-frontend.md`.
- **Существующие паттерны:** server state хранится в TanStack Query, generated SDK используется только через
  `src/shared/api`, query keys включают `domainId`, HTTP-ошибки нормализуются общим API layer, состояния запросов
  собираются из `shared/ui`.
- **Текущая основа 9.20:** локальный versioned draft на предметную область, не более одного компонента каждого типа,
  восстановление detail-данных, явные add/replace/remove/clear и двухколоночный responsive workspace.
- **Контракт backend:** direct и intersection responses уже сгруппированы в порядке типов; intersection исключает
  базовые компоненты и сохраняет evidence для каждого base; batch search возвращает независимый результат для каждого
  base в порядке запроса.
- **Ограничение DTO:** compatibility responses содержат short component data без изображений и полных атрибутов.
  Подгружать detail каждого кандидата отдельным запросом не планируется, чтобы не создавать N+1 запросов.
- **Локальное состояние:** файл
  `configurator-integration-tests/src/test/resources/testcontainers.properties` изменён пользователем и не должен
  редактироваться, форматироваться, индексироваться или попадать в будущий commit.

## Confirmed and Implicit Requirements

1. Пустая сборка продолжает показывать общий пагинированный каталог активных компонентов, чтобы первый компонент можно
   было выбрать свободно.
2. Для одного выбранного активного компонента браузер получает прямых кандидатов через single-component endpoint.
3. Для двух и более выбранных активных компонентов браузер получает серверное пересечение; клиент не повторяет
   алгоритм пересечения.
4. Выбранные типы исключаются из обычного режима добавления. Замена запускается отдельным действием из текущей сборки.
5. В режиме замены заменяемый компонент исключается из base IDs: кандидат должен быть совместим со всеми оставшимися
   компонентами, но не обязан быть совместим с удаляемым компонентом.
6. Если после исключения заменяемого слота bases отсутствуют, кандидаты замены берутся из общего активного каталога с
   фиксированным фильтром типа; при одном base используется direct endpoint, при двух и более — intersection endpoint.
7. Существующий черновик проверяется batch search. Для каждой пары выбранных компонентов наличие второго компонента в
   direct result первого означает совместимость; транзитивное достижение не учитывается.
8. Архивный, недоступный или ещё загружающийся слот не удаляется автоматически, но блокирует достоверную проверку и
   контекстный подбор до удаления, восстановления или успешного retry.
9. Несовместимый восстановленный черновик сохраняется и явно помечается. Обычное добавление блокируется, пока конфликт
   не устранён удалением или корректной заменой.
10. При ошибке compatibility API UI сохраняет текущий draft, показывает retry и не откатывается молча к общему каталогу.
11. Имя и тип фильтруют уже полученный compatibility result на клиенте; пагинация также клиентская, потому что текущие
    compatibility endpoints возвращают полный непагинированный набор.
12. Compatibility candidate cards используют name, brand и type из short response. Изображение не догружается; это
    осознанный компромисс до появления подходящего server response.
13. В 9.21 показывается только итоговый статус «совместим / конфликт / проверка недоступна» и факт прямой совместимости.
    Manual/automatic evidence и транзитивные пути визуализируются в 9.22.
14. Смена предметной области изолирует все compatibility queries и replacement state; локальные draft records разных
    областей не смешиваются.
15. Все действия доступны с клавиатуры, live/status messages доступны screen reader, минимальная ширина 360 px не
    получает горизонтальную прокрутку.
16. Runtime-авторизация не добавляется в рамках пункта; сохраняется текущая security-модель проекта.

## Considered Approaches

### A. Server-driven contextual browser with explicit replacement mode — selected

- Single endpoint используется для одного base, intersection endpoint — для нескольких, batch endpoint — для
  валидации состава.
- Сервер остаётся источником истины для automatic/manual compatibility и пересечения.
- Отдельный replacement mode корректно исключает заменяемый слот из bases.
- Недостаток: compatibility cards не имеют изображений, а режим замены требует отдельного состояния UI.

### B. Keep the paginated catalog and disable incompatible cards

- Внешне почти не меняет 9.20.
- Не гарантирует удобный поиск: совместимые варианты могут находиться на других server pages, а текущий compatibility
  API не поддерживает server pagination/search.
- Требует объединять два разных источника данных и сложнее объяснять empty state.

### C. Fetch batch results and intersect all sets in the browser

- Одним ответом можно построить разные candidate pools для типов.
- Дублирует уже реализованную backend intersection semantics и повышает риск расхождения клиента и сервера.
- Не решает выбор первого/единственного replacement candidate без дополнительного catalog query.

## Development Approach

- **Testing approach:** regular — небольшая реализация, затем unit/component tests в рамках той же задачи; E2E после
  стабилизации сценария.
- Выполнять задачи последовательно и полностью; перед следующей задачей запускать релевантные тесты.
- Для pure преобразований и определения конфликтов использовать отдельные функции без React-зависимостей.
- Не дублировать generated transport DTO и не изменять `build/generated/**` или
  `configurator-web/src/shared/api/generated/**` вручную.
- При изменении scope немедленно актуализировать этот план и требования эпика.
- Не трогать unrelated пользовательские изменения.

## Testing Strategy

- **Unit/model:** нормализация групп, client-side filters/pagination, вычисление конфликтных пар из batch result,
  исключение заменяемого base, обработка пустых и неполных результатов.
- **API hooks with MSW:** single, batch и intersection requests, `includeTransitive=false`, domain-scoped query keys,
  disabled states, retries и backend errors.
- **Component/workspace:** empty draft catalog, direct candidates, intersection candidates, valid/conflicting/unresolved
  assembly, explicit replacement, empty/error/loading states, domain isolation and persisted draft recovery.
- **E2E:** свободный первый выбор, прямое добавление, серверное пересечение, блокировка несовместимого варианта,
  корректная замена без проверки против удаляемого слота, reload и responsive smoke в Chromium/Firefox/WebKit.
- **Required verification:** `npm ci`, `npm run check`, `npm run test:coverage`, `npm run test:e2e`.
- Backend/external integration не требуются, если OpenAPI/backend/Docker contract действительно не изменятся.

## Solution Overview

```text
local draft + hydrated slots
            |
            +--> batch direct search --> assembly status / conflict pairs
            |
            +--> 0 bases --> active component catalog
            +--> 1 base  --> direct compatibility
            +--> 2+ bases --> server intersection
                                      |
                                      +--> type/name filter + client pagination
                                      +--> add candidate to local draft

replacement target --> remove target from bases --> choose the same 0/1/2+ query path
```

TanStack Query хранит три read models под одной domain-scoped key family. UI получает унифицированные короткие
candidate records, но raw explanations сохраняются в query data для 9.22. Модель validation строит детерминированный
набор конфликтных пар и IDs конфликтных слотов из batch response, не меняя draft автоматически.

## Technical Details

- Query keys включают `domainId`, режим (`single`, `batch`, `intersection`), ordered base IDs и
  `includeTransitive=false`.
- POST search/intersection используются через `useQuery`, поскольку операции read-only и должны кэшироваться по
  входным IDs; они не оформляются как mutations.
- Query запускается только после завершённой hydration всех bases и подтверждения, что они активны.
- Candidate identity — `id/componentTypeId`; отображаемые поля — `name/brand/componentTypeName`. Explanations не
  теряются в model mapping.
- В обычном режиме группы уже выбранных типов скрываются. В replacement mode фиксируется один target type и из списка
  исключается текущий component ID.
- Конфликтная пара хранится в каноническом порядке draft indices, чтобы одно ребро не отображалось дважды.
- Изменение draft IDs автоматически переключает query key; предыдущие данные не должны отображаться как результат
  новой сборки.
- Compatibility empty state отличается от catalog empty state: отсутствие пересечения — валидный `200`, а не ошибка.

## What Goes Where

- `features/configurator/api` — вызовы generated SDK, domain-scoped query options/hooks.
- `features/configurator/model` — чистые candidate/validation/replacement helpers.
- `features/configurator/ui` — status summary, контекстный browser и replacement controls.
- `pages/ConfiguratorPage.tsx` — orchestration workspace/modal state без transport logic.
- `shared/i18n/resources.ts` — русские и английские строки.
- `docs/requirements/epic-9-frontend.md` — окончательные acceptance criteria 9.21.

## Implementation Steps

### Task 1: Finalize the 9.21 product contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260823-direct-compatibility-search-ui.md`

- [x] добавить подробный раздел 9.21 с режимами empty/single/intersection/replacement
- [x] зафиксировать строгую direct-only validation и поведение старого несовместимого draft
- [x] зафиксировать loading/error/empty, accessibility, responsive и non-goals 9.22
- [x] сверить требования со всеми тремя существующими OpenAPI endpoints
- [x] проверить форматирование документации перед следующей задачей

### Task 2: Add domain-scoped compatibility query adapters

**Files:**

- Create: `configurator-web/src/features/configurator/api/configurator-compatibility.ts`
- Create: `configurator-web/src/features/configurator/api/configurator-compatibility.test.tsx`

- [x] создать общую query key family с `domainId`, ordered component IDs и mode
- [x] добавить cached query для direct single-component search с `includeTransitive=false`
- [x] добавить cached POST query для batch validation с `includeTransitive=false`
- [x] добавить cached POST query для server intersection с `includeTransitive=false`
- [x] корректно отключать запросы без валидного domain/base input
- [x] протестировать request shape, caching/isolation, disabled states, success и API errors через MSW
- [x] запустить новые API tests — они должны пройти до Task 3

### Task 3: Build compatibility candidate and assembly validation models

**Files:**

- Create: `configurator-web/src/features/configurator/model/configurator-compatibility.ts`
- Create: `configurator-web/src/features/configurator/model/configurator-compatibility.test.ts`
- Modify: `configurator-web/src/features/configurator/model/use-configurator-draft.ts`

- [x] унифицировать single/intersection groups без потери explanations для будущего 9.22
- [x] добавить deterministic client filter/pagination и исключение выбранных типов/current replacement ID
- [x] вычислять valid/conflicting pairs и conflict component IDs из batch response
- [x] формировать base IDs для обычного и replacement modes
- [x] классифицировать hydration как ready, pending или blocked без автоматического удаления draft item
- [x] протестировать happy paths, empty sets, reordered IDs, missing evidence, conflicts и replacement edge cases
- [x] запустить model tests — они должны пройти до Task 4

### Task 4: Display direct assembly validation and replacement controls

**Files:**

- Create: `configurator-web/src/features/configurator/ui/AssemblyCompatibilityStatus.tsx`
- Create: `configurator-web/src/features/configurator/ui/ConfiguratorCandidateCard.tsx`
- Modify: `configurator-web/src/features/configurator/ui/CurrentAssembly.tsx`
- Modify: `configurator-web/src/features/configurator/ui/configurator-workspace.module.css`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] показать pending, valid, conflict, blocked и request-error states с retry
- [x] пометить конфликтные/архивные/недоступные slots без изменения сохранённого draft
- [x] добавить доступное действие «Заменить» для ready slot и выход из replacement mode
- [x] блокировать обычное добавление при unresolved или несовместимом составе
- [x] добавить RU/EN строки и responsive styles без hardcoded light-only colors
- [x] протестировать valid/conflicting/restored/unavailable/error/retry и keyboard flows
- [x] запустить workspace component tests — они должны пройти до Task 5

### Task 5: Replace the generic browser with contextual direct/intersection search

**Files:**

- Modify: `configurator-web/src/features/configurator/ui/AvailableComponentBrowser.tsx`
- Modify: `configurator-web/src/pages/ConfiguratorPage.tsx`
- Modify: `configurator-web/src/features/configurator/ui/configurator-workspace.module.css`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: `configurator-web/src/features/configurator/ui/ConfiguratorWorkspace.test.tsx`

- [x] сохранить общий server-paginated catalog только для пустой сборки и replacement без remaining bases
- [x] подключить direct query для одного base и intersection query для двух и более bases
- [x] добавить client-side name/type filters и pagination compatibility results
- [x] скрыть уже выбранные типы в add mode и зафиксировать target type в replacement mode
- [x] показать понятные loading/refresh/error/retry/no-intersection states без fallback к несовместимому каталогу
- [x] завершать replacement mode только после подтверждённой замены или явной отмены
- [x] не создавать detail N+1; отобразить short candidate card без фиктивного изображения
- [x] протестировать 0/1/2+ bases, filters, empty intersection, replacement base exclusion и domain switch
- [x] запустить все configurator feature tests — они должны пройти до Task 6

### Task 6: Add end-to-end compatibility workspace coverage

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`

- [x] добавить deterministic mocks для direct, batch и intersection endpoints
- [x] проверить первый свободный выбор и переход к direct candidates
- [x] проверить пересечение после второго компонента и отсутствие несовместимого кандидата
- [x] проверить conflict status восстановленного draft и устранение конфликта replacement flow
- [x] проверить, что replacement request исключает заменяемый component ID
- [x] проверить reload, смену области и mobile 360 px без горизонтальной прокрутки
- [x] запустить Playwright для Chromium, Firefox и WebKit — все сценарии должны пройти

### Task 7: Verify acceptance criteria and repository boundaries

**Files:**

- Modify if required: `docs/plans/20260823-direct-compatibility-search-ui.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run check`
- [x] выполнить `npm run test:coverage` и сверить coverage
- [x] выполнить `npm run test:e2e`
- [x] убедиться, что OpenAPI, backend, Flyway, jOOQ и generated API client не изменены
- [x] проверить `git diff --check` и отсутствие unrelated `testcontainers.properties` в staged diff

### Task 8: [Final] Complete implementation documentation

**Files:**

- Modify: `docs/plans/20260823-direct-compatibility-search-ui.md`
- Move: `docs/plans/20260823-direct-compatibility-search-ui.md` to `docs/plans/completed/`

- [x] отметить фактически выполненные пункты и отклонения от плана
- [x] обновить README/AGENTS только если появился новый повторно используемый проектный паттерн
- [x] перенести завершённый план в `docs/plans/completed/`
- [x] подготовить отчёт по изменениям, API/DB impact, выполненным и невыполненным проверкам

## Completion Record

- Реализованы direct-only candidate search, server intersection, batch validation и slot-aware replacement.
- Общий каталог сохранён для первого выбора и замены единственного слота; compatibility results фильтруются и
  пагинируются на клиенте без detail N+1.
- Восстановленные конфликтные, архивные и недоступные позиции не удаляются автоматически; UI показывает состояния
  valid/conflict/blocked/error и доступные действия восстановления.
- Большая candidate card разметка вынесена в отдельный компонент; повторно используемые глобальные архитектурные
  правила не изменились, поэтому README и AGENTS не потребовали обновления.
- OpenAPI, backend, Flyway, jOOQ, БД и generated frontend API client не изменены.
- Проверки: `npm ci`, `npm run check` (153 tests), `npm run test:coverage` (90.45% lines), `npm run test:e2e`
  (30 tests: Chromium, Firefox, WebKit), `git diff --check`.
- Один промежуточный coverage-прогон обнаружил существующую нестабильность language-menu test при последовательном
  запуске сразу после полного check; отдельный повторный полный coverage-прогон прошёл 153/153. Финальные обязательные
  прогоны зелёные.

## Post-Completion

### Manual verification

- Проверить понятность перехода «общий каталог → прямые кандидаты → пересечение» на реальном demo domain.
- Проверить replacement flow на сборке из 3–6 компонентов и при пустом результате.
- Проверить screen reader announcement статуса и клавиатурную навигацию.
- Проверить light/dark/system themes и ширины 360 px, tablet и desktop.

### Deferred to 9.22+

- Переключатель транзитивного режима и визуализация shortest paths.
- Подробные manual/automatic/transitive explanations.
- Server-side name/type filters и pagination compatibility results.
- Изображения или полные component details в compatibility responses.
- Серверное сохранение конфигурации и окончательная server validation (9.23).
