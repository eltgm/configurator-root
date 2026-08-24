# Fix Playwright Firefox HOME Ownership in GitHub Actions

## Overview

GitHub Actions job `frontend-browser` запускает официальный Playwright image от `root`, но runner подставляет
`HOME=/github/home`, смонтированный с владельцем `pwuser`/uid 1001. Firefox запрещает запуск root-процесса с домашним
каталогом другого пользователя, поэтому functional E2E падают при первом переходе матрицы с Chromium на Firefox.

Исправление должно локально согласовать `HOME` с фактическим пользователем job container, не менять браузерную матрицу,
не ослаблять sandbox/test assertions и не затрагивать runtime приложения.

## Context (from discovery)

- **Files/components involved:** `.github/workflows/ci.yml`; существующие `configurator-web/playwright.config.ts` и
  `configurator-web/e2e/compatibility.spec.ts` используются для проверки, но менять их не требуется.
- **Related patterns found:** `configurator-web/scripts/run-visual-tests.mjs` уже задаёт container-local `HOME=/tmp`;
  Playwright image и `@playwright/test` согласованы на exact версии `1.62.1`.
- **Dependencies identified:** GitHub Actions job containers запускают steps от root и монтируют `/github/home`;
  Firefox проверяет соответствие владельца `$HOME` текущему uid.
- **Observed failure:** Chromium tests успели пройти, затем Firefox завершился до выполнения test body с сообщением
  `Running Nightly as root in a regular user's session is not supported`.
- **Unrelated local changes:**
  `configurator-integration-tests/src/test/resources/testcontainers.properties` уже изменён пользователем и должен
  остаться нетронутым.
- **Official references:**
  [GitHub job containers](https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/run-jobs-in-a-container),
  [GitHub container filesystem](https://docs.github.com/en/actions/reference/runners/github-hosted-runners),
  [Playwright discussion with the same Firefox failure](https://github.com/microsoft/playwright/issues/23388).

## Development Approach

- **Testing approach:** regular — сначала минимальная workflow-правка, затем targeted и полный browser regression.
- Завершить и проверить изменение workflow до любых дополнительных корректировок.
- Не менять Playwright tests, retry policy, browser projects или container security options без нового доказанного сбоя.
- Сохранить exact Playwright image и текущие минимальные `GITHUB_TOKEN` permissions.
- Обновлять этот план, если фактический CI выявит дополнительную независимую причину.

## Testing Strategy

- **Workflow validation:** проверить YAML/diff и убедиться, что `HOME=/root` ограничен job `frontend-browser`.
- **Targeted regression:** выполнить упавший `compatibility.spec.ts` для Firefox в
  `mcr.microsoft.com/playwright:v1.62.1-noble` с `HOME=/root`.
- **Full browser regression:** в том же pinned container выполнить `npm run test:e2e`, `npm run test:accessibility` и
  `npm run test:visual:container`.
- **Frontend baseline:** выполнить `npm ci` и `npm run check` согласно project Definition of Done.
- **Definitive verification:** после push/PR повторно запустить GitHub Actions job `Frontend browser quality`; локальный
  Docker-прогон не воспроизводит runner mounts `/github/*` полностью и не выдаётся за CI verification.
- Новые product tests не нужны: product behavior не меняется, а существующая Firefox suite является regression test
  для исправляемой container configuration.

## Progress Tracking

- Отмечать выполненные пункты `[x]` сразу после фактического выполнения.
- Добавлять обнаруженные задачи с `[+]`, blockers — с `[!]`.
- Не считать задачу завершённой, пока targeted Firefox regression не прошёл.
- После реализации зафиксировать, какие проверки выполнены локально, а какие требуют GitHub Actions.

## Solution Overview

В `frontend-browser` задать job-level environment `HOME: /root`. Это сохраняет требуемого GitHub Actions пользователя
`root`, делает домашний каталог принадлежащим текущему uid и применяется ко всем трём последовательным Playwright gates.

Не выбранные варианты:

- `container.options: --user pwuser` — может нарушить доступ GitHub Actions к смонтированному workspace;
- `HOME=/root` только перед одной командой — оставляет accessibility/visual gates уязвимыми и дублирует конфигурацию;
- отключение Firefox или sandbox — снижает coverage/security и не исправляет ownership mismatch.

## Technical Details

Ожидаемая конфигурация ограничена одним job:

```yaml
frontend-browser:
  env:
    HOME: /root
  container:
    image: mcr.microsoft.com/playwright:v1.62.1-noble
```

После изменения Firefox должен видеть одновременно `uid=0`, `HOME=/root` и root-owned home directory. Сообщение
`CanCreateUserNamespace() clone() failure: EPERM` в приведённом логе является сопутствующим sandbox diagnostic;
фатальная строка непосредственно указывает на несовпадение пользователя и владельца `$HOME`. Дополнительное ослабление
seccomp не планируется, пока запуск с корректным `HOME` не покажет самостоятельный sandbox failure.

## What Goes Where

- `.github/workflows/ci.yml` — единственная планируемая implementation change.
- `configurator-web/e2e/compatibility.spec.ts` — targeted regression без изменения файла.
- `docs/plans/20260824-playwright-firefox-home-ownership.md` — план и фактический progress/status.

## Implementation Steps

### Task 1: Align the browser job HOME with its container user

**Files:**

- Modify: `.github/workflows/ci.yml`
- Modify: `docs/plans/20260824-playwright-firefox-home-ownership.md`

- [x] добавить `HOME: /root` на уровне job `frontend-browser`
- [x] убедиться, что изменение не затрагивает другие jobs, permissions, image version или browser projects
- [x] проверить workflow diff и `git diff --check`
- [x] выполнить targeted Firefox regression для `e2e/compatibility.spec.ts` в pinned container
- [x] зафиксировать результат проверки в этом плане до перехода к полной матрице

### Task 2: Verify all frontend quality gates

**Files:**

- Modify only if a verification result requires correction: `.github/workflows/ci.yml`
- Modify: `docs/plans/20260824-playwright-firefox-home-ownership.md`

- [x] выполнить `npm ci`
- [x] выполнить `npm run check`
- [x] выполнить полный `npm run test:e2e` в pinned Playwright container с `HOME=/root`
- [x] выполнить `npm run test:accessibility` в том же container
- [x] выполнить `npm run test:visual:container` в том же container
- [x] проверить, что browser reports/test-results по-прежнему собираются при failure
- [x] обновить план фактическими результатами и непроверенными пунктами

### Task 3: Verify acceptance criteria

- [x] Firefox запускается и выполняет тесты вместо завершения на ownership check
- [x] Chromium, Firefox и WebKit остаются в functional E2E matrix
- [x] accessibility и visual gates не изменены и проходят
- [x] OpenAPI, generated code, backend architecture, Flyway/jOOQ и БД не изменены
- [x] unrelated `testcontainers.properties` не попал в diff задачи
- [ ] после push/PR повторно запущен GitHub Actions job `Frontend browser quality`

### Task 4: [Final] Complete documentation

- [x] записать локальные verification results и непроверенный GitHub Actions status в этот план
- [ ] после подтверждённого CI success переместить план в `docs/plans/completed/`

## Post-Completion

После локальной реализации владелец репозитория должен push-нуть ветку/обновить PR и подтвердить зелёный повторный запуск
`Frontend browser quality`. До этого исправление считается локально проверенным, но не подтверждённым в реальном
GitHub-hosted runner environment.

## Verification Results

- `npm ci` — passed with Node `v24.10.0` and npm `11.6.0`.
- `npm run check` — passed; 41 Vitest files / 207 tests, TypeScript build and Vite production build succeeded.
- Targeted pinned-container Firefox regression — 3/3 `compatibility.spec.ts` tests passed.
- Full pinned-container functional E2E — 69/69 tests passed across Chromium, Firefox and WebKit.
- Pinned-container accessibility gate — 34/34 tests passed.
- Pinned-container visual regression gate — 7/7 tests passed.
- `git diff --check` — passed.
- `[!]` GitHub-hosted Actions rerun remains pending until the pushed branch is used in a PR or otherwise dispatched;
  branch pushes alone do not match this workflow's `push` filter (`develop`, `master`).
