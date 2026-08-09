# CON1-105 — Structured API errors

## Overview

Добавить стабильные машинно-читаемые коды и структурированные детали ко всем JSON-ответам об ошибках. Frontend сможет
выбирать пользовательский сценарий по `code` и привязывать ошибки валидации к полям по `details`, не разбирая текст
`message`.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Текущий `ErrorResponse` содержит только `timestamp`, `status`, `error`, `message`, `path`.
- `ControllerExceptionHandler` централизованно отображает domain и transport exceptions в HTTP.
- Ошибки Bean Validation сейчас сортируются и объединяются в одну строку.
- Существующие пять полей и безопасное логирование ошибок должны сохраниться.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — контракт и реализация, затем unit- и integration-тесты.
- Сначала изменить OpenAPI, generated DTO получить только через Gradle lifecycle.
- Сохранить HTTP status и существующие человекочитаемые сообщения для обратной совместимости.
- Не переносить transport error codes в domain exceptions и не нарушать REST boundary.
- Не включать rejected values, stack traces и внутренние причины в публичный ответ.

## Testing Strategy

- Unit-тесты `ControllerExceptionHandler` для каждого верхнеуровневого кода и каждой transport validation detail.
- Shared local/external integration contract для бизнес-конфликта, not found, field validation и malformed body.
- Полный `./gradlew build` и внешний integration contract.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или решений.

## Solution Overview

1. Расширить `ErrorResponse` обязательными `code` и `details`.
2. Описать закрытый набор верхнеуровневых `ApiErrorCode` в OpenAPI.
3. Представить `details` массивом `ApiErrorDetail` с необязательным `field`, стабильным `code` и сообщением.
4. Централизованно отображать concrete exception types в API-коды внутри REST advice.
5. Извлекать отдельные детали Spring/Bean Validation с детерминированной сортировкой и дедупликацией.

## Technical Details

- `details` всегда присутствует и равен пустому массиву для ошибок без дополнительных деталей.
- Верхнеуровневые коды: `BUSINESS_ERROR`, `INTERNAL_ERROR`, `NOT_FOUND`, `ENTITY_ALREADY_EXISTS`,
  `ENTITY_HAS_RELATED_ENTITIES`, `COMPONENT_ARCHIVED`, `CONFIGURATION_CONFLICT`, `VALIDATION_ERROR`,
  `IMAGE_TOO_LARGE`, `UNSUPPORTED_IMAGE_FORMAT`, `EXTERNAL_STORAGE_UNAVAILABLE`.
- Constraint code преобразуется в `UPPER_SNAKE_CASE`; transport codes включают `TYPE_MISMATCH`,
  `MISSING_PARAMETER`, `MISSING_PART`, `MALFORMED_REQUEST` и fallback `INVALID_VALUE`.
- Поле детали nullable/optional для object-level и malformed-body ошибок.
- БД, Flyway и jOOQ не меняются.

## Implementation Steps

### Task 1: Extend and document the public error contract

**Files:**
- Modify: `specs/configurator-api.yaml`
- Modify: `docs/requirements/epic-9-frontend.md`

- [x] define `ApiErrorCode`, `ApiErrorDetail` and extended `ErrorResponse`
- [x] document compatibility, security and deterministic ordering rules
- [x] regenerate and compile OpenAPI DTOs through Gradle
- [x] verify generated code is not edited or committed

### Task 2: Map exceptions to structured errors

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/advice/ControllerExceptionHandler.java`
- Modify: handler unit tests

- [x] map every currently handled exception to a stable top-level code
- [x] extract field, constraint and transport validation details
- [x] keep legacy message/status behavior and sanitized infrastructure errors
- [x] test every code and detail extraction branch
- [x] run affected unit tests

### Task 3: Extend shared local/external integration contract

**Files:**
- Modify: shared integration contract specs

- [x] verify required structured fields for representative errors
- [x] verify field validation detail and normalized constraint code
- [x] verify malformed-body detail
- [x] verify business conflict/not-found codes and empty details
- [x] run local integration tests

### Task 4: Verify acceptance criteria

- [x] run `./gradlew :configurator:test`
- [x] run `./gradlew build`
- [x] run external Docker integration contract
- [x] verify JaCoCo, ArchitectureTest and Spotless
- [x] inspect diff for generated code, credentials and unrelated files

### Task 5: Final documentation

- [x] synchronize requirements with implementation
- [x] mark completed plan items
- [x] move plan to `docs/plans/completed/`

## Post-Completion

- Commit and push only after owner review or explicit request.
- Runtime authentication/authorization remains outside CON1-105.
