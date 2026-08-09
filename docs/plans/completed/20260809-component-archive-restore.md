# CON1-102 — Просмотр архива и восстановление компонентов

## Overview

Добавить API, необходимый frontend для отдельного просмотра архива и возврата компонента в основной каталог. Текущий
список компонентов получает необязательный фильтр `archived`, а новый идемпотентный restore endpoint снимает архивный
флаг и возвращает полный компонент с атрибутами и изображениями.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Архивирование уже реализовано как `DELETE /components/{id}` и сохраняет связанные данные.
- `GET /domains/{domainId}/components` сейчас возвращает активные и архивные записи без явного фильтра.
- `Component` уже содержит `archived`; изменение схемы БД не требуется.
- Application flow: `ComponentController -> ComponentFacade -> ComponentService -> ComponentRepository`.
- Persistence реализован собственным jOOQ adapter, не Spring Data.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — сначала контракт/реализация, затем тесты каждого слоя.
- Выполнять задачи последовательно небольшими изменениями.
- Каждая новая и изменённая ветка поведения получает success/error tests.
- Все тесты текущей задачи должны пройти до следующего этапа.
- Сохранять `controller -> facade -> service -> outbound port -> infrastructure`.
- Не редактировать `build/generated/**` вручную.
- Поддерживать обратную совместимость: отсутствующий `archived` сохраняет текущую выборку всех записей.

## Testing Strategy

- Controller/facade/service unit tests для фильтра и идемпотентного restore.
- Repository tests для `archived=true`, `false`, `null` и снятия архивного флага.
- Shared local/external integration contract для каталога, архива, восстановления и сохранения связанных данных.
- Полный `./gradlew build` и внешний PostgreSQL/MinIO contract.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или решений.

## Solution Overview

1. `GET /domains/{domainId}/components` получает nullable query-параметр `archived`.
2. `archived=true` выбирает только архив, `false` — только активные, отсутствие параметра — обе группы.
3. `POST /components/{id}/restore` загружает полный компонент и идемпотентно снимает флаг `archived`.
4. Активный компонент возвращается без записи в БД; неизвестный идентификатор возвращает `404`.
5. Атрибуты, изображения, совместимость и configuration references не изменяются.

## Technical Details

- Restore success: `200 OK` и полный `Component` с `archived=false`.
- Restore active component: `200 OK`, состояние без изменений.
- Invalid component ID: `400`; missing component: `404`.
- Existing pagination, type and name filters combine with `archived` through AND.
- Database schema and Flyway migrations are unchanged.

## Implementation Steps

### Task 1: Extend the OpenAPI archive contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] document nullable `archived` query filter and backward-compatible semantics
- [x] define `POST /components/{id}/restore`
- [x] define success and validation/not-found responses
- [x] regenerate and compile OpenAPI interfaces
- [x] verify generated code only through Gradle lifecycle

### Task 2: Add archive filtering and restore persistence

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ComponentRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ComponentRepositoryImpl.java`
- Modify: repository tests

- [x] add nullable archive condition to paged query
- [x] add repository operation that clears `archived`
- [x] test active-only, archived-only and unfiltered pages
- [x] test restore success and missing row
- [x] run repository tests

### Task 3: Implement restore and filtered list use cases

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacadeImpl.java`
- Modify: service/facade tests

- [x] pass archive filter through application layers
- [x] implement idempotent restore for active and archived components
- [x] return the full restored component without losing nested data
- [x] handle persistence failure as `BusinessException`
- [x] test success, idempotency, not found and failure paths
- [x] run affected unit tests

### Task 4: Expose the HTTP contract

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ComponentController.java`
- Modify: controller tests

- [x] pass nullable `archived` from generated list method
- [x] expose restore delegation with `200` response
- [x] keep generated validation at HTTP boundary
- [x] test parameter forwarding and response mapping
- [x] run controller tests

### Task 5: Extend the shared local/external contract

**Files:**
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractComponentControllerContract.groovy`
- Modify: deterministic SQL fixtures only if required

- [x] verify unfiltered list remains backward compatible
- [x] verify active-only and archive-only list filters
- [x] verify restore returns active component and moves it between filtered lists
- [x] verify restore preserves attributes and images
- [x] verify repeated restore, missing ID and invalid ID
- [x] run local integration tests

### Task 6: Verify acceptance criteria

- [x] run `./gradlew :configurator:test`
- [x] run `./gradlew build`
- [x] build/start external Docker contour
- [x] run `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [x] verify JaCoCo, ArchitectureTest and Spotless
- [x] inspect diff for generated code, credentials and unrelated files

### Task 7: Final documentation

- [x] synchronize requirements with implementation
- [x] mark completed plan items
- [x] move plan to `docs/plans/completed/`

## Post-Completion

- Commit and push only after owner review or explicit request.
- Runtime authentication/authorization remains outside CON1-102.
