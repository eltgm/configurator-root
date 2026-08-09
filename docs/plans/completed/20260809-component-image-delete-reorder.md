# CON1-101 — Удаление и атомарная перестановка изображений компонента

## Overview

Добавить безвозвратное удаление изображения и сохранение полного порядка галереи для frontend Epic 9. Удаление
удаляет объект из приватного MinIO и затем его метаданные из PostgreSQL. Перестановка принимает полный итоговый список
идентификаторов, проверяет его и атомарно назначает непрерывные `orderIndex` от `0`.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Уже реализованы `POST/GET /components/{id}/images` и `GET /component-images/{id}/content`.
- Слой приложения: `ComponentFacade -> ComponentService -> ComponentRepository / ComponentImageStorage`.
- `component_image.file_path` хранит object key; `order_index` уже существует и не требует изменения схемы.
- Runtime-аутентификация пока отсутствует; endpoint'ы сохраняют текущую OpenAPI security-декларацию проекта.
- Локальное изменение `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — контракт и реализация, затем тесты каждого изменённого слоя.
- Выполнять задачи последовательно небольшими изменениями.
- Для каждой новой и изменённой ветки поведения обязательны success/error tests.
- Все тесты задачи должны пройти до перехода к следующему этапу.
- Поддерживать архитектуру `controller -> facade -> service -> outbound port -> infrastructure`.
- Не редактировать `build/generated/**` вручную.
- Обновлять план при изменении scope или решений.

## Testing Strategy

- Controller unit tests для `204` удаления и `200` перестановки.
- Facade/service unit tests для mapping, validation, archive policy, storage/repository sequencing и ошибок.
- Validator tests для полного набора, положительных уникальных идентификаторов и точного совпадения.
- Repository tests для удаления metadata и транзакционного обновления всех `order_index`.
- Общий local/external integration contract для удаления, перестановки и отказов validation.
- Полный `./gradlew build` и внешний Docker contract согласно Definition of Done.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые обнаруженные задачи отмечаются `[+]`.
- Блокеры отмечаются `[!]`.
- План должен соответствовать фактическому состоянию реализации.

## Solution Overview

1. `DELETE /component-images/{id}` загружает metadata и родительский компонент, запрещает изменение архива, удаляет
   объект из MinIO и только после успеха удаляет metadata.
2. `PUT /components/{id}/images/order` принимает `imageIds` в итоговом порядке.
3. Application validation требует полный текущий набор положительных уникальных идентификаторов без пропусков и
   чужих изображений.
4. Service назначает индексы `0..n-1`, а repository обновляет их внутри одной Spring-транзакции.
5. Ответ перестановки возвращает итоговый список `ComponentImage` в сохранённом порядке.

## Technical Details

- Delete success: `204 No Content`.
- Reorder success: `200` и полный отсортированный массив `ComponentImage`.
- Missing image on delete or missing component: `404`.
- Invalid identifier/order payload or mismatched image set: `400`.
- Archived parent component: `409`.
- MinIO failure during delete: `503`; metadata remains available for retry.
- Delete is storage-first because cross-resource ACID transaction with MinIO is unavailable in the current architecture.
- Reordering an empty gallery with an empty list is valid.
- Database schema and Flyway migrations are unchanged.

## Implementation Steps

### Task 1: Extend the OpenAPI gallery contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] define `DELETE /component-images/{id}` and all response codes
- [x] define `ReorderComponentImagesRequest`
- [x] define `PUT /components/{id}/images/order` with full-set semantics
- [x] regenerate and compile OpenAPI interfaces/DTOs
- [x] verify generated code only through Gradle lifecycle

### Task 2: Add gallery validation and persistence operations

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/validator/ComponentImageValidator.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/validator/ComponentImageValidatorImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ComponentRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ComponentRepositoryImpl.java`
- Modify: corresponding validator and repository tests

- [x] validate positive unique identifiers and exact current image set
- [x] add metadata deletion by image identifier
- [x] add transactional full-order persistence returning deterministic result
- [x] test success, missing row and unchanged data after invalid input
- [x] run validator and repository tests

### Task 3: Implement delete and reorder use cases

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacadeImpl.java`
- Modify: corresponding service and facade tests

- [x] implement storage-first hard delete and metadata removal
- [x] retain metadata when storage deletion fails
- [x] reject delete/reorder for archived components
- [x] implement atomic reorder orchestration and response mapping
- [x] test success, not found, archive, validation and dependency failure paths
- [x] run affected unit tests

### Task 4: Expose thin HTTP controllers

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ComponentImageController.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ComponentController.java`
- Modify: corresponding controller tests

- [x] delegate image deletion to facade and return `204`
- [x] delegate reorder DTO to facade and return `200`
- [x] keep transport validation at the generated interface boundary
- [x] test controller delegation and response payloads
- [x] run controller tests

### Task 5: Extend the shared local/external contract

**Files:**
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractComponentControllerContract.groovy`
- Modify: local/external transport implementations only if required
- Modify: deterministic SQL fixtures only if required

- [x] verify upload then delete removes the image resource and list entry
- [x] verify missing and archived delete responses
- [x] verify full reorder and persisted `0..n-1` indexes
- [x] verify duplicate, omitted, foreign and archived reorder failures leave order unchanged
- [x] verify empty-gallery reorder
- [x] run local integration tests

### Task 6: Verify acceptance criteria

- [x] run `./gradlew :configurator:test`
- [x] run `./gradlew build`
- [x] build and start the external Docker contour
- [x] run `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [x] verify JaCoCo, ArchitectureTest and Spotless
- [x] inspect diff for generated code, credentials and unrelated files

### Task 7: Final documentation

- [x] synchronize completed requirements and decisions
- [x] mark all completed plan items
- [x] move this plan to `docs/plans/completed/`

## Post-Completion

- Commit and push only after owner review or explicit request.
- Reliable asynchronous object cleanup via outbox is intentionally outside the local single-user release scope.
- Runtime authentication/authorization remains a release blocker outside CON1-101.
