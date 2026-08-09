# AGENTS.md

Обязательные правила для AI-агентов, работающих в `configurator-root`. Human onboarding находится в `README.md` и
`CONTRIBUTING.md`.

## 1. Контекст проекта

`configurator-root` — backend-first конфигуратор компонентов. Репозиторий содержит:

- `configurator` — Spring Boot runtime;
- `configurator-integration-tests` — единые local/external integration contracts;
- `configurator-web` — независимый React/Vite frontend;
- `specs/configurator-api.yaml` — source of truth REST API.

Реализованные области: домены, типы компонентов, атрибуты, компоненты, изображения в MinIO, ручная и автоматическая
совместимость, граф и транзитивный поиск, множественный поиск, пересечения, сохранение и JSON-экспорт конфигураций.

### Текущее ограничение безопасности

OpenAPI содержит `POST /auth/register`, `POST /auth/login` и Bearer JWT scheme, но runtime-аутентификация и авторизация
не реализованы. `TemporaryCurrentUserProvider` возвращает системного пользователя `-1`; соответствующая запись создаётся
миграцией `V5`. Не описывать текущую версию как production-ready и не считать OpenAPI security declaration фактической
защитой endpoint'ов.

## 2. Стек

- Java 21, Gradle;
- Spring Boot 3.4.11;
- PostgreSQL 17, Flyway, jOOQ;
- OpenAPI Generator;
- MinIO;
- MapStruct, Lombok;
- JUnit 5, Spock, ArchUnit, Testcontainers, MockMvc, RestAssured;
- Spotless/Google Java Format;
- JaCoCo с minimum line coverage `0.90` после исключения generated/domain boilerplate.

Frontend:

- Node.js 24 LTS, npm 11;
- React 19.2, TypeScript 6.0 strict, Vite 8.2;
- React Router 7.18, Mantine 9.5, i18next/react-i18next;
- ESLint flat config, Prettier, Stylelint;
- Vitest, Testing Library, MSW, Playwright.

## 3. Архитектурный инвариант

```text
controller -> facade -> service -> outbound port -> infrastructure
```

### Слои

- `api.inbounds.rest`: тонкие контроллеры, OpenAPI interfaces/DTO, web advice. Только HTTP boundary, без
  business/persistence logic.
- `application.facade`: REST DTO ↔ domain mapping, базовая transport validation, вызов service.
- `application.service`: use cases и бизнес-оркестрация с доменными моделями.
- `application.port.out`: интерфейсы repository/storage/current-user и других outbound dependencies.
- `application.mapper`: техническое преобразование моделей без бизнес-решений.
- `application.validator`: application/domain policies.
- `domain.model`, `domain.exception`: чистые доменные модели и ошибки без HTTP/jOOQ dependencies.
- `infrastructure.persistence`: реализации портов через jOOQ/PostgreSQL.
- `infrastructure.storage`: MinIO/S3 implementation.
- `infrastructure.security`: adapter текущего пользователя; сейчас временный, позднее Spring Security-backed.
- `common`: только действительно общие utilities.

Запрещено:

- controller -> repository/infrastructure;
- facade -> persistence в обход service;
- REST DTO в service/domain;
- jOOQ records/generated types выше persistence boundary;
- бизнес-правила в mapper/controller;
- ручное редактирование generated code.

Архитектурные изменения должны сохранять эту модель либо быть отдельно согласованы. Проверки находятся в
`ArchitectureTest`.

## 4. Source of truth и generated code

Никогда не редактировать `build/generated/**`.

### REST API

1. Изменить `specs/configurator-api.yaml`.
2. Запустить Gradle lifecycle (`:configurator:compileJava` или `build`).
3. Адаптировать controller/facade/service/tests.

Generated packages:

- `ru.sultanyarov.configurator.api.inbounds.rest`;
- `ru.sultanyarov.configurator.api.inbounds.rest.dto`.

Frontend API client генерируется из `specs/configurator-api.yaml` командой `npm run api:generate` в
`configurator-web/src/shared/api/generated`; ручные дубликаты transport DTO и правки generated client запрещены.
Прикладной frontend импортирует SDK и типы только через `configurator-web/src/shared/api`. Изменение OpenAPI всегда
сначала вносится в спецификацию, затем отражается и в backend, и во frontend client; `npm run api:check` проверяет
отсутствие drift.

Глобальные frontend providers находятся в `configurator-web/src/app/providers`, route objects — в `src/app/router`,
AppShell и единая desktop/mobile navigation model — в `src/app/layout`. Предметные страницы не создают собственные
Router/Mantine/i18next providers. Пользовательские строки shell и общих страниц должны находиться в translation
resources, а прикладная тема — использовать Mantine tokens вместо hardcoded light-only цветов.

### База данных

1. Добавить новую versioned migration в `configurator/src/main/resources/db/migration`.
2. Не изменять уже выпущенные миграции.
3. Запустить jOOQ generation через Gradle lifecycle.
4. Адаптировать persistence/application и SQL fixtures.

Generated jOOQ package: `ru.sultanyarov.configurator.domain.entity.jooq`.

## 5. Тестовая стратегия

### Backend/unit/repository/architecture

```bash
./gradlew :configurator:test
```

### Local integration

```bash
./gradlew :configurator-integration-tests:test
```

Spring Boot работает in-process, PostgreSQL поднимается Testcontainers, HTTP проверяется MockMvc.

### External integration

```bash
./gradlew :configurator:bootJar
docker compose up -d --build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

External transport использует RestAssured, а setup — PostgreSQL SQL fixtures. Параметры: `test.baseUrl`, `test.dbUrl`,
`test.dbUser`, `test.dbPassword`.

Local и external сценарии должны оставаться единым контрактом. Нельзя дублировать одинаковый кейс с разной логикой.
Общие deterministic fixtures находятся в `configurator-integration-tests/src/test/resources/sql`.

### Frontend

```bash
cd configurator-web
npm ci
npm run api:check
npm run check
npm run test:coverage
```

`npm run check` объединяет format check, ESLint, Stylelint, unit tests, TypeScript typecheck и production build. E2E
запускаются отдельно после однократного `npx playwright install`; browser binaries не скачиваются автоматически при
`npm ci`.

### Definition of Done

Для значимых изменений обязательны:

```bash
./gradlew build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Если внешний контур недоступен, явно указать непроверенный статус. Не выдавать тесты за выполненные без фактического
запуска.

Для изменения только frontend toolchain/UI обязательны `npm ci` и `npm run check`; backend и external integration
проверяются дополнительно, если меняются OpenAPI, backend, Docker delivery или общий runtime-контракт.

## 6. Локальное окружение

`docker-compose.yml` поднимает:

- PostgreSQL 17 — `localhost:5432`;
- MinIO — `localhost:9000`, console `localhost:9001`;
- приложение — `localhost:8080`.

Локальные credentials из Compose нельзя использовать в production. Host-specific Docker socket paths не коммитить;
задавать их локально через environment/IDE или локально изменённый `testcontainers.properties`.

Основные переменные:

- `DB_URL`, `DB_USER`, `DB_PASSWORD`;
- `IMAGE_STORAGE_ENDPOINT`, `IMAGE_STORAGE_ACCESS_KEY`, `IMAGE_STORAGE_SECRET_KEY`;
- `IMAGE_STORAGE_BUCKET`.

Frontend dev server запускается из `configurator-web` командой `npm run dev` на `http://127.0.0.1:5173`. Запросы
`/api/*` проксируются на `http://127.0.0.1:8080/*` с удалением префикса `/api`.

## 7. Git и релизы

### Ветки

- feature: `feature/CON<версия>-<номер>`;
- bugfix: `bugfix/CON<версия>-<номер>`;
- рабочая интеграционная ветка: `develop`;
- стабильная релизная ветка: `master`.

Прямые push в `develop` и `master` не использовать. Feature/bugfix -> PR в `develop`; релиз -> PR `develop` в `master`.

### Коммиты

```text
CON<версия>-<номер> <English description in past tense>
```

Пример: `CON1-83 Prepared repository for first release`.

Не включать в commit unrelated user changes, IDE metadata, `.DS_Store`, credentials или host-specific configuration.

### Версии и release automation

- Semantic Versioning, tag `vX.Y.Z`;
- до реализации authentication/authorization — только `0.x`;
- default Gradle version может быть `-SNAPSHOT`, release workflow передаёт `-PreleaseVersion=X.Y.Z`;
- тег ставится только на commit, достижимый из `master`;
- `.github/workflows/release.yml` создаёт draft release; публикация draft — явное действие владельца;
- release notes и `CHANGELOG.md` должны соответствовать фактически проверенному функционалу.

## 8. GitHub repository hygiene

При изменении workflows:

- минимальные `GITHUB_TOKEN` permissions;
- pin external actions на full commit SHA;
- не использовать `pull_request_target` для checkout untrusted code;
- CODEOWNERS должен явно определять владельца workflows; обязательный review включать только при наличии независимого
  reviewer;
- поддерживать Dependabot для Gradle, Docker и GitHub Actions.

Перед релизом проверить `docs/release/RELEASE_CHECKLIST.md`, CI, branch protection, security settings, лицензию и
отсутствие секретов.

## 9. Обязательный pre-change check

Перед правкой определить:

1. меняется ли OpenAPI;
2. меняется ли schema/Flyway/jOOQ;
3. будет ли косвенно regenerated code;
4. затрагиваются ли архитектурные boundaries;
5. требуется ли общий integration contract;
6. есть ли unrelated local changes, которые надо сохранить.

После работы сообщить:

- что изменено;
- затронуты ли OpenAPI и БД;
- какие проверки реально выполнены;
- что осталось непроверенным;
- какие release blockers сохраняются.

## 10. Приоритет

Architecture, source of truth, security и reproducibility важнее скорости. Предпочитать небольшие локальные изменения;
спорное изменение архитектуры, публичного API, схемы безопасности или release policy сначала согласовать.
