# CON1-103 — Full update of saved configuration

## Overview

Добавить атомарное полное редактирование сохранённой конфигурации для frontend: пользователь изменяет название,
описание и весь состав компонентов одним запросом. После успешного обновления сервер возвращает актуальную полную
конфигурацию, а неизменяемые metadata и принадлежность предметной области сохраняются.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Создание уже использует `ConfigurationDraft(name, description, componentIds)` и централизованную бизнес-валидацию.
- Существующая цепочка: `ConfigurationController -> ConfigurationFacade -> ConfigurationService -> ConfigurationRepository`.
- Persistence реализован собственным jOOQ adapter; схема уже поддерживает замену связей configuration-component.
- Ownership скрывает чужую конфигурацию как `404` через current user provider.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — контракт и реализация, затем тесты каждого слоя.
- Выполнять задачи последовательно небольшими изменениями.
- Каждая новая и изменённая ветка поведения получает success/error tests.
- Все тесты текущей задачи должны пройти до следующего этапа.
- Сохранять `controller -> facade -> service -> outbound port -> infrastructure`.
- Не редактировать `build/generated/**` вручную.
- Переиспользовать create validation без расхождения правил.

## Testing Strategy

- Controller/facade/service unit tests для update delegation, mapping и полной строгой валидации.
- Repository tests для атомарной замены metadata и component links с ownership scope.
- Shared local/external integration contract для success, ownership, validation, conflicts и rollback.
- Полный `./gradlew build` и внешний PostgreSQL/MinIO contract.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или решений.

## Solution Overview

1. Добавить `PUT /configurations/{id}` и отдельный `UpdateConfigurationRequest`.
2. Полностью заменять `name`, `description` и набор component links.
3. Брать `domainId`, owner и `createdAt` из существующей конфигурации и не разрешать их менять.
4. Строго повторно валидировать весь итоговый состав по правилам create.
5. Выполнять update parent row и замену links в одной service-транзакции.

## Technical Details

- Success: `200 OK` и полный `Configuration`.
- Request: обязательные `name` и `componentIds`, nullable `description`.
- Архивный компонент всегда даёт `409`, включая компонент, ранее сохранённый в этой конфигурации.
- Foreign-domain/malformed input даёт `400`; missing component или configuration — `404`.
- Foreign-owned configuration маскируется как `404`.
- Duplicate component type и отсутствие прямой попарной совместимости дают `409`.
- Blank description нормализуется в `null`; name/description trim сохраняется.
- Database schema, Flyway и generated jOOQ schema не изменяются.

## Implementation Steps

### Task 1: Extend and document the update REST contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] define `UpdateConfigurationRequest`
- [x] define `PUT /configurations/{id}` and all responses
- [x] document strict replacement and immutable metadata semantics
- [x] regenerate and compile OpenAPI interfaces through Gradle
- [x] verify generated code is not edited or committed

### Task 2: Implement atomic persistence replacement

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ConfigurationRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ConfigurationRepositoryImpl.java`
- Modify: repository tests

- [x] add ownership-scoped repository update operation
- [x] update mutable metadata without changing domain, owner or createdAt
- [x] delete and recreate the complete component link set
- [x] return the freshly loaded configuration
- [x] test successful replacement, missing/foreign owner and related data preservation
- [x] run repository tests

### Task 3: Implement strict full-update use case

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationServiceImpl.java`
- Modify: service tests

- [x] load the existing owned configuration before validation
- [x] reuse draft normalization and complete create validation
- [x] reject archived, foreign-domain, duplicate-type and incompatible components
- [x] preserve immutable metadata in the repository command
- [x] map repository failure consistently
- [x] test success, normalization, ownership, strict conflicts and rollback paths
- [x] run service tests

### Task 4: Expose facade and controller update

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/mapper/ConfigurationMapper.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ConfigurationFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ConfigurationFacadeImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ConfigurationController.java`
- Modify: mapper/facade/controller tests

- [x] map `UpdateConfigurationRequest` to `ConfigurationDraft`
- [x] delegate full update through facade
- [x] expose generated PUT handler with `200` response
- [x] test mapping, delegation and response
- [x] run affected unit tests

### Task 5: Extend shared local/external integration contract

**Files:**
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractConfigurationControllerContract.groovy`
- Modify: deterministic SQL fixtures only if required

- [x] verify complete name, description and component-set replacement
- [x] verify immutable domain, owner and createdAt
- [x] verify blank description normalization
- [x] verify invalid input, missing and foreign-owned configuration
- [x] verify missing, foreign-domain and archived components
- [x] verify duplicate type and incompatible pair conflicts
- [x] verify failed update leaves the original configuration unchanged
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
- Runtime authentication/authorization remains outside CON1-103.
