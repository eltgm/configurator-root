# Configurator Web

Frontend Configurator на React, TypeScript и Vite. На этапе 9.8 проект содержит toolchain и минимальный smoke screen;
предметные экраны добавляются последующими задачами Epic 9.

## Требования для разработки

- Node.js 24 LTS;
- npm 11.

## Команды

```bash
npm ci
npm run api:generate
npm run dev
npm run check
npm run test:coverage
```

Dev server доступен на `http://127.0.0.1:5173`. Запросы к `/api/*` проксируются на backend
`http://127.0.0.1:8080` с удалением префикса `/api`.

Для локального E2E один раз установите browser binaries:

```bash
npx playwright install
npm run test:e2e
```

Production build создаётся в `dist/`.

## Навигационный каркас

Основные маршруты:

- `/configurator` — конфигуратор;
- `/components` — каталог компонентов;
- `/configurations` — сохранённые конфигурации;
- `/settings/*` — типы, совместимость, граф и параметры предметной области.

На desktop используется боковая навигация, на телефоне — нижняя. Меню в header переключает системную, светлую и
тёмную темы, а также русский и английский языки. Выбор сохраняется в `localStorage`. До реализации 9.12 предметная
область в header отображается как невыбранная.

## OpenAPI client

Типы, SDK-функции и Fetch-клиент генерируются только из `../specs/configurator-api.yaml`:

```bash
npm run api:generate
npm run api:check
```

Generated output находится в `src/shared/api/generated`, коммитится и не редактируется вручную. Прикладной код должен
импортировать API из `@/shared/api`. `api:check` входит в общий `npm run check` и обнаруживает рассинхронизацию со
спецификацией.

## Server state и ошибки

TanStack Query подключён в общем application provider и является единственным механизмом кэширования данных backend.
Запросы используют generated SDK вместе с `apiRequest`, который преобразует структурированные API-ошибки, сетевые
сбои и неизвестные ошибки в безопасный `AppError`. Для вызова generated SDK следует включать `throwOnError: true`,
чтобы ошибка прошла через этот boundary.

Query повторяется не более одного раза и только при сетевой или серверной ошибке. Мутации автоматически не
повторяются: их ошибки показываются централизованно, а success-уведомление вызывает конкретный сценарий после
подтверждённого результата. Ошибка первоначальной загрузки отображается внутри страницы; ошибка фонового обновления
при наличии cached data показывается notification.

Общие `PageHeader`, `LoadingState`, `EmptyState`, `ErrorState` и `ServerDataState` экспортируются из `@/shared/ui`.
Field-level сообщения для форм извлекаются из `ErrorResponse.details` функцией `getFieldErrors`.
