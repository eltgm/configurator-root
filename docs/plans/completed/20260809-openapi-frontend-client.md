# CON1-108 — Generated frontend OpenAPI client

## Overview

Настроить воспроизводимую генерацию типизированного frontend-клиента из `specs/configurator-api.yaml` без ручного
дублирования REST DTO и endpoint-контрактов.

## Pre-change check

- OpenAPI source не меняется: задача только читает существующую спецификацию.
- Flyway, jOOQ и backend не меняются; generated backend code не пересоздаётся.
- Frontend boundary: feature-код импортирует API только из `src/shared/api`.
- Общий backend integration contract не требуется; runtime клиента проверяется Vitest/MSW.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Architecture decision

- Generator: `@hey-api/openapi-ts` с официальной поддержкой TypeScript 6.
- Output: committed `src/shared/api/generated`, который полностью принадлежит generator.
- Runtime: встроенный Fetch client; отдельный deprecated `@hey-api/client-fetch` package не используется.
- Base URL: `/api`, совпадает с Vite proxy и будущей same-origin поставкой.
- Type isolation: generator-owned output получает `@ts-nocheck`, потому что bundled client templates не совместимы с
  `exactOptionalPropertyTypes`; строгая проверка handwritten boundary и потребителей сохраняется.
- Security: узкий npm override обновляет только уязвимый `js-yaml` внутри parser до исправленной версии.
- Drift: отдельный Node script генерирует output во временный каталог и побайтово сравнивает его с committed output.

## Implementation steps

### Task 1: Document contract

- [x] актуализировать требования 9.9
- [x] зафиксировать source of truth, scope и архитектурное решение

### Task 2: Configure code generation

- [x] добавить generator dependency и security override
- [x] добавить `openapi-ts.config.ts`
- [x] добавить `api:generate` и `api:check`
- [x] подключить drift-check к `npm run check`

### Task 3: Generate client

- [x] сгенерировать TypeScript models
- [x] сгенерировать typed SDK functions
- [x] сгенерировать bundled Fetch client с `baseUrl=/api`
- [x] добавить handwritten API boundary

### Task 4: Verify behavior

- [x] проверить base URL и typed success response
- [x] проверить сериализацию query parameters через MSW
- [x] проверить детерминированность повторной генерации

### Task 5: Quality and documentation

- [x] выполнить `npm ci`
- [x] выполнить `npm run api:check`
- [x] выполнить `npm run test:coverage`
- [x] выполнить `npm run check`
- [x] выполнить `npm audit --audit-level=high`
- [x] актуализировать frontend README и AGENTS.md
- [x] проверить Git diff и сохранить unrelated local changes
- [x] завершить и переместить план в `docs/plans/completed/`

## Deferred

- TanStack Query integration and common API error policy: 9.11.
- Authentication token injection: security task after UI foundation.
- CI workflow integration: 9.30.
