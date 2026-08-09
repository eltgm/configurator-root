# Configurator Web

Frontend Configurator на React, TypeScript и Vite. На этапе 9.8 проект содержит toolchain и минимальный smoke screen;
предметные экраны добавляются последующими задачами Epic 9.

## Требования для разработки

- Node.js 24 LTS;
- npm 11.

## Команды

```bash
npm ci
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
