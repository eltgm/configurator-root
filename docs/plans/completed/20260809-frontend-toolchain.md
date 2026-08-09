# CON1-107 — Frontend toolchain

## Overview

Создать независимый frontend-проект `configurator-web` на React/Vite с воспроизводимыми зависимостями и полным
локальным quality gate. Задача формирует технический фундамент без реализации будущей маршрутизации и предметных
экранов.

## Context (from discovery)

- Frontend-проект и `.openai/hosting.json` отсутствуют.
- Локально доступны Node `24.10.0` и npm `11.6.0`.
- npm registry: React `19.2.8`, Vite `8.2.1`, TypeScript `7.0.2`.
- Актуальный typescript-eslint `8.66.0` поддерживает TypeScript `<6.1`, поэтому выбрана последняя совместимая версия
  TypeScript `6.0.3`.
- jsdom 30 требует Node `24.15+`; для заявленного Node 24 LTS выбрана совместимая линия jsdom 29.
- `configurator-web` не является Gradle-модулем; OpenAPI и БД не меняются.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` находится вне scope.

## Development Approach

- **Testing approach:** Regular — scaffold и конфигурация, затем unit/smoke tests.
- Все зависимости фиксируются точными версиями и `package-lock.json`.
- Не подключать Router, Mantine, Query и OpenAPI generator до задач, в которых они используются.
- TypeScript strict и отдельные lint/typecheck/test/build команды объединяются в `npm run check`.
- Browser binaries Playwright не скачиваются автоматически при `npm ci`.

## Testing Strategy

- Vitest/Testing Library проверяют минимальный React entrypoint.
- MSW server lifecycle подключается общим setup и запрещает необработанные запросы.
- Playwright smoke spec проверяет загрузку приложения во всех трёх browser projects.
- Реально выполняются format, lint, stylelint, typecheck, unit tests, coverage и production build.

## Progress Tracking

- Завершённые пункты отмечаются `[x]`.
- Новые задачи отмечаются `[+]`, блокеры — `[!]`.
- План обновляется при изменении scope или совместимости зависимостей.

## Solution Overview

1. Создать npm package с Node/npm engines и lock-файлом.
2. Настроить React 19.2, TypeScript 6.0 strict и Vite 8.2.
3. Настроить ESLint flat config, Prettier и Stylelint.
4. Настроить Vitest, Testing Library, MSW и Playwright.
5. Добавить минимальный доступный экран, unit test и E2E smoke spec.
6. Документировать developer workflow и frontend-правила для AI-агентов.

## Technical Details

- Source alias: `@/` -> `src/`.
- Dev URL: `http://127.0.0.1:5173`.
- Dev API proxy: `/api/*` -> `http://127.0.0.1:8080/*`.
- Output: `dist/`; reports: `coverage/`, `playwright-report/`, `test-results/`.
- Package type: ESM.
- OpenAPI, backend, Docker delivery and CI workflow remain unchanged in 9.8.

## Implementation Steps

### Task 1: Create reproducible npm package

**Files:**
- Create: `configurator-web/package.json`
- Create: `configurator-web/package-lock.json`
- Create: Node version files
- Modify: `.gitignore`

- [x] define scripts and Node/npm engines
- [x] install exact compatible dependency versions
- [x] generate npm lockfile
- [x] verify clean install with `npm ci`

### Task 2: Configure React, TypeScript and Vite

**Files:**
- Create: TypeScript and Vite configs
- Create: `index.html`, `src/main.tsx`, `src/app/App.tsx`

- [x] enable strict project references and no-emit typecheck
- [x] configure React plugin, alias and API proxy
- [x] create accessible Russian entry screen
- [x] verify production build

### Task 3: Configure static quality tools

**Files:**
- Create: ESLint flat config
- Create: Prettier and Stylelint configs

- [x] configure TypeScript/React ESLint rules
- [x] configure CSS linting
- [x] configure deterministic formatting
- [x] run lint, stylelint and format check

### Task 4: Configure automated tests

**Files:**
- Create: Vitest setup and unit test
- Create: MSW server setup
- Create: Playwright config and smoke spec

- [x] configure jsdom and Testing Library
- [x] enforce strict unhandled-request behavior in MSW
- [x] configure Chromium, Firefox and WebKit projects
- [x] run unit tests and coverage
- [x] validate Playwright test discovery without downloading browsers

### Task 5: Verify acceptance criteria

- [x] run `npm ci`
- [x] run `npm run check`
- [x] run `npm run test:coverage`
- [x] run `npm run test:e2e -- --list`
- [x] inspect dependency audit
- [x] inspect Git diff for generated reports, credentials and unrelated files

### Task 6: Final documentation

- [x] add `configurator-web/README.md`
- [x] update `AGENTS.md` with frontend commands and boundaries
- [x] synchronize requirements with actual versions
- [x] mark completed plan items
- [x] move plan to `docs/plans/completed/`

## Post-Completion

- CI integration is implemented in 9.30.
- OpenAPI client generation is implemented in 9.9.
- Commit and push only after owner review or explicit request.
