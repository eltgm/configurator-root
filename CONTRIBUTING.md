# Участие в разработке

Спасибо за интерес к Configurator. Перед изменениями ознакомьтесь с `README.md`, а AI-агентам также необходимо соблюдать
`AGENTS.md`.

## Локальная подготовка

Понадобятся JDK 21 и Docker.

```bash
git clone https://github.com/eltgm/configurator-root.git
cd configurator-root
./gradlew build
```

## Рабочий процесс

1. Создайте issue либо привяжите существующую задачу.
2. Создайте ветку от `develop`: `feature/CON1-<id>` или `bugfix/CON1-<id>`.
3. Вносите небольшие атомарные изменения.
4. Обновите source of truth до ручного кода: OpenAPI для REST, Flyway для БД.
5. Добавьте unit tests и единый local/external integration contract.
6. Запустите проверки.
7. Откройте pull request в `develop` и заполните checklist.

## Проверки

```bash
./gradlew build
./gradlew :configurator:bootJar
docker compose up -d --build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Если внешний контур не удалось запустить, явно укажите это в pull request.

### Frontend

Для frontend-изменений дополнительно нужны Node.js 24 и npm 11:

```bash
cd configurator-web
npm ci
npm run check
npm run test:coverage
npx playwright install
npm run test:e2e
npm run test:accessibility
npm run test:visual
```

Visual regression выполняется в pinned Playwright Docker image. Эталоны обновляются только после намеренного UI
изменения через `npm run test:visual:update`, просматриваются в diff и подтверждаются повторным обычным прогоном.
Подробности: `docs/testing/FRONTEND_TESTING.md`.

## Стиль и архитектура

- Цепочка: `controller -> facade -> service -> port -> infrastructure`.
- Generated files в `build/generated/**` не редактируются.
- Java форматируется Spotless/Google Java Format.
- REST DTO не используются как domain model.
- Local и external integration tests не дублируются по смыслу.

## Коммиты

```text
CON<версия>-<номер> <English description in past tense>
```

Пример: `CON1-83 Prepared repository for first release`.

## Сообщения об ошибках и уязвимостях

Для обычных ошибок используйте bug report. Уязвимости сообщайте приватно по инструкции в `SECURITY.md`, а не через
публичный issue.
