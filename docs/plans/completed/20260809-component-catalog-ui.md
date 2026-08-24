# CON1-113 — Component catalog UI

## Overview

Реализовать `/components` как рабочий каталог выбранной предметной области: server-side поиск, фильтр типа,
активный каталог/архив, пагинацию и переключаемые карточки/таблицу. Архивирование и восстановление замыкают
пользовательский сценарий 9.14.

## Context

- Existing foundation: generated OpenAPI client, TanStack Query/AppError, domain selection, types cache, AppShell и i18n.
- REST: paged `GET /domains/{domainId}/components`, archive `DELETE /components/{id}`, restore
  `POST /components/{id}/restore`.
- `Component` уже содержит attributes и ordered image metadata; image content получается через backend URL.
- Runtime authentication не реализована; 9.14 не добавляет фиктивную защиту.
- Локальный `configurator-integration-tests/src/test/resources/testcontainers.properties` вне scope.

## Development Approach

- **Testing approach:** regular — небольшие законченные слои, затем unit/component tests перед следующим слоем.
- Не дублировать generated DTO и не редактировать generated code.
- Не включать create/edit/detail и gallery management из 9.15–9.16.
- Поддерживать план в актуальном состоянии и перенести в `completed` после quality gate.

## Testing Strategy

- API unit tests: query parameters, query keys, content URL, archive/restore cache invalidation.
- Component tests with MSW: filters, pagination, both representations, persisted view, empty/error, archive/restore.
- Playwright smoke: deterministic loaded catalog and mobile table-to-list representation.
- Quality gate: `npm run check`, `npm run test:coverage`, `npm audit --audit-level=high`, E2E when browser binaries exist.

## Solution Overview

- `features/components/api`: typed query parameters, paged query and mutations.
- `features/components/model`: view-mode persistence and small presentation helpers.
- `features/components/ui`: filter toolbar and reusable cards/table/list representations.
- `pages/ComponentsPage`: orchestration of domain, filters, page, archive confirmation and responsive representation.

## Technical Details

- Query key: `['domains', domainId, 'components', { componentTypeId, name, archived, page, size }]`.
- UI page — one-based; REST page — zero-based. Fixed page size: 12.
- Search is trimmed and debounced; changing filters resets page to 1.
- `archived=false` is always sent for the active catalog, `archived=true` for archive.
- Mutations invalidate every component query below the current domain prefix so both active/archive views stay coherent.
- Component image URL is normalized to the configured `/api` boundary without direct MinIO access.

## Implementation Steps

### Task 1: Component catalog API and view state

**Files:**

- Create: `configurator-web/src/features/components/api/components.ts`
- Create: `configurator-web/src/features/components/api/components.test.tsx`
- Create: `configurator-web/src/features/components/model/catalog-preferences.ts`
- Modify: `configurator-web/src/shared/config/preferences.ts`

- [x] implement paged component query with fully scoped query keys
- [x] implement archive/restore mutations and domain-prefix invalidation
- [x] implement persisted `cards | table` preference and backend image URL normalization
- [x] cover query parameters, sorting helpers, storage fallback and mutation behavior with tests
- [x] run targeted tests before Task 2 (5 passed)

### Task 2: Responsive catalog page

**Files:**

- Create: `configurator-web/src/pages/ComponentsPage.tsx`
- Create: `configurator-web/src/pages/components-page.module.css`
- Create: `configurator-web/src/features/components/ui/ComponentCatalog.test.tsx`
- Create: `configurator-web/src/features/components/ui/ComponentCatalogContent.tsx`
- Create: `configurator-web/src/features/components/ui/component-catalog-content.module.css`
- Modify: `configurator-web/src/app/router/routes.tsx`
- Modify: `configurator-web/src/shared/i18n/resources.ts`

- [x] implement search, type, active/archive controls and page reset rules
- [x] implement shared card/table data with desktop table and compact mobile list
- [x] implement initial/loading/background/error/empty/pagination states
- [x] implement archive confirmation and immediate restore action
- [x] add RU/EN localization and replace `/components` placeholder
- [x] cover loaded, filtered, paged, persisted view, empty/error and archive/restore scenarios with MSW
- [x] run targeted tests before Task 3 (13 passed)

### Task 3: Acceptance and documentation

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`
- Modify: `README.md`
- Modify: `configurator-web/README.md`
- Modify: `docs/requirements/epic-9-frontend.md`
- Move: this plan to `docs/plans/completed/`

- [x] add deterministic catalog E2E smoke including mobile compact list
- [x] verify desktop/mobile layout and browser console
- [x] run `npm run check`, coverage, audit and E2E where available
- [x] verify no OpenAPI/backend/DB/generated or unrelated local changes are included
- [x] update documentation and complete the plan

## Verification Results

- `npm run check`: generated-client drift-check, format, lint, Stylelint, 61 tests and production build passed.
- `npm run test:coverage`: 93.94% lines, 93.92% statements.
- `npm audit --audit-level=high`: 0 vulnerabilities.
- `npx playwright test --list`: 12 scenarios discovered; execution requires local `npx playwright install`.
- Desktop/mobile empty catalog and responsive filters were checked with the real backend; browser console has no errors.
- Populated cards/table, filter, pagination, archive and restore flows are covered deterministically through MSW.
- AGENTS.md remains current; README files describe the new catalog scope.

## Post-Completion

- Commit and push only after an explicit owner request.
- Runtime authentication/authorization remains a release blocker.
