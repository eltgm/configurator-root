# CON1-104 — Hard delete of saved configuration

## Overview

Добавить безвозвратное удаление сохранённой конфигурации для frontend. Endpoint удаляет только конфигурацию текущего
пользователя, возвращает пустой успешный ответ и не раскрывает существование чужих данных.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Существующая цепочка: `ConfigurationController -> ConfigurationFacade -> ConfigurationService -> ConfigurationRepository`.
- `configuration_component.configuration_id` уже имеет `ON DELETE CASCADE`.
- Configuration ownership определяется через `CurrentUserProvider`; чужие read/update операции возвращают `404`.
- Миграция БД и изменение domain-модели не требуются.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — контракт и реализация, затем тесты каждого слоя.
- Выполнять задачи последовательно небольшими изменениями.
- Каждая новая и изменённая ветка поведения получает success/error tests.
- Все тесты текущей задачи должны пройти до следующего этапа.
- Сохранять `controller -> facade -> service -> outbound port -> infrastructure`.
- Не редактировать `build/generated/**` вручную.

## Testing Strategy

- Controller/facade/service unit tests для `204` delegation и ownership-aware not found.
- Repository tests для hard delete, cascade и защиты чужого владельца.
- Shared local/external integration contract для удаления, повторного удаления, missing/foreign ID и изоляции данных.
- Полный `./gradlew build` и внешний PostgreSQL/MinIO contract.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или решений.

## Solution Overview

1. Добавить `DELETE /configurations/{id}`.
2. Удалять строку одной командой с условием `(id, created_by_user_id)`.
3. Полагаться на существующий FK cascade для удаления configuration-component links.
4. Возвращать `404`, если ownership-scoped delete не затронул строку.
5. Возвращать `204 No Content` только при фактическом удалении.

## Technical Details

- Success: `204 No Content`, пустое тело.
- Missing, foreign-owned или уже удалённая конфигурация: `404`.
- Invalid non-positive ID: `400` через generated boundary validation.
- Компоненты каталога, другие конфигурации и предметная область не удаляются.
- Database schema, Flyway и generated jOOQ schema не изменяются.

## Implementation Steps

### Task 1: Extend and document the delete REST contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] define `DELETE /configurations/{id}` and responses
- [x] document hard-delete, ownership and repeated-delete semantics
- [x] regenerate and compile OpenAPI interfaces through Gradle
- [x] verify generated code is not edited or committed

### Task 2: Implement ownership-scoped persistence delete

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ConfigurationRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ConfigurationRepositoryImpl.java`
- Modify: repository tests

- [x] add `deleteByIdAndUserId` returning whether a row was deleted
- [x] delete by configuration ID and owner in one SQL command
- [x] verify component links are removed by cascade
- [x] verify catalog components and other configurations remain
- [x] test missing and foreign-owner cases
- [x] run repository tests

### Task 3: Implement service, facade and controller delete

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ConfigurationFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ConfigurationFacadeImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ConfigurationController.java`
- Modify: service/facade/controller tests

- [x] resolve current user at application boundary
- [x] map unsuccessful delete to ownership-safe `NotFoundException`
- [x] delegate through facade
- [x] return `204 No Content` from controller
- [x] test success, missing/foreign and repeated-delete behavior
- [x] run affected unit tests

### Task 4: Extend shared local/external integration contract

**Files:**
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractConfigurationControllerContract.groovy`
- Modify: local/external configuration transports

- [x] verify successful delete returns empty `204`
- [x] verify GET, export and list no longer expose deleted configuration
- [x] verify repeated, missing and foreign-owned delete return `404`
- [x] verify invalid ID returns `400`
- [x] verify other configurations and catalog components remain
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
- Runtime authentication/authorization remains outside CON1-104.
