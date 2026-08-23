# CON1-124 — Configuration Copy, JSON Export and Permanent Delete UI

## Overview

Пункт 9.25 завершает управление сохранёнными конфигурациями: пользователь может создать независимую копию актуальной
конфигурации, скачать versioned JSON export и безвозвратно удалить конфигурацию после явного подтверждения. Действия
доступны как из detail 9.24, так и из компактного меню карточки списка.

Backend-контракты export и delete уже реализованы. `GET /configurations/{id}/export/json` возвращает owner-scoped
`ConfigurationExport` с `schemaVersion`, `exportedAt` и актуальным составом; архивные компоненты сохраняются в export.
`DELETE /configurations/{id}` атомарно удаляет конфигурацию и её component links, не затрагивая каталог и другие
конфигурации.

Отдельного copy endpoint нет. Рекомендуемый frontend-only вариант создаёт копию через существующий
`POST /domains/{id}/configurations`: modal предварительно заполняет новое название и описание, а backend строго
перепроверяет активность, область, уникальность типов и прямую совместимость полного snapshot. Поэтому историческую
конфигурацию с архивной позицией нельзя скопировать без предварительного исправления через editor 9.24.

При подтверждении рекомендуемого варианта реализация остаётся frontend-only. OpenAPI, backend, Flyway, jOOQ, БД и
generated API client изменять не требуется.

> План подготовлен по локальным source-of-truth файлам без Spring MCP: Amplicode установлен, но IntelliJ IDEA и
> Amplicode MCP не были подключены к текущей сессии.

## Context (from discovery)

- **Текущее состояние веток:** 9.24 уже слит в `develop` merge-коммитом `99bec11`; новую реализацию можно начинать из
  актуального `develop` в `feature/CON1-124` без stacked branch.
- **Copy contract gap:** OpenAPI не содержит copy endpoint. Существующий create принимает name, nullable description и
  `1..50` component IDs, допускает одинаковые названия и создаёт новую owner-scoped конфигурацию с новым ID/createdAt.
- **Strict copy validation:** create отвергает архивные компоненты, повтор типа и direct-incompatible состав. Изменение
  compatibility rules после сохранения может сделать snapshot некопируемым даже при отсутствии archived flag.
- **Export contract:** owner-only `GET /configurations/{id}/export/json`; missing/foreign — `404`; response header
  задаёт `attachment; filename="configuration-{id}.json"`; body — self-contained schema version 1 с server timestamp.
- **Archived export:** export строится из текущего catalog state и сохраняет `archived=true`; клиент не должен
  фильтровать или повторно собирать document.
- **Delete contract:** owner-only permanent delete возвращает `204`; missing, foreign и repeated delete — `404`;
  identifier `0` — `400`. Configuration-component links удаляются каскадно, каталог и другие configurations остаются.
- **Atomicity:** UI не должен оптимистически удалять карточку или создавать copy до server success.
- **Generated SDK:** `deleteConfigurationsById` и `getConfigurationsByIdExportJson` уже экспортируются через
  `src/shared/api`; `ConfigurationExport` сгенерирован из OpenAPI.
- **Frontend 9.24:** detail/edit routes, domain-scoped detail/list keys, strict editor, explicit domain mismatch и
  domain-change guard уже реализованы.
- **Create modal:** `CreateConfigurationModal` уже выполняет POST и field mapping, но всегда стартует с пустыми
  metadata и не показывает общую non-field ошибку. Для copy его следует безопасно обобщить, сохранив поведение 9.23.
- **Error UX:** проект имеет `showErrorNotification`, `ErrorState`, success notifications и подтверждение permanent
  delete через Mantine Modal; mutations автоматически не повторяются.
- **Download:** прямой переход на `/api/.../export/json` плохо совместим с будущим Bearer auth и не позволяет безопасно
  показать API error. Рекомендуется получить typed body generated SDK, затем скачать неизменённый JSON через Blob.
- **Pagination:** после удаления единственного элемента на странице `page > 0` список должен перейти на предыдущую
  страницу, а не оставаться в искусственном empty state.
- **Security:** OpenAPI декларирует Bearer/owner semantics, но runtime пока использует temporary system user. UI не
  должен описываться как production-ready authorization.
- **Unrelated state:** пользовательский
  `configurator-integration-tests/src/test/resources/testcontainers.properties` остаётся незатронутым и не включается
  в будущий commit.

## Proposed Product Contract

1. Detail header предоставляет действия «Копировать», «Скачать JSON» и визуально отделённое destructive-действие
   «Удалить» рядом с существующим edit/back navigation.
2. Каждая list card получает компактное responsive actions menu с open, edit, copy, export и delete; действия имеют
   configuration-specific accessible names.
3. Copy открывает modal с read-only составом, description исходной конфигурации и локализованным предварительным
   названием `<исходное название> — копия`; имя обрезается до 255 символов с сохранением полного suffix.
4. Пользователь может изменить copy name/description. Submit отправляет immutable snapshot component IDs через
   существующий `POST /domains/{id}/configurations`; source configuration и localStorage draft `/configurator` не
   меняются.
5. Copy доступен только если source composition не содержит archived components. UI показывает понятную причину
   недоступности; скрывать действие нельзя.
6. Отсутствие archived positions не гарантирует успешный copy: backend повторно проверяет текущие direct rules. При
   `400/404/409` modal, metadata и source page сохраняются, field details привязываются к полям, non-field error
   показывается внутри modal.
7. После успешного copy list family текущей области инвалидируется, response помещается в detail cache, показывается
   success notification и открывается detail новой конфигурации. Исходная конфигурация остаётся без изменений.
8. Export является on-demand mutation без кэширования и automatic retry. Повторное действие для той же конфигурации
   блокируется до ответа.
9. Клиент скачивает ровно полученный `ConfigurationExport`, pretty-printed двумя пробелами с завершающей новой строкой,
   MIME `application/json;charset=utf-8` и именем `configuration-{id}.json`; поля, archived flags и server timestamps не
   пересчитываются.
10. Для скачивания используется generated SDK + Blob/object URL. Временная ссылка удаляется, object URL освобождается;
    при API error файл не создаётся, UI показывает error notification и сохраняет текущий экран.
11. Delete открывает modal с названием конфигурации и явным предупреждением о необратимости. Подтверждение одной
    destructive-кнопкой соответствует текущему стандарту проекта; ввод названия вручную не требуется.
12. Во время DELETE modal нельзя закрыть click-outside/Escape, повторный submit блокируется; при `400/404` modal
    остаётся открыт и показывает нормализованную ошибку.
13. После успешного delete detail cache удаляется, list family области инвалидируется и показывается notification. Из
    detail пользователь возвращается на `/configurations`; из списка остаётся на текущей странице либо переходит на
    предыдущую, если удалён её последний элемент.
14. Delete не очищает configurator local draft, не удаляет catalog components и не изменяет другие configurations.
15. Copy/export/delete недоступны на edit route, чтобы не создавать конкурирующие действия рядом с dirty form;
    сначала пользователь сохраняет или отменяет редактирование.
16. Все действия доступны с клавиатуры, сохраняют focus после закрытия modal/menu, объявляют pending/error/success и
    работают без горизонтальной прокрутки начиная с 360 px.

## Considered Approaches

### A. Strict frontend copy through existing POST — recommended

- Не меняет публичный API и backend architecture.
- Использует тот же authoritative validation path, что обычное создание.
- Создаёт действительно независимую конфигурацию с новым ID и createdAt.
- Archived или более не совместимый historical snapshot нельзя скопировать; пользователь сначала исправляет его в
  editor.

### B. New backend `POST /configurations/{id}/copy`

- Может копировать historical composition без повторной active/direct validation, если это явно требуется продукту.
- Требует определить новые semantics: сохранять ли архивные позиции, делать ли snapshot текущего catalog state, как
  называть copy, какой status возвращать при missing component.
- Затрагивает OpenAPI, controller/facade/service/repository contracts и local/external integration tests.
- Не рекомендуется без явного требования копировать архивные или устаревшие конфигурации.

### C. Load source into `/configurator` and save manually

- Позволяет пользователю починить состав до создания.
- Перезаписывает или смешивает независимый localStorage draft, не является быстрым copy и повторяет отклонённую в 9.24
  архитектуру.
- Не рассматривается.

### Export delivery alternatives

- **Generated SDK + Blob — recommended:** совместим с будущей Bearer auth, нормализует ошибки, тестируется без
  navigation; filename детерминирован по контракту.
- **Direct anchor to API endpoint:** использует server `Content-Disposition`, но не показывает API error и может
  потерять auth header после реализации JWT.

## Development Approach

- **Testing approach:** regular — небольшая реализация, затем unit/component tests в рамках той же задачи; E2E после
  стабилизации полного operations flow.
- После подтверждения создать `feature/CON1-124` от актуального `develop`; новая ветка не зависит от незавершённой
  feature branch.
- Выполнять задачи последовательно; relevant tests должны пройти до перехода к следующей задаче.
- Не редактировать generated code и не дублировать transport DTO; использовать generated exports через
  `src/shared/api`.
- Держать request/cache logic в `features/configurations/api`, pure copy/download helpers в `model`/`shared/lib`,
  dialogs и menus в `features/configurations/ui`, route/page reactions — в pages.
- Не добавлять optimistic delete/copy, client-side compatibility algorithm или отдельное global store.
- При изменении copy semantics сначала актуализировать этот план и требования эпика.

## Testing Strategy

- **API/MSW:** export GET, delete `204`, `400/404`, no automatic retry, targeted list invalidation, exact detail cache
  removal and create response cache seeding.
- **Unit/model:** localized copy suffix/truncation at 255, description/component snapshot, archived availability,
  deterministic filename, pretty JSON and Blob cleanup.
- **Create/copy modal:** initial metadata, independent edits, read-only composition, archived state, field details,
  non-field `409`, pending close lock and source preservation.
- **Actions/detail:** accessible action names, export pending/success/error, delete cancel/confirm/error/success, detail
  redirect and no actions on edit route.
- **List:** responsive menu, copy/export/delete callbacks, domain isolation, current-page retention and previous-page
  fallback after deleting the last card.
- **E2E:** create/open; copy and verify independent source/new detail; download and inspect JSON filename/body; cancel then
  confirm permanent delete; verify list/source/catalog survival; run at mobile viewport in Chromium, Firefox and WebKit.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e -- --workers=1`, `git diff --check`.
- Backend/external integration tests не требуются для confirmed frontend-only approach. При выборе backend copy endpoint
  обязательны `./gradlew build` и `:configurator-integration-tests:externalIntegrationTest`.

## Solution Overview

```text
detail/list action
      |
      +--> COPY --> prefilled modal --> POST /domains/{domainId}/configurations
      |                                      |
      |                                      +--> seed new detail cache
      |                                      +--> invalidate domain list
      |                                      +--> navigate new detail
      |
      +--> EXPORT --> GET /configurations/{id}/export/json
      |                                      |
      |                                      +--> JSON.stringify server DTO
      |                                      +--> Blob + temporary anchor download
      |
      +--> DELETE --> irreversible confirm --> DELETE /configurations/{id}
                                             |
                                             +--> remove detail cache
                                             +--> invalidate domain list
                                             +--> reconcile list page / navigate list
```

UI pages владеют selected operation/target, а reusable action presentation только сообщает intent. Это позволяет иметь
один copy/delete modal на list page вместо modal/mutation instance на каждой карточке и сохранить одинаковую семантику
detail/list.

## Technical Details

- `configurationKeys.detail(domainId, id)` удаляется только после успешного DELETE; `configurationKeys.lists(domainId)`
  инвалидируется после successful create/delete.
- `useCreateConfigurationMutation` рекомендуется дополнить `setQueryData(detail(...))` на success, чтобы переход к
  copied detail не создавал лишний GET; list invalidation сохраняется.
- `useExportConfigurationMutation` возвращает generated `ConfigurationExport`; download side effect выполняется в UI
  после `mutateAsync`, чтобы API adapter оставался тестируемым и не зависел от DOM.
- `serializeConfigurationExport` не изменяет generated data и добавляет только indentation/trailing newline.
- `downloadJson` получает injectable URL/document adapters для unit tests; production использует Blob,
  `URL.createObjectURL`, скрытый anchor, click/remove и deferred `URL.revokeObjectURL`.
- Copy name helper получает localized suffix и сохраняет suffix при обрезке исходного name до общей длины 255.
- Source configuration component IDs берутся в server order, но create semantics рассматривают их как полный snapshot;
  backend вернёт canonical order.
- Copy modal не запускает отдельный batch request. Archived blocker известен локально, а остальные concurrent rule
  changes проверяет authoritative POST.
- Delete modal получает полную configuration summary, а mutation variables содержат `domainId` и `configurationId`
  для точного cache update.
- List page до DELETE фиксирует `items.length` и `page`; successful delete последнего item при `page > 0` уменьшает page
  на один до/вместе с refetch.
- Export/download, copy и delete mutations имеют `retry: false` по общему QueryClient default и не добавляют локальных
  retry overrides, кроме явного повторного пользовательского действия.
- 9.25 не добавляет bulk operations, import JSON, restore deleted configuration, version history или sharing.

## What Goes Where

- `features/configurations/api/configurations.ts` — export/delete adapters, cache operations and create detail seeding.
- `features/configurations/model/configuration-operations.ts` — copy initial values, suffix truncation, archived
  availability, export serialization/filename.
- `shared/lib/download.ts` or configuration-local equivalent — reusable safe Blob download primitive without domain
  decisions.
- `features/configurations/ui/CreateConfigurationModal.tsx` — backward-compatible initial values/variant and visible
  non-field error.
- `features/configurations/ui/ConfigurationActionMenu.tsx` — accessible list/detail action presentation.
- `features/configurations/ui/CopyConfigurationModal.tsx` — thin copy specialization if generalizing create modal makes
  its API unclear.
- `features/configurations/ui/DeleteConfigurationModal.tsx` — irreversible confirmation and inline error.
- `features/configurations/ui/ConfigurationDetails.tsx` — detail action slots/callbacks.
- `features/configurations/ui/ConfigurationList.tsx` — per-card action menu callbacks only.
- `pages/ConfigurationDetailsPage.tsx` — selected action, export/download, post-copy navigation and post-delete redirect.
- `pages/ConfigurationsPage.tsx` — shared list operation target and pagination reconciliation.
- `shared/i18n/resources.ts` — RU/EN labels, warnings, errors and notifications.
- `docs/requirements/epic-9-frontend.md` — final acceptance criteria 9.25.

## Implementation Steps

### Task 1: Finalize the 9.25 product contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260823-configuration-copy-export-delete-ui.md`

- [x] подтвердить strict frontend copy либо заменить план новым backend copy contract
- [x] зафиксировать list/detail actions, archived copy blocker, export file and delete semantics
- [x] сверить acceptance criteria с OpenAPI statuses, backend contracts and integration tests
- [x] проверить форматирование документации до изменения runtime-кода

### Task 2: Add export and delete API adapters

**Files:**

- Modify: `configurator-web/src/features/configurations/api/configurations.ts`
- Modify: `configurator-web/src/features/configurations/api/configurations.test.tsx`

- [x] добавить typed on-demand export mutation через generated SDK
- [x] добавить delete mutation с domain/configuration variables
- [x] после delete удалить exact detail cache и инвалидировать только domain list family
- [x] после create seed copied detail cache без изменения configurator draft semantics
- [x] написать MSW tests для success, `400/404`, no retry and cache isolation
- [x] запустить API tests — они должны пройти до Task 3

### Task 3: Build pure copy and JSON download helpers

**Files:**

- Create: `configurator-web/src/features/configurations/model/configuration-operations.ts`
- Create: `configurator-web/src/features/configurations/model/configuration-operations.test.ts`
- Create/modify: `configurator-web/src/shared/lib/download.ts`
- Create: `configurator-web/src/shared/lib/download.test.ts`

- [x] реализовать copy suffix/truncation, initial metadata and immutable component snapshot
- [x] определить archived copy availability без повторной compatibility logic
- [x] сериализовать untouched export DTO с deterministic indentation/newline and filename
- [x] реализовать safe Blob/object URL lifecycle с injectable browser adapters
- [x] покрыть boundaries, unicode, archived, Blob click/remove/revoke and error-free cleanup тестами
- [x] запустить model/shared tests — они должны пройти до Task 4

### Task 4: Generalize create modal for strict copy

**Files:**

- Modify: `configurator-web/src/features/configurations/ui/CreateConfigurationModal.tsx`
- Modify/create relevant modal tests
- Modify: `configurator-web/src/shared/i18n/resources.ts`

- [x] добавить backward-compatible initial values and create/copy presentation variant
- [x] сохранить обычный create flow 9.23 без изменения default values and post-success behavior
- [x] показать non-field POST error внутри modal и сбрасывать stale error при новом open/edit
- [x] заблокировать close/duplicate submit pending и сохранить copy draft при `409`
- [x] покрыть ordinary create regression, prefilled copy, field/non-field errors and pending tests
- [x] запустить modal/configurator tests — они должны пройти до Task 5

### Task 5: Implement reusable configuration operation controls

**Files:**

- Create: `configurator-web/src/features/configurations/ui/ConfigurationActions.tsx`
- Create: `configurator-web/src/features/configurations/ui/DeleteConfigurationModal.tsx`
- Create/modify relevant component tests
- Modify: `configurator-web/src/shared/i18n/resources.ts`

- [x] реализовать accessible open/edit/copy/export/delete presentation для list и detail
- [x] показать copy disabled reason для archived composition
- [x] реализовать irreversible delete modal с pending lock and inline error
- [x] обеспечить loading state только активной export/delete operation
- [x] покрыть keyboard/menu, cancel, confirm, error and archived action tests
- [x] запустить operation component tests — они должны пройти до Task 6

### Task 6: Integrate copy/export/delete into configuration detail

**Files:**

- Modify: `configurator-web/src/features/configurations/ui/ConfigurationDetails.tsx`
- Modify: `configurator-web/src/pages/ConfigurationDetailsPage.tsx`
- Modify: `configurator-web/src/features/configurations/ui/ConfigurationDetailsAndEditor.test.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`

- [x] подключить detail actions без изменения edit route
- [x] после copy открыть new detail and preserve source/local configurator draft
- [x] скачать server export and show success/error notification without navigation
- [x] после confirmed delete navigate list only on `204`; preserve page/modal on error
- [x] покрыть copy success/conflict, archived blocker, export body/download/404 and delete flows
- [x] запустить detail tests — они должны пройти до Task 7

### Task 7: Integrate compact actions into paged list

**Files:**

- Modify: `configurator-web/src/features/configurations/ui/ConfigurationList.tsx`
- Modify: `configurator-web/src/pages/ConfigurationsPage.tsx`
- Modify: `configurator-web/src/features/configurations/ui/ConfigurationsPage.test.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`

- [x] добавить card action menu without per-card modal instances
- [x] переиспользовать одинаковые copy/export/delete semantics detail page
- [x] сохранить current page после обычного delete and decrement last non-first page
- [x] сбрасывать selected operation при domain switch and never mix query caches
- [x] покрыть list actions, pagination reconciliation, domain isolation and 360 px layout
- [x] запустить list tests — они должны пройти до Task 8

### Task 8: Add end-to-end operations coverage

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`
- Modify test API routes/fixtures only as needed

- [x] расширить stateful configuration routes for copy/export/delete
- [x] проверить independent copy and unchanged source
- [x] проверить browser download filename and parsed versioned JSON body
- [x] проверить cancel/confirm permanent delete and surviving catalog/source data
- [x] проверить detail/list actions and mobile viewport in Chromium, Firefox and WebKit
- [x] запустить targeted E2E — они должны пройти до full verification

### Task 9: Complete verification and documentation

**Files:**

- Modify: `docs/plans/20260823-configuration-copy-export-delete-ui.md`
- Move when complete: `docs/plans/completed/20260823-configuration-copy-export-delete-ui.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run check`
- [x] выполнить `npm run test:coverage`
- [x] выполнить `npm run test:e2e -- --workers=1`
- [x] выполнить `git diff --check` и проверить commit scope
- [x] подтвердить, что OpenAPI/backend/DB/generated code не изменились
- [x] не включать `testcontainers.properties` или другие unrelated files
- [x] заполнить Progress/Solution реальными результатами и перенести план в `completed/`

## Progress

- План подготовлен и подтверждён 2026-08-23; выбран strict frontend copy через существующий POST.
- Реализация завершена в `feature/CON1-124` поверх актуального `develop`.
- Добавлены copy/export/permanent delete в detail и список, общие dialogs/actions и domain-scoped cache updates.
- OpenAPI/backend: без изменений; `npm run api:check` подтвердил отсутствие drift generated client.
- БД/Flyway/jOOQ: без изменений.
- Generated frontend client: без изменений.
- Проверки: `npm ci`, `npm run check` (206 tests), `npm run test:coverage` (90.78% lines), Playwright
  (36/36 Chromium/Firefox/WebKit) и `git diff --check` прошли.
- Unrelated `configurator-integration-tests/src/test/resources/testcontainers.properties`: сохранён без изменений.

## Solution

Сохранённая конфигурация теперь копируется через существующий strict create contract с локализованным prefill и
archived blocker, экспортируется typed SDK-запросом в versioned JSON-файл и удаляется только после irreversible
confirmation. API adapters точно обновляют domain-scoped cache, UI переиспользует одни operation controls на detail и
list, а список возвращается на предыдущую страницу после удаления последней карточки. Stateful E2E подтверждает
независимость копии, содержимое download, cancel/confirm delete и сохранность исходной конфигурации, каталога и
localStorage draft.

## Post-Completion

- Runtime authentication/authorization остаётся отдельным release blocker; owner-only OpenAPI semantics пока не
  обеспечены Spring Security.
- Import JSON, backup/restore, bulk operations and deleted configuration recovery находятся вне 9.25.
- Если продукту потребуется копирование archived historical snapshot, необходимо отдельно спроектировать backend copy
  endpoint вместо ослабления существующего create validation.
