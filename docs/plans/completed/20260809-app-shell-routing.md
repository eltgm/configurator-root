# CON1-109 — Routing, AppShell, themes and localization

## Overview

Создать навигационный каркас frontend: стабильные маршруты, адаптивный Mantine AppShell, светлую/тёмную/системную
тему и расширяемую ru/en локализацию без преждевременной загрузки предметных данных.

## Pre-change check

- OpenAPI, backend, Flyway и jOOQ не меняются.
- Generated API client не пересоздаётся, кроме обязательного drift-check общего frontend quality gate.
- Новых integration contracts не требуется; поведение проверяется Vitest/Testing Library.
- `testcontainers.properties` с локальным Docker socket находится вне scope.

## Architecture decision

- React Router 7.18.2 используется как актуальная stable-версия; заявленная ранее версия 8 отсутствует в npm.
- Маршруты не содержат `domainId`: предметная область является переключаемым глобальным контекстом.
- Route objects отделены от Router instance, чтобы production использовал browser history, а tests — memory history.
- App providers владеют Mantine theme и i18next; feature pages не настраивают глобальные providers.
- Desktop и mobile navigation используют одну декларативную модель, исключая расхождение путей и переводов.
- Настройки темы и языка имеют version-stable localStorage keys.

## Implementation steps

### Task 1: Requirements and dependencies

- [x] актуализировать требования 9.10
- [x] проверить stable package versions
- [x] установить Router, Mantine, icons и i18next exact versions

### Task 2: Application foundation

- [x] добавить Mantine/i18next providers
- [x] определить route objects и browser router
- [x] добавить redirect, placeholder routes и 404

### Task 3: Responsive shell

- [x] реализовать header и domain placeholder
- [x] реализовать desktop sidebar со вложенными settings
- [x] реализовать mobile bottom navigation
- [x] обеспечить active и keyboard-accessible states

### Task 4: Preferences

- [x] реализовать system/light/dark theme menu и persistence
- [x] реализовать ru/en locale menu и persistence
- [x] вынести все строки shell в translation resources

### Task 5: Tests and quality

- [x] покрыть redirect и основные маршруты
- [x] покрыть desktop/mobile navigation и 404
- [x] покрыть theme and locale switching
- [x] выполнить `npm ci`
- [x] выполнить `npm run test:coverage`
- [x] выполнить `npm run check`
- [x] выполнить `npm audit --audit-level=high`

### Task 6: Documentation

- [x] актуализировать frontend README и AGENTS.md
- [x] проверить Git diff и unrelated changes
- [x] завершить и переместить план в `docs/plans/completed/`

## Deferred

- Domain loading/switching and first-run flow: 9.12.
- Server state, notifications and API errors: 9.11.
- Final accessibility, E2E and visual regression hardening: 9.26–9.27.
