# CON1-112 — Component types and attributes UI

## Overview

Реализовать `/settings/types` для управления типами компонентов текущей предметной области и определениями атрибутов
выбранного типа.

## Context

- Domain selection, generated API client, TanStack Query/error foundation и бизнес-формы созданы в 9.11–9.12.
- Доступны CRUD типа и create/list/update атрибута; REST delete атрибута отсутствует.
- Backend отклоняет удаление типа с зависимыми компонентами/атрибутами.
- Runtime authentication не реализована; 9.13 не добавляет фиктивный auth flow.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` вне scope.

## Development approach

- Разместить предметную логику в `features/component-types` и `features/attributes`.
- Включать `domainId` во все query keys; атрибуты дополнительно ключевать по `componentTypeId`.
- Использовать master–detail страницу с локальным preferred type ID и детерминированным fallback.
- Формы реализовать через React Hook Form + Zod и generated request DTO.
- Не добавлять cascade delete и не имитировать отсутствующий attribute delete endpoint.

## Implementation steps

### 1. Requirements and API

- [x] детализировать требования 9.13
- [x] добавить type/attribute query keys, queries и mutations
- [x] добавить детерминированную сортировку

### 2. Forms and UI

- [x] реализовать create/update форму типа
- [x] реализовать create/update форму атрибута с ENUM values
- [x] реализовать адаптивную master–detail страницу
- [x] реализовать delete confirmation и конфликт зависимостей
- [x] заменить placeholder маршрута `/settings/types`

### 3. Verification and documentation

- [x] покрыть API и основные UI-сценарии через MSW
- [x] проверить RU/EN локализацию и mobile layout
- [x] обновить README/AGENTS при изменении актуального функционала
- [x] выполнить `npm run check`, coverage, audit и diff-аудит

## Scope impact

- OpenAPI: без изменений.
- Flyway/jOOQ/БД: без изменений.
- Generated code: без изменений.
- Backend integration contract: без изменений.

## Post-completion

- `npm run check`: 48 tests passed, production build и generated-client drift-check прошли.
- `npm run test:coverage`: 94.36% lines, 94.3% statements.
- `npm audit --audit-level=high`: 0 vulnerabilities.
- Playwright обнаружил 9 smoke-сценариев; их локальный запуск требует отдельной установки browser binaries.
- Desktop/mobile layout проверен встроенным браузером на реальном backend; browser console без ошибок.
- AGENTS.md не менялся: архитектурные и process-инварианты остались актуальными; текущий UI-функционал обновлён в README.
- Commit и push только по отдельной команде владельца.
