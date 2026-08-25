# Переиспользуемый каталог атрибутов предметной области

## Overview

- Перенести определения атрибутов из владения одним типом компонента в отдельный каталог текущей предметной области.
- Разрешить связывать один атрибут с несколькими типами компонентов: при работе с типом можно создать новый атрибут
  или подключить существующий из каталога.
- Хранить общими `name`, `label`, `dataType` и `enumValues`; хранить `isRequired` и `orderIndex` отдельно для каждой
  связи атрибута с типом.
- Добавить самостоятельный frontend-раздел «Атрибуты» для просмотра, создания, изменения и удаления каталожных
  атрибутов.
- Разделить удаление связи и определения: «Убрать из типа» удаляет только связь и значения компонентов этого типа;
  «Удалить атрибут» удаляет определение, все связи и все значения. Оба действия требуют явного подтверждения.
- Запрещать удаление каталожного атрибута, пока он используется правилом совместимости, и возвращать понятный `409`.

## Context (from discovery)

- **Backend:** Java 21, Spring Boot 3.4.11, jOOQ/PostgreSQL; persistence реализован через собственные outbound ports,
  не через Spring Data JPA/JDBC.
- **Текущая схема:** `attribute_definition.component_type_id` задаёт единственного владельца; `is_required` и
  `order_index` находятся в самой definition; `attribute_value` удаляется каскадно вместе с definition; условия
  совместимости ссылаются на definition через `ON DELETE RESTRICT`.
- **Текущий API:** поддерживает list/create для `/component-types/{id}/attributes` и update для `/attributes/{id}`;
  delete, доменный каталог и attach/detach отсутствуют.
- **Backend-заготовка:** `AttributeService.deleteById` и `AttributeRepository.deleteById` существуют, но не выведены
  через facade/controller/OpenAPI и не нормализуют конфликт с правилами совместимости.
- **Frontend:** `/settings/types` умеет создавать и редактировать атрибут выбранного типа; отдельного маршрута,
  навигации, delete mutation и attach-existing flow нет.
- **Миграция:** существующие определения не объединяются автоматически, даже если их системные имена совпадают.
  Каждый ID, значение и compatibility-rule reference сохраняется, а definition связывается с прежним типом.
- **Локальные изменения:** `configurator-integration-tests/src/test/resources/testcontainers.properties` уже изменён
  пользователем и должен быть сохранён без включения в эту работу.

## Development Approach

- **Подход к тестированию:** обычный — небольшая часть реализации, затем её unit/contract/UI-тесты и успешный прогон
  перед переходом к следующей задаче.
- Сохранять архитектурный путь `controller -> facade -> service -> outbound port -> infrastructure`.
- Сначала менять source of truth (`specs/configurator-api.yaml`, новая Flyway migration), затем регенерировать код;
  `build/generated/**` и frontend generated SDK вручную не редактировать.
- Делать небольшие сфокусированные изменения и полностью завершать каждую задачу до следующей.
- Для каждой изменённой функции и каждого нового сценария добавлять success и error/edge tests.
- Все тесты соответствующей задачи должны пройти до перехода к следующей задаче.
- При изменении scope синхронно обновлять этот план.
- Не менять runtime security status: Bearer declaration не считается реализованной авторизацией.

## Testing Strategy

- **Backend unit/repository/architecture:** сервисы, facade/controller, mapper и jOOQ repositories; запуск
  `./gradlew :configurator:test` после каждого backend-этапа.
- **Unified integration contract:** дополнить `AbstractAttributesControllerContract`, чтобы одни сценарии выполнялись
  local и external transport implementations; обновить deterministic SQL fixtures под новую схему.
- **Frontend unit/integration:** MSW-тесты query/mutation hooks, каталожной страницы, attach/detach и destructive
  confirmation; запуск `npm run check` и `npm run test:coverage`.
- **E2E/accessibility/visual:** добавить управление каталогом и переиспользование атрибута в Playwright fixtures/specs,
  accessibility routes/interactions и reviewed visual baseline для нового раздела.
- **Definition of Done:** выполнить `./gradlew build`, затем на полном Compose
  `./gradlew :configurator-integration-tests:externalIntegrationTest`; frontend — `npm ci`, `npm run api:check`,
  `npm run check`, `npm run test:coverage`, `npm run test:e2e`, `npm run test:accessibility`, `npm run test:visual` и
  `npm run test:delivery`. Недоступные проверки явно отметить непроверенными.

## Progress Tracking

- Отмечать завершённые пункты `[x]` сразу после выполнения.
- Новые обнаруженные задачи помечать `[+]`, блокеры — `[!]`.
- Не переходить к следующей задаче при падающих тестах текущей.
- При отклонении реализации от согласованной модели обновить Overview, Technical Details и соответствующие tasks.

## Solution Overview

- `attribute_definition` становится доменным каталожным объектом и получает `domain_id` вместо
  `component_type_id`.
- Новая таблица `component_type_attribute` реализует many-to-many связь и хранит per-type настройки
  `is_required`/`order_index`.
- Каталожное определение остаётся единой сущностью: изменение общих полей действует во всех связанных типах.
- Получение атрибутов типа возвращает составное представление definition + link settings, чтобы component validation
  и формы знали обязательность и порядок в контексте конкретного типа.
- Attach existing и изменение link settings выполняются одним idempotent endpoint; detach транзакционно удаляет
  значения только компонентов указанного типа, затем связь.
- Catalog delete транзакционно удаляет definition; существующие FK каскадно удаляют связи и значения, а service
  заранее выявляет references из compatibility rules и возвращает доменный conflict вместо raw database error.
- Compatibility rule может сравнивать один переиспользуемый attribute ID на обеих сторонах, поэтому прежнее
  ограничение `left_attribute_definition_id <> right_attribute_definition_id` удаляется.

## Technical Details

### Data model

- `attribute_definition`: `id`, `domain_id`, `name`, `label`, `data_type`, `enum_values_json`, `created_at`.
- `component_type_attribute`: composite key (`component_type_id`, `attribute_definition_id`), `is_required`,
  `order_index`, `created_at`; индексы для стабильной выдачи и обратного поиска.
- FK definition -> domain и link -> component type/definition используют согласованные cascade rules; compatibility
  conditions сохраняют `ON DELETE RESTRICT`.
- Миграция `V7` создаёт link для каждой текущей definition, переносит `is_required`/`order_index`, вычисляет `domain_id`
  через прежний component type, сохраняет ID и ссылки, затем удаляет старые колонки и distinct-attribute check.
- Каталог допускает сохранённые одноимённые definitions после миграции; service не разрешает связать с одним типом две
  definitions с одинаковым `name`.

### REST contract

- `GET /domains/{domainId}/attributes` — каталог текущей области.
- `POST /domains/{domainId}/attributes` — создать definition без обязательной привязки к типу.
- `PUT /attributes/{id}` — изменить только общие поля каталожной definition.
- `DELETE /attributes/{id}` — удалить definition со всеми links/values либо вернуть `404`/`409`.
- Сохранить `GET /component-types/{id}/attributes` как выдачу связанных attributes с per-type metadata.
- Сохранить `POST /component-types/{id}/attributes` как атомарные create-definition + attach для обратной совместимости.
- `PUT /component-types/{componentTypeId}/attributes/{attributeId}` — attach existing или обновить `isRequired` и
  `orderIndex`; отклонять foreign-domain attribute и duplicate name in type.
- `DELETE /component-types/{componentTypeId}/attributes/{attributeId}` — удалить значения компонентов этого типа и
  detach link; definition остаётся в каталоге.
- Разделить OpenAPI schemas каталожной definition, связанного attribute view, create-definition request и link-settings
  request; нормализованные errors остаются в существующем формате.

### Frontend interaction

- Новый route `/settings/attributes` и desktop settings-navigation item «Атрибуты»; mobile settings entry продолжает
  покрывать весь `/settings` prefix.
- Каталожная страница показывает атрибуты выбранного `domainId`, связанные типы и действия create/edit/delete.
- На `/settings/types` кнопка добавления предлагает два сценария: «Создать новый» и «Использовать существующий».
- При выборе существующего показывать только attributes текущего domain, ещё не связанные с выбранным типом и не
  конфликтующие по системному имени; после выбора задаются per-type `isRequired`/`orderIndex`.
- В карточке связанного атрибута редактирование per-type settings отделено от перехода/редактирования общей definition;
  действие «Убрать из типа» показывает предупреждение об удалении значений этого типа.
- Catalog delete confirmation предупреждает об удалении всех links/values; conflict с compatibility rules остаётся в
  диалоге через общий normalized API error.
- Query keys включают `domainId`, а type links дополнительно `componentTypeId`; mutations обновляют/инвалидируют оба
  соответствующих cache scope без автоматических retries.

## What Goes Where

- **Implementation Steps:** source-of-truth API/schema, backend domain/application/persistence, unified contracts,
  generated clients, frontend feature/UI/navigation и automated verification.
- **Post-Completion:** ручная проверка UX на реальных данных, clean Compose external/delivery checks и review visual
  baselines; публикация/релиз в scope не входят.

## Implementation Steps

### Task 1: Нормализовать схему каталога и many-to-many связи

**Files:**
- Create: `configurator/src/main/resources/db/migration/V7__create-domain-attribute-catalog.sql`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/domain/model/AttributeDefinition.java`
- Create: `configurator/src/main/java/ru/sultanyarov/configurator/domain/model/ComponentTypeAttribute.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/AttributeRepository.java`
- Create: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ComponentTypeAttributeRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/AttributeRepositoryImpl.java`
- Create: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ComponentTypeAttributeRepositoryImpl.java`
- Regenerate: `configurator/build/generated/jooq/**` through Gradle lifecycle
- Modify: relevant persistence/domain tests and SQL fixtures under `configurator/src/test/**` and
  `configurator-integration-tests/src/test/resources/sql/**`

- [x] добавить `V7` с backfill без auto-merge, composite link key, indexes/FK и снятием obsolete columns/check
- [x] адаптировать global definition и per-type link domain models/ports без jOOQ types выше persistence boundary
- [x] реализовать catalog CRUD/list и link CRUD/list, включая детерминированную сортировку
- [x] обеспечить транзакционный detach: удалить attribute values только у components связанного типа, затем link
- [x] обновить deterministic SQL fixtures на новую структуру, не затрагивая локальный `testcontainers.properties`
- [x] написать repository tests для migration-preserved IDs, catalog isolation, attach/update/detach и cascade values
- [x] написать error/edge tests для foreign domain, duplicate link/name и compatibility-rule FK restriction
- [x] запустить `./gradlew :configurator:test` — тесты должны пройти до Task 2

### Task 2: Перестроить application services и validation под shared definitions

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/AttributeService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/AttributeServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/validator/ComponentValidatorImpl.java`
- Modify: compatibility validation/evaluation services that consume type attributes
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/service/core/AttributeServiceImplTest.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/application/validator/ComponentValidatorImplTest.java`
- Modify: relevant compatibility service/validator tests

- [x] реализовать domain-scoped catalog create/list/update/delete and link attach/update/detach use cases
- [x] перенести required/order validation на context-specific `ComponentTypeAttribute`
- [x] запретить смену `dataType` при существующих values и удаление definition при compatibility-rule references
- [x] разрешить compatibility condition использовать один shared attribute ID для двух разных component types
- [x] сохранить проверки принадлежности attribute/type одной предметной области и уникальности имени внутри типа
- [x] написать service/validator success tests для shared attribute, per-type settings и cascade detach/delete
- [x] написать `404`/`409`/validation tests для foreign domain, duplicates, persisted values и rule references
- [x] запустить `./gradlew :configurator:test` — тесты должны пройти до Task 3

### Task 3: Расширить OpenAPI и REST boundary

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/AttributesFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/AttributesFacadeImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/mapper/AttributeDefinitionMapper.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/AttributesController.java`
- Regenerate, do not edit: `configurator/build/generated/openapi/**`
- Modify: controller/facade/mapper tests for attributes

- [x] добавить catalog CRUD и attach/update/detach paths/schemas/responses в OpenAPI source of truth
- [x] сохранить create-and-attach endpoint и явно описать destructive cascade/conflict semantics
- [x] регенерировать backend interfaces/DTO и адаптировать mapper/facade/controller тонким REST boundary
- [x] вернуть `204` для успешных delete/detach, `404` для отсутствующих IDs и `409` для rule references/conflicts
- [x] написать controller/facade/mapper success tests для каждого нового endpoint
- [x] написать transport error tests для invalid scope, duplicate links/names, missing resources и conflicts
- [x] запустить `./gradlew :configurator:test` — тесты должны пройти до Task 4

### Task 4: Расширить единый local/external integration contract

**Files:**
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractAttributesControllerContract.groovy`
- Modify: relevant helpers in `configurator-integration-tests/src/test/groovy/**`
- Modify: relevant SQL fixtures in `configurator-integration-tests/src/test/resources/sql/**`

- [x] добавить contract scenarios catalog list/create/update/delete with domain isolation
- [x] добавить create-and-attach и attach-existing scenarios с разными per-type required/order settings
- [x] проверить detach cascade только для values components выбранного type при сохранении definition/других links/values
- [x] проверить catalog delete cascade и `409`, когда definition используется compatibility rule
- [x] проверить shared attribute в component create/update и compatibility evaluation
- [x] обновить fixtures без разветвления local/external логики
- [x] запустить `./gradlew :configurator-integration-tests:test` — тесты должны пройти до Task 5

### Task 5: Регенерировать frontend SDK и реализовать data layer

**Files:**
- Regenerate, do not edit: `configurator-web/src/shared/api/generated/**`
- Modify: `configurator-web/src/shared/api/index.ts`
- Modify: `configurator-web/src/features/attributes/api/attributes.ts`
- Modify: `configurator-web/src/features/attributes/api/attributes.test.ts`
- Modify/Create: attribute catalog/link models and validation under `configurator-web/src/features/attributes/model/**`

- [x] выполнить `npm run api:generate` после OpenAPI change и проверить отсутствие ручных transport DTO
- [x] разделить query keys catalog-by-domain и links-by-domain/type
- [x] добавить create/update/delete catalog and attach/update/detach mutations с корректной cache invalidation
- [x] отключить retries для mutations и использовать общий normalized API error pipeline
- [x] адаптировать формы компонентов и compatibility feature к новым generated view types
- [x] написать MSW hook/model success tests для catalog/link mutations и сортировки
- [x] написать cache/error tests для domain switch, cascade delete, detach и failed conflict
- [x] запустить `npm run api:check` и frontend unit/type checks — они должны пройти до Task 6

### Task 6: Добавить раздел «Атрибуты» и переиспользование на странице типов

**Files:**
- Create: `configurator-web/src/pages/AttributesPage.tsx`
- Create: `configurator-web/src/pages/AttributesPage.test.tsx`
- Modify: `configurator-web/src/pages/ComponentTypesPage.tsx`
- Modify/Create: attribute catalog/form/link/delete UI under `configurator-web/src/features/attributes/ui/**`
- Modify: `configurator-web/src/app/router/routes.tsx`
- Modify: `configurator-web/src/app/router/lazy-pages.tsx`
- Modify: `configurator-web/src/app/layout/navigation.ts`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify: relevant app/navigation/component-type page tests

- [x] добавить protected route `/settings/attributes`, lazy page и desktop settings navigation item
- [x] реализовать responsive catalog list with associated types and create/edit/delete actions
- [x] реализовать create-new/use-existing chooser и link-settings form на странице component types
- [x] реализовать отдельные edit-global/edit-link actions и destructive confirmations для detach/delete
- [x] привязать loading/empty/error/success states к shared UI, notifications и RU/EN translations
- [x] обеспечить keyboard/focus/mobile/dark-theme accessibility и отсутствие hardcoded light-only colors
- [x] написать UI success tests для catalog CRUD, attach existing и независимых per-type settings
- [x] написать UI error/confirmation tests для detach cascade, delete cascade, `409` rule reference и domain switch
- [x] запустить `npm run check` и `npm run test:coverage` — проверки должны пройти до Task 7

### Task 7: Добавить E2E, accessibility и visual coverage

**Files:**
- Modify: `configurator-web/e2e/fixtures/mock-api.ts`
- Modify/Create: relevant Playwright specs under `configurator-web/e2e/**`
- Update after review: `configurator-web/e2e/__screenshots__/linux-chromium/**`

- [x] расширить mock API domain catalog/link state и destructive operations
- [x] добавить E2E flow create catalog attribute -> attach to two types -> configure independently -> detach
- [x] добавить E2E delete confirmation/conflict flow и проверить сохранение definition после detach
- [x] добавить новый route/interactions в accessibility suite
- [x] добавить reviewed desktop/mobile visual state каталога и updated types page
- [x] запустить `npm run test:e2e` и `npm run test:accessibility`
- [x] запустить pinned-Docker `npm run test:visual`; baselines обновлять только после визуального review

### Task 8: Проверить acceptance criteria и полный runtime contract

**Files:**
- Modify only if required by verified contract: `README.md`, `docs/testing/FRONTEND_TESTING.md`
- Do not modify unless scope changes: `AGENTS.md`

- [x] проверить миграцию существующих IDs/values/rules без auto-merge и без потери данных
- [x] проверить все согласованные delete/detach, domain isolation, shared edit и per-type settings scenarios
- [x] выполнить `./gradlew build`
- [x] поднять полный Compose и выполнить `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [x] выполнить `npm ci`, `npm run api:check`, `npm run check` и `npm run test:coverage`
- [x] выполнить `npm run test:e2e`, `npm run test:accessibility`, `npm run test:visual` и `npm run test:delivery`
- [x] проверить JaCoCo line coverage >= 0.90 и отсутствие generated/client drift
- [x] выполнить diff/status audit и убедиться, что unrelated `testcontainers.properties` не включён в изменения

### Task 9: [Final] Обновить документацию и закрыть план

**Files:**
- Modify: `README.md` при изменении пользовательского описания функций
- Modify: `docs/plans/20260825-reusable-domain-attribute-catalog.md`
- Move on completion: `docs/plans/completed/20260825-reusable-domain-attribute-catalog.md`

- [x] описать доменный каталог, reuse, attach/detach и destructive semantics в пользовательской документации
- [x] зафиксировать реально выполненные/невыполненные проверки и оставшиеся release blockers
- [x] синхронизировать все progress markers с фактическим состоянием
- [x] переместить завершённый план в `docs/plans/completed/`

## Post-Completion

## Verification Results

- `./gradlew build` — успешно; включает unit/repository/architecture/integration checks и JaCoCo verification.
- `./gradlew :configurator-integration-tests:externalIntegrationTest` — успешно через production-like gateway.
- `npm run check` — успешно, 43 suites / 211 tests, API drift, lint, strict TypeScript и production build.
- `npm run test:coverage` — успешно: statements 90.06%, lines 90.63%.
- `npm run test:e2e` — успешно: 72 tests в Chromium, Firefox и WebKit.
- `npm run test:accessibility` — успешно: 36 desktop/mobile checks, включая `/settings/attributes`.
- `npm run test:visual:update` — успешно: 8 tests; desktop/mobile baselines каталога и обновлённая навигация просмотрены.
- `npm run test:delivery` — успешно через production gateway без HTTP mocks.
- Compose после внешних проверок остановлен; пользовательский `testcontainers.properties` сохранён как unrelated change.
- Сохраняющийся release blocker: runtime authentication/authorization по-прежнему не реализованы; поддерживается только
  trusted-local loopback deployment, как и до этой доработки.

**Manual verification:**

- Проверить на реальном backend создание атрибута из каталога и из типа, подключение к нескольким типам, независимые
  required/order settings, редактирование общих полей и оба destructive confirmation flows.
- Проверить понятность UX при одноимённых сохранённых definitions после миграции и при `409` от compatibility rules.
- Провести review новых visual baselines на desktop/mobile и light/dark themes.

**External/runtime verification:**

- Проверить migration `V7` на копии непустой базы перед delivery/release.
- Проверить внешний gateway contract и production SPA deep link `/settings/attributes` через `npm run test:delivery`.
- Security/authentication не расширяется: trusted-local loopback limitation и временный пользователь `-1` остаются
  release blockers для LAN/public/server deployment.
- Публикация images/assets, tag/release workflow и изменение веток/commit history в scope не входят.
