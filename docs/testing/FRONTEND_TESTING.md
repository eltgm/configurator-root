# Frontend testing

## Установка

Из `configurator-web` установите exact зависимости и Playwright browsers:

```bash
npm ci
npx playwright install
```

`npm ci` намеренно не скачивает браузеры. Для visual regression дополнительно нужен запущенный Docker Desktop.

## Наборы проверок

```bash
npm run check
npm run test:coverage
npm run test:e2e
npm run test:accessibility
npm run test:visual
npm run test:delivery
```

- `check` проверяет OpenAPI client drift, формат, ESLint, Stylelint, unit/component tests, TypeScript и production build;
- `test:coverage` блокирует падение ниже 90% lines/statements, 85% functions и 80% branches;
- `test:e2e` выполняет функциональные journeys в Chromium, Firefox и WebKit;
- `test:accessibility` выполняет axe WCAG A/AA scans в Chromium для desktop и mobile;
- `test:visual` выполняет сравнение PNG-baselines в pinned Linux Playwright container.
- `test:delivery` проверяет собранную SPA и реальный `/api` boundary через запущенный production gateway.

Functional, accessibility и visual suites используют один детерминированный mock HTTP boundary. Они не требуют
запущенного backend/PostgreSQL/MinIO и не обращаются к пользовательским данным.

Delivery suite намеренно отделён: перед ним нужно собрать boot JAR и поднять Compose с development override, чтобы
host-side external fixtures имели loopback-доступ к PostgreSQL:

```bash
./gradlew :configurator:bootJar
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
cd configurator-web
npm run test:delivery
```

Тест открывает `http://127.0.0.1:8080` без route mocks, проверяет production HTML, реальный `/api/domains` и прямую
навигацию на вложенный SPA route. Значение entry point можно переопределить через `CONFIGURATOR_DELIVERY_BASE_URL`.

### Диагностика gateway

- `502 Bad Gateway`: проверьте `docker compose ps` и `docker compose logs app gateway`; liveness `/healthz` проверяет
  только NGINX, readiness backend подтверждает `/api/v3/api-docs`;
- старый UI после rebuild: убедитесь, что запущен новый gateway image; `index.html` не кэшируется, а `/assets/*`
  используют immutable content hash;
- `413 Request Entity Too Large`: gateway принимает до 16 MB, backend — файл до 10 MB; больший ответ является
  ожидаемой защитой, а не сетевой ошибкой;
- API вернул HTML: проверьте наличие `/api` в browser URL и trailing slash у `proxy_pass` в reviewed NGINX config.

## Accessibility

Accessibility gate проверяет автоматически обнаруживаемые нарушения по тегам `wcag2a`, `wcag2aa`, `wcag21a`,
`wcag21aa`, `wcag22aa`. При падении откройте HTML report в `playwright-report/accessibility`: JSON attachment содержит
полные `violations` и `incomplete`, а сообщение теста — rule, impact, selectors и help URL.

Нельзя исправлять падение blanket `exclude`, глобальным отключением rule или постоянным allowlist. Сначала подтвердите
нарушение в DOM и исправьте UI. Возможный third-party false positive должен быть точечно доказан, документирован и
получить отдельную follow-up задачу. Axe не заменяет manual checklist из `docs/accessibility/WCAG_2_2_AA_AUDIT.md`.

## Visual baselines

Обычное сравнение:

```bash
npm run test:visual
```

Контролируемое обновление после намеренного UI-изменения:

```bash
npm run test:visual:update
git status --short configurator-web/e2e/__screenshots__
```

Обе команды используют образ `mcr.microsoft.com/playwright:v1.62.1-noble`; версия должна совпадать с exact
`@playwright/test` в `package.json`. Host `node_modules` закрыт отдельным container volume, поэтому Linux packages не
попадают на Windows/macOS.

Перед commit просмотрите каждый added/changed PNG и убедитесь, что diff вызван ожидаемым изменением, данные не содержат
секретов, а masks/tolerances не скрывают regressions. Затем повторно выполните `npm run test:visual`: второй прогон
должен пройти без обновления файлов. Не создавайте baselines обычным host Playwright — rasterization зависит от ОС.

## CI и диагностика

CI выполняет два frontend jobs. `Frontend static and unit quality` запускает `npm ci`, `check` и coverage. После него
`Frontend browser quality` внутри того же pinned Playwright image выполняет functional, accessibility и visual suites
с одним worker. Job external contracts поднимает production gateway, выполняет REST contracts через `/api`, затем
запускает delivery smoke без mocks.

При failure на семь дней сохраняются отдельные `playwright-report` и `test-results`. Trace открывается командой:

```bash
npx playwright show-trace path/to/trace.zip
```

Локально HTML report можно открыть через `npx playwright show-report` с соответствующим каталогом report.
