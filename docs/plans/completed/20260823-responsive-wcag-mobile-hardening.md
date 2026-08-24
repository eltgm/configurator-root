# CON1-125 — Responsive, WCAG 2.2 AA and Mobile Hardening

## Overview

Пункт 9.26 завершает адаптацию существующего frontend для desktop, tablet и phone и проводит системную проверку
доступности по WCAG 2.2 уровня AA. Задача не добавляет новые бизнес-сценарии: она укрепляет AppShell, общие UI-
компоненты и все реализованные страницы эпика 9 так, чтобы ими можно было пользоваться с клавиатуры, screen reader,
увеличением масштаба и на узком экране в светлой и тёмной теме.

Реализация остаётся frontend-only. OpenAPI, backend, Flyway, jOOQ, БД и generated API client изменять не требуется.
Автоматизированный axe-аудит и visual regression остаются отдельной задачей 9.27; в 9.26 добавляются необходимые
поведенческие unit/component и responsive Playwright-проверки, а также ручная матрица соответствия.

## Context (from discovery)

- `AppShell` уже содержит skip link, semantic `main`, desktop navbar, mobile bottom navigation и safe-area отступы.
- Глобальный `:focus-visible` существует, loading/status/error состояния в основном семантически размечены, а
  пользовательские действия имеют accessible names.
- Каталог переключает таблицу на мобильный список; формы, карточки и compatibility screens уже используют responsive
  breakpoints, но не проходили единый аудит на 320 px, zoom/reflow, ориентацию и перекрытие focus фиксированными
  header/footer.
- В `global.css` задано `body { min-width: 360px; }`, а требования называют 360 px минимальной шириной. Это
  несовместимо с WCAG 2.2 SC 1.4.10 Reflow: уровень AA требует отсутствие двумерной прокрутки при ширине контента,
  эквивалентной 320 CSS px, кроме действительно двумерных представлений.
- `.main` использует `overflow-x: clip`. Он может скрывать симптом переполнения и focus outline вместо исправления
  конкретного responsive layout.
- Header и mobile footer фиксированы, но общего `scroll-padding`/`scroll-margin` для SC 2.4.11 Focus Not Obscured нет.
- Compatibility graph построен на React Flow. Узлы сейчас можно перетаскивать мышью, хотя координаты не сохраняются и
  для предметного сценария drag не нужен. Это создаёт лишнюю обязанность предоставить одноуказательную альтернативу по
  SC 2.5.7 Dragging Movements.
- Graph canvas является по природе двумерным содержимым и может использовать исключение Reflow. Поиск, fit/reset,
  controls и доступная панель деталей должны оставаться полноценной недрагговой альтернативой для изучения графа.
- Отдельного axe-инструментария и visual snapshot baseline пока нет. Их добавление запланировано в 9.27.
- Runtime-аутентификация отсутствует. Критерии доступной аутентификации пока неприменимы и должны быть повторно
  проверены при реализации auth UI.
- Пользовательский файл `configurator-integration-tests/src/test/resources/testcontainers.properties` не относится к
  задаче и не должен редактироваться, форматироваться, индексироваться или попадать в commit.

Normative references:

- WCAG 2.2 Recommendation: https://www.w3.org/TR/WCAG22/
- WCAG 2.2 Quick Reference: https://www.w3.org/WAI/WCAG22/quickref/
- New success criteria in WCAG 2.2: https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/

## Proposed Product Contract

1. Основной поддерживаемый пользовательский phone viewport остаётся 360 px, но технический reflow baseline меняется
   на 320 CSS px, чтобы требование WCAG 2.2 AA было непротиворечивым.
2. На 320 px страница не получает горизонтальную прокрутку и не обрезает интерактивный контент. Исключения возможны
   только для явно обозначенных двумерных областей, например graph canvas; рядом остаётся доступная линейная
   альтернатива.
3. Поддерживаются текущие Chrome, Edge, Firefox и Safari, portrait/landscape, desktop/tablet/phone, светлая, тёмная и
   системная тема.
4. Все сценарии доступны с клавиатуры: логичный tab order, отсутствие keyboard trap, видимый focus, возврат focus из
   modal/drawer и skip link к основному содержимому.
5. Focused control не может быть полностью скрыт фиксированными header, mobile footer, sticky actions, modal или
   author-created overlay.
6. Все interactive targets имеют минимум 24 x 24 CSS px либо достаточное расстояние/допустимое исключение по WCAG
   2.5.8. Для основных мобильных действий используется практический размер 44 x 44 px, где это не ухудшает плотность.
7. Функции не требуют drag. Перетаскивание узлов compatibility graph отключается как непредметное действие; pan/zoom
   сохраняются вместе с controls, поиском, fit/reset и доступной панелью деталей.
8. Название, роль, состояние и value каждого контрола доступны assistive technologies; visible label входит в
   accessible name. Icon-only controls имеют локализованный `aria-label` и tooltip, если значение иконки не очевидно.
9. Структура страниц использует landmarks и последовательную heading hierarchy. После SPA-перехода изменение страницы
   объявляется без неожиданного перемещения пользовательского focus; skip link остаётся доступным.
10. Loading, empty, success, validation, conflict и request-error состояния не передаются одним цветом. Важные status
    messages объявляются через подходящий `status`/`alert`/live region без лишнего повторения.
11. Формы сохраняют видимые labels, help/error association, field-level backend errors, summary для общих ошибок,
    корректный required/invalid state и предсказуемый focus после submit.
12. Текст, не-текстовые controls, focus indicator, disabled state и графические элементы сохраняют необходимый контраст
    в светлой и тёмной теме; интерфейс не ломается при пользовательском text spacing и увеличении до 200%.
13. Изображения получают содержательный alt только когда несут информацию; декоративные previews не дублируются screen
    reader. Gallery navigation и reorder доступны без жестов и drag.
14. Responsive presentation не меняет бизнес-семантику: desktop table и mobile cards показывают одинаковые ключевые
    данные и действия, а destructive confirmations и unsaved-change guards сохраняются на любом viewport.
15. WCAG AA трактуется как соответствие всем применимым критериям уровней A и AA, а не только новым критериям 2.2.
    Для каждого критерия ведётся evidence/status matrix и список известных ограничений.
16. Документация говорит об internal implementation target/audit, а не о формальной сертификации: автоматические тесты
    не заменяют ручную проверку клавиатурой, screen reader, zoom, contrast и responsive behavior.

## Considered Approaches

### A. Systematic hardening with a manual audit matrix — selected

- Исправить общий shell и primitives, затем пройти все feature groups на 320/360/tablet/desktop.
- Создать полную A/AA applicability/evidence matrix и закрыть найденные gaps тестами соответствующего уровня.
- Использовать текущие Testing Library и Playwright для semantic, keyboard и responsive contracts.
- Оставить axe integration и visual baselines в 9.27, чтобы задачи имели ясные границы.
- Недостаток: часть критериев требует ручного evidence до появления автоматизированного axe-аудита.

### B. Add axe and visual regression in 9.26

- Даёт раннюю автоматическую проверку типовых нарушений.
- Дублирует основной scope 9.27, увеличивает объём изменения toolchain и смешивает исправление UI с построением новой
  quality platform.
- Не выбран; 9.26 подготавливает семантику и стабильные responsive states, которые 9.27 затем закрепит.

### C. Limit the task to CSS mobile polish

- Самый небольшой объём изменения.
- Не проверяет keyboard, focus, semantics, status messages, contrast, pointer alternatives и формы; заявить WCAG 2.2 AA
  после такого изменения нельзя.
- Не рассматривается для реализации.

## Solution Overview

```text
WCAG A/AA inventory + viewport inventory
                    |
                    +--> AppShell / reflow / focus foundations
                    |
                    +--> shared controls / states / forms / overlays
                    |
                    +--> CRUD + catalog + details + gallery
                    |
                    +--> compatibility + configurator + configurations
                    |
                    +--> component tests + responsive Playwright + manual evidence
```

Изменение начинается с критериев, которые влияют на весь frontend: 320 px reflow, focus not obscured, target size,
landmarks, route announcement и shared status/form semantics. После этого feature pages проверяются группами, чтобы
исправления не превращались в локальные CSS patches с разными правилами.

## Technical Decisions

- Удалить глобальный `min-width: 360px`; 360 px оставить в требованиях как основной продуктовый phone target, 320 px —
  как обязательный WCAG reflow test.
- Убрать маскировку общего overflow через `overflow-x: clip` после исправления конкретных источников переполнения.
  Разрешённые горизонтальные/двумерные области должны быть локальными, keyboard-accessible и явно обозначенными.
- Добавить shell-aware scroll offsets для fixed header/mobile footer и проверить focus placement после validation,
  modal/drawer close и навигации.
- Реализовать ненавязчивое SPA page announcement на основе локализованного route title. Не сбрасывать focus с контрола,
  которым пользователь инициировал переход, если в этом нет необходимости.
- Не вводить вторую design system: исправления выполняются через Mantine theme/tokens, существующие shared UI и CSS
  modules.
- Отключить `nodesDraggable` в React Flow. Edge/node selection, search, accessible details, pan/zoom buttons и reset/fit
  остаются.
- Не дублировать transport DTO и не менять generated API client.
- Для responsive E2E выделить behavioural checks без screenshot baselines и axe scan; эти слои добавит 9.27.

## Testing Strategy

- **Static/manual matrix:** все WCAG 2.2 A/AA criteria получают applicability, implementation, evidence, status и
  follow-up. N/A должен иметь объяснение.
- **Unit/model/component:** landmarks/headings, accessible names/states/descriptions, live statuses, form errors,
  keyboard order, modal/drawer focus return, theme/locale semantics и graph without drag.
- **Responsive component checks:** отсутствие недоступных действий при переключении table/cards, compact header,
  stacking actions, long translated content и 320 px layout contracts.
- **Playwright behavioural E2E:** 320 x 568, 360 x 800, representative tablet и desktop; keyboard-only core journey,
  visible/focus-not-obscured assertions, no unexpected document overflow, portrait/landscape and light/dark smoke in
  Chromium, Firefox and WebKit.
- **Manual browser QA:** 200% zoom, text spacing, keyboard, VoiceOver on macOS/Safari, contrast/focus in both themes,
  responsive orientation and touch-size sampling.
- **Deferred to 9.27:** `axe-core` integration, automated accessibility gates, screenshot baselines and visual regression
  workflow.
- **Required verification:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`,
  `npm run test:e2e`, `git diff --check`.
- Backend build and external integration are not required if OpenAPI/backend/Docker delivery remain unchanged.

## Implementation Steps

### Task 1: Finalize requirements and build the WCAG evidence baseline

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Create: `docs/accessibility/WCAG_2_2_AA_AUDIT.md`
- Modify: `docs/plans/completed/20260823-responsive-wcag-mobile-hardening.md`

- [x] добавить подробный раздел 9.26 с viewport, keyboard, focus, contrast, semantics и responsive acceptance criteria
- [x] устранить конфликт требований 360/320 px и явно описать исключения двумерного content
- [x] перечислить все A/AA criteria, определить applicability и исходные gaps/evidence
- [x] зафиксировать границу с 9.27 и отсутствие формального certification claim

### Task 2: Harden AppShell, reflow and navigation semantics

**Files:**

- Modify: `configurator-web/src/styles/global.css`
- Modify: `configurator-web/src/app/layout/AppShellLayout.tsx`
- Modify: `configurator-web/src/app/layout/app-shell-layout.module.css`
- Modify as needed: `configurator-web/src/app/router/*`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify/add focused AppShell tests

- [x] поддержать 320 px без global min-width и скрытого overflow
- [x] сделать header/domain selector/preferences и mobile bottom navigation устойчивыми к длинному тексту и safe areas
- [x] исключить перекрытие focus fixed header/footer и сохранить рабочий skip link
- [x] проверить landmarks, heading/page title consistency и ненавязчивое объявление SPA route
- [x] покрыть keyboard navigation, focus and locale/theme behavior component tests

### Task 3: Strengthen shared UI, overlays, forms and status messages

**Files:**

- Modify as needed: `configurator-web/src/shared/ui/*`
- Modify as needed: shared form/error/notification adapters under `configurator-web/src/shared`
- Modify: `configurator-web/src/shared/i18n/resources.ts`
- Modify/add focused shared UI tests

- [x] унифицировать accessible names, descriptions, disabled reasons и target sizes
- [x] проверить loading/empty/success/error/live-region semantics без color-only indication
- [x] проверить modal/drawer keyboard trap, initial focus, close and focus return
- [x] проверить form field association, required/invalid state, backend field details и submit focus behavior
- [ ] проверить light/dark contrast tokens, focus indicator, text spacing and reduced motion behavior

### Task 4: Complete responsive and accessibility sweep for management screens

**Files:**

- Modify as needed: domain/settings/component-type/attribute pages and feature UI
- Modify as needed: `configurator-web/src/pages/*.module.css`
- Modify/add co-located component tests

- [x] проверить first-run/domain switching and settings pages at 320/360/tablet/desktop
- [x] сделать lists/tables/forms/actions эквивалентными по данным и функциям на desktop/mobile
- [x] проверить long names, validation errors, empty/error/loading states and destructive confirmations
- [x] сохранить unsaved-change guards и focus restoration на узком экране
- [x] обновить локализованные labels/help/status при найденных semantic gaps

### Task 5: Complete responsive and accessibility sweep for catalog and component details

**Files:**

- Modify as needed: `configurator-web/src/features/components/ui/*`
- Modify as needed: catalog/details page CSS modules
- Modify/add co-located tests

- [x] проверить cards/table parity, filters, pagination and action menus
- [x] адаптировать detail/form layouts, attribute values and long content to 320 px
- [x] проверить gallery alt semantics, lightbox focus, upload/delete/reorder without drag and touch targets
- [x] исключить clipped focus/menus and unexpected horizontal scrolling
- [x] покрыть responsive presentation and keyboard interactions tests

### Task 6: Complete responsive and accessibility sweep for compatibility and configurator

**Files:**

- Modify as needed: `configurator-web/src/features/compatibility/**/*`
- Modify as needed: `configurator-web/src/features/configurator/**/*`
- Modify as needed: related page CSS modules
- Modify/add co-located tests

- [x] адаптировать manual links, rule list/form, condition reorder and intersection screens to narrow viewports
- [x] отключить graph node drag и проверить search, controls, fit/reset, selection and accessible details alternative
- [x] обозначить graph canvas как локальное двумерное представление, не допуская overflow всей страницы
- [x] проверить candidate browser, assembly, filters, explanations Drawer and conflict states at 320 px
- [x] сохранить keyboard-only completion of direct/transitive/intersection flows

### Task 7: Complete responsive and accessibility sweep for saved configurations

**Files:**

- Modify as needed: configuration pages/features and CSS modules
- Modify/add co-located tests

- [x] проверить list/cards/details/edit/copy/export/delete across supported viewports
- [x] адаптировать component groups, totals, notes and long values without losing meaning
- [x] проверить destructive dialog, unsaved changes, retry/status and focus return
- [x] обеспечить доступность JSON export feedback и copied configuration navigation

### Task 8: Add responsive behavioural coverage and finish the audit

**Files:**

- Modify: `configurator-web/e2e/smoke.spec.ts`
- Modify: `docs/accessibility/WCAG_2_2_AA_AUDIT.md`
- Modify: `docs/plans/completed/20260823-responsive-wcag-mobile-hardening.md`

- [x] добавить 320/360/tablet/desktop viewport coverage without screenshot baselines
- [x] проверить no unexpected document overflow, keyboard-only journeys, visible/not-obscured focus and target sampling
- [x] прогнать light/dark, portrait/landscape and three-browser representative paths
- [ ] выполнить ручной VoiceOver/zoom/text-spacing/contrast checklist и записать evidence/known limitations
- [x] прогнать все required frontend checks и зафиксировать фактические результаты
- [x] оставить axe automation and visual regression follow-up явно привязанными к 9.27

## Non-Goals

- Формальная внешняя сертификация WCAG или юридическое заявление о полном соответствии.
- Axe/pa11y integration, accessibility CI gate и visual screenshot regression — задача 9.27.
- Новые бизнес-функции, изменение backend/OpenAPI/БД или generated client.
- Реализация runtime authentication/authorization и auth UI.
- Сохранение пользовательской раскладки graph nodes или новый mobile graph renderer.
- Редизайн brand identity или замена Mantine/design tokens.

## Risks and Mitigations

- **Риск:** исправление 320 px затронет много локальных layout rules. **Мера:** сначала убрать глобальные причины,
  затем проходить feature groups с co-located tests, не использовать новый общий `overflow: hidden`.
- **Риск:** ложное заявление о WCAG AA на основе тестов. **Мера:** полная applicability matrix, manual evidence и
  формулировка internal target, а не certification.
- **Риск:** screen-reader announcements станут шумными. **Мера:** объявлять только page/status transitions и не
  дублировать notification, inline error и live region без необходимости.
- **Риск:** отключение graph drag изменит привычное исследование. **Мера:** drag не сохраняет результат и не является
  бизнес-действием; pan/zoom/search/fit/reset/details остаются.
- **Риск:** 9.26 начнёт дублировать 9.27. **Мера:** не добавлять axe package, snapshots и visual baselines в этой ветке.

## Decision Gate Before Implementation

Перед созданием `feature/CON1-125` и изменением runtime-кода нужно подтвердить:

1. технический reflow baseline меняется с 360 на 320 CSS px; 360 px остаётся основным продуктовым phone target;
2. непредметное перетаскивание узлов compatibility graph отключается;
3. axe automation и visual regression остаются в 9.27, а 9.26 использует manual matrix + существующие test stacks;
4. результат описывается как WCAG 2.2 AA implementation target/internal audit, без заявления о формальной сертификации.

## Implementation Result

- Подтверждённый baseline 320 CSS px реализован без глобального `min-width` и без маскирующего `overflow-x: clip`.
- AppShell получил scroll offsets для fixed regions, keyboard-focusable skip target, SPA page announcement и компактный
  вид текущей предметной области на phone.
- Добавлен reduced-motion fallback; существующие responsive tables/cards, forms, overlays, galleries, configurator and
  configuration screens проверены единым route sweep.
- Перетаскивание graph nodes отключено. Поиск, node/edge selection, pan/zoom controls, fit/reset и линейная details
  alternative сохранены; инструкция локализована.
- Автоматизированно проверены 320 x 568, текущие phone/tablet/desktop scenarios, landscape, light/dark и ru/en, отсутствие
  document overflow, minimum 24 px button targets, mobile navigation targets и focus outside fixed regions.
- `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage` и `npm run test:e2e` выполнены успешно.
  Результат: 207 unit/component tests, line coverage 90.83%, 39 Playwright scenarios в Chromium/Firefox/WebKit.
- Ручные VoiceOver, browser zoom 200%, text-spacing и contrast checks не выдаются за выполненные. Они остаются видимыми
  пунктами внутренней матрицы; axe/visual automation остаётся в 9.27.
