# CON1-110 — Server state, errors and shared UI states

## Overview

Подготовить общий frontend-фундамент для всех server-driven экранов Epic 9: TanStack Query, безопасную модель ошибок,
уведомления и доступные повторно используемые состояния загрузки, пустого результата и ошибки.

## Context

- REST-модели и SDK генерируются из `specs/configurator-api.yaml` и не редактируются вручную.
- Backend уже возвращает структурированный `ErrorResponse` с `code` и `details`.
- AppShell, маршрутизация, темы и ru/en локализация реализованы в 9.10.
- Предметные запросы и выбор домена относятся к 9.12; auth interceptor реализуется вместе с runtime security.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- Не скрывать generated client за вторым набором ручных endpoint-функций.
- Держать transport error normalization в `shared/api`, query policy — в application provider, визуальные состояния —
  в `shared/ui`.
- Не повторять мутации автоматически и не показывать внутренние browser/JavaScript diagnostics пользователю.
- Уведомлять об ошибке query глобально только при фоновом обновлении уже отображённых данных.

## Implementation Steps

### 1. Requirements and dependencies

- [x] детализировать требования 9.11
- [x] добавить совместимые версии TanStack Query и Mantine Notifications
- [x] задокументировать frontend foundation

### 2. Server-state and error foundation

- [x] добавить `QueryClient` factory и application provider
- [x] реализовать нормализацию API, network и unknown errors
- [x] реализовать retry policy и извлечение field errors
- [x] добавить централизованные mutation/background-query notifications

### 3. Shared UI states

- [x] добавить `PageHeader`, loading, empty и error components
- [x] добавить композиционный `ServerDataState`
- [x] добавить безопасный route error fallback
- [x] локализовать все новые пользовательские строки

### 4. Verification

- [x] покрыть error/query policy unit tests
- [x] покрыть shared state и route fallback component tests
- [x] выполнить `npm run check` и `npm run test:coverage`
- [x] выполнить audit и проверить diff на generated/unrelated файлы

Playwright обнаружил три smoke-сценария. Их browser run был запущен, но локальные Chromium/Firefox/WebKit binaries не
установлены; установка и полный E2E остаются отдельным явным шагом, как определено toolchain-требованиями 9.8.

## Scope impact

- OpenAPI: без изменений.
- Flyway/jOOQ/БД: без изменений.
- Generated code: без изменений.
- Backend integration contract: не требуется; используется существующий `ErrorResponse`.

## Post-Completion

- Переместить план в `docs/plans/completed/` после прохождения quality gate.
- Commit и push выполнять только по отдельной команде владельца.
