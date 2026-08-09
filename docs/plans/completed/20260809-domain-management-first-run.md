# CON1-111 — Domain management and first run

## Overview

Реализовать первый предметный frontend-сценарий: загрузку и выбор предметной области, first-run без обязательного
мастера, создание пустой/демо-области, редактирование и подтверждённое безвозвратное удаление.

## Context

- API уже содержит `GET/POST /domains`, `GET/PUT/DELETE /domains/{id}` и `POST /domains/demo`.
- Server state/error/notification foundation реализован в 9.11.
- `domainId` по принятому решению не входит в frontend URL.
- Backend запрещает удаление области с зависимыми сущностями и возвращает структурированный `409`.
- Runtime authentication не реализована; frontend не добавляет фиктивный auth flow.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` вне scope.

## Development Approach

- Загружать все страницы списка в один TanStack Query cache entry; не копировать DTO в context/store.
- Хранить только preferred selected ID в localStorage и автоматически восстанавливать корректный fallback.
- Использовать generated SDK с `throwOnError: true` через `apiRequest`.
- Применить React Hook Form + Zod к create/update и server field details к ошибкам формы.
- Не реализовывать client-side cascade delete и не менять backend contract.

## Implementation Steps

### 1. Requirements and dependencies

- [x] детализировать требования 9.12
- [x] добавить React Hook Form, Zod и resolver
- [x] документировать domain feature architecture

### 2. Domain state and API

- [x] добавить query keys и полную постраничную загрузку
- [x] добавить create/demo/update/delete mutations
- [x] добавить provider выбранной области и persistence/fallback policy

### 3. User interface

- [x] заменить header placeholder на доступный domain selector
- [x] добавить reusable create/update form modal
- [x] добавить first-run состояние и создание demo
- [x] реализовать страницу управления и delete confirmation
- [x] закрыть domain-dependent placeholder routes first-run guard

### 4. Verification

- [x] покрыть API, context, first-run и CRUD component tests через MSW
- [x] проверить ru/en локализацию и mobile layout
- [x] выполнить `npm run check`, coverage и audit
- [x] проверить diff на generated, backend и unrelated файлы

Playwright browser matrix описана шестью E2E-сценариями и успешно обнаруживается через `--list`, но локально не
выполнена: browser binaries для Playwright 1.62.1 не установлены. Mobile viewport дополнительно проверен визуально во
встроенном браузере на `390×844` с реальным локальным backend; console errors отсутствуют.

## Scope impact

- OpenAPI: без изменений.
- Flyway/jOOQ/БД: без изменений.
- Generated code: без изменений.
- Backend integration contract: без изменений.

## Post-Completion

- Переместить план в `docs/plans/completed/` после quality gate.
- Commit и push только по отдельной команде владельца.
