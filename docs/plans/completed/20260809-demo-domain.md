# CON1-106 — Demo PC domain

## Overview

Добавить явное создание полноценной демонстрационной предметной области «Сборка ПК» для первого запуска frontend.
Один запрос создаёт каталог, атрибуты, совместимость и готовую конфигурацию атомарно и возвращает созданную область.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Существующие application services умеют создавать все необходимые DB-backed объекты и возвращают generated IDs.
- Новый orchestration service может объединить их общей `@Transactional` границей без прямого вызова repositories.
- MinIO не участвует в PostgreSQL-транзакции, поэтому изображения исключены из 9.7.
- Повторный запрос должен вернуть `409`, а не создавать копию.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — контракт и реализация, затем unit- и integration-тесты.
- Сначала изменить OpenAPI, generated interface получить только через Gradle lifecycle.
- Оркестратор работает только с domain-моделями и существующими application services.
- Вся DB-операция выполняется одной транзакцией; ошибка любого шага откатывает набор целиком.
- Идентификаторы передаются между этапами только из фактически сохранённых моделей.

## Testing Strategy

- Unit-тест orchestration service проверяет состав, связи, правила, конфигурацию и propagation ошибки.
- Controller/facade unit-тесты проверяют delegation и `201`.
- Shared local/external integration contract проверяет полный набор, производные read API и повторный `409`.
- Полный `./gradlew build` и внешний Docker contract.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или решений.

## Solution Overview

1. Добавить `POST /domains/demo` без тела запроса.
2. Создать домен текущего пользователя через `CurrentUserProvider`.
3. Создать шесть типов и их определения атрибутов.
4. Создать двенадцать компонентов, включая намеренно несовместимые варианты.
5. Создать пять автоматических rule sets и ручные связи для готовой конфигурации.
6. Сохранить конфигурацию «Игровой ПК 1440p» и вернуть домен.

## Technical Details

- Domain: `Сборка ПК`.
- Types: CPU, motherboard, memory, GPU, PSU and case — two components each.
- Automatic rules: socket equality, memory-standard equality, form-factor equality, GPU recommended power `LTE`
  PSU power, GPU length `LTE` case maximum length.
- Manual links complete direct pairwise compatibility of the saved six-component build where no automatic rule exists.
- Duplicate domain name is mapped by existing behavior to `409 ENTITY_ALREADY_EXISTS`.
- Images and database migrations are outside scope.

## Implementation Steps

### Task 1: Define and document the REST contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] add `POST /domains/demo` with `201` and `409`
- [x] document exact deterministic dataset and atomicity
- [x] regenerate and compile OpenAPI interfaces
- [x] verify generated code is not edited or committed

### Task 2: Implement transactional demo orchestration

**Files:**
- Create: demo application service interface and implementation
- Create: service unit tests

- [x] create domain through current-user-aware orchestration
- [x] create types, attributes and twelve components
- [x] create five automatic rules and required manual links
- [x] create saved configuration and return domain
- [x] test full collaborator contract and failure propagation
- [x] run service tests

### Task 3: Expose the use case through facade and controller

**Files:**
- Modify: `DomainFacade`, `DomainFacadeImpl`, `DomainController`
- Modify: facade/controller tests

- [x] add facade domain mapping
- [x] implement generated controller method with `201`
- [x] test delegation and response
- [x] run affected tests

### Task 4: Extend shared local/external integration contract

**Files:**
- Modify: `AbstractDomainControllerContract.groovy`

- [x] verify domain, six types, attributes and twelve components
- [x] verify automatic rules and graph/manual links
- [x] verify saved configuration is valid and readable
- [x] verify second request returns structured `409`
- [x] run local integration tests

### Task 5: Verify acceptance criteria

- [x] run `./gradlew :configurator:test`
- [x] run `./gradlew build`
- [x] build/start external Docker contour
- [x] run `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [x] verify JaCoCo, ArchitectureTest and Spotless
- [x] inspect diff for generated code, credentials and unrelated files

### Task 6: Final documentation

- [x] synchronize requirements with implementation
- [x] mark completed plan items
- [x] move plan to `docs/plans/completed/`

## Post-Completion

- Commit and push only after owner review or explicit request.
- Runtime authentication/authorization remains outside CON1-106.
