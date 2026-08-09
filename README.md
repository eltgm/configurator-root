# Configurator

[![CI](https://github.com/eltgm/configurator-root/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/eltgm/configurator-root/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4.11](https://img.shields.io/badge/Spring%20Boot-3.4.11-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![OpenAPI 3.1](https://img.shields.io/badge/OpenAPI-3.1-6BA539?logo=openapiinitiative&logoColor=white)](specs/configurator-api.yaml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Backend-first конфигуратор компонентов с React-интерфейсом: каталог предметных областей и компонентов, ручные и
атрибутивные правила совместимости, поиск совместимых наборов и сохранение конфигураций.

> [!IMPORTANT]
> Текущий релизный уровень — `0.1.0` (MVP preview). Контракты `POST /auth/register` и `POST /auth/login` описаны в
> OpenAPI, но аутентификация и авторизация ещё не реализованы. API нельзя публиковать в недоверенной сети до добавления
> Spring Security/JWT.

## Возможности

| Область          | Состояние       | Возможности                                                                       |
|------------------|-----------------|-----------------------------------------------------------------------------------|
| Домены           | Реализовано     | создание, чтение, обновление, удаление, пагинация                                 |
| Типы компонентов | Реализовано     | CRUD внутри домена                                                                |
| Атрибуты         | Реализовано     | создание, получение списка, обновление определений                                |
| Компоненты       | Реализовано     | создание, получение, обновление, архивирование, списки и фильтрация               |
| Изображения      | Реализовано     | загрузка в MinIO, порядок изображений, получение списка                           |
| Совместимость    | Реализовано     | ручные связи, граф, CRUD атрибутивных правил, объяснение результата               |
| Конфигуратор     | Реализовано     | прямой и транзитивный поиск, поиск по нескольким компонентам, пересечение наборов |
| Конфигурации     | Реализовано     | создание, список, получение и экспорт в JSON                                      |
| Web-интерфейс    | В разработке    | shell, темы, локализация, первый запуск и управление предметными областями         |
| Аутентификация   | Только контракт | регистрация, login и Bearer JWT описаны, runtime-реализации нет                   |

Предметные страницы типов, компонентов, совместимости, конфигуратора и конфигураций пока представлены подготовленными
маршрутами. Реальный пользовательский сценарий уже доступен для создания демо/пустой предметной области, выбора,
редактирования и подтверждённого удаления.

## Архитектура

Проект следует слоистой архитектуре с гексагональными границами:

```text
controller -> facade -> service -> outbound port -> infrastructure
```

- `api.inbounds.rest` — HTTP, generated OpenAPI-интерфейсы и REST DTO;
- `application.facade` — transport boundary и маппинг;
- `application.service` — use cases и бизнес-оркестрация;
- `application.port.out` — интерфейсы хранилищ и внешних систем;
- `domain` — доменные модели и ошибки;
- `infrastructure` — jOOQ/PostgreSQL, MinIO и временный адаптер текущего пользователя.

Архитектурные границы проверяются тестами ArchUnit. Generated-код в `build/generated/**` вручную не редактируется.

## Технологии

- Java 21, Gradle Wrapper;
- Spring Boot 3.4.11, Spring Web, Bean Validation;
- PostgreSQL 17, Flyway, jOOQ;
- OpenAPI 3.1 и OpenAPI Generator;
- MinIO для изображений;
- MapStruct и Lombok;
- JUnit 5, Spock, ArchUnit, Testcontainers, MockMvc и RestAssured;
- JaCoCo с обязательным покрытием не ниже 90%;
- Spotless и Google Java Format;
- Node.js 24 LTS и npm 11;
- React 19.2, TypeScript 6, Vite 8, React Router;
- Mantine, TanStack Query, React Hook Form, Zod и i18next;
- Vitest, Testing Library, MSW и Playwright.

## Структура репозитория

```text
.
├── configurator/                   # Spring Boot приложение
├── configurator-integration-tests/ # общие local/external integration contracts
├── configurator-web/               # независимый React/Vite frontend
├── specs/configurator-api.yaml     # источник истины REST API
├── docs/release/                   # релизный аудит и checklist
├── .github/                        # CI, release automation, templates
├── docker-compose.yml
└── Dockerfile
```

Источники истины:

- REST API — [`specs/configurator-api.yaml`](specs/configurator-api.yaml);
- схема БД — [`configurator/src/main/resources/db/migration`](configurator/src/main/resources/db/migration);
- правила для AI-агентов — [`AGENTS.md`](AGENTS.md).

## Быстрый старт

Понадобятся JDK 21 и Docker с Compose plugin. Отдельно устанавливать Gradle не нужно.

```bash
./gradlew :configurator:bootJar
docker compose up --build
```

После запуска доступны:

- приложение — <http://localhost:8080>;
- Swagger UI — <http://localhost:8080/swagger-ui/index.html>;
- OpenAPI JSON — <http://localhost:8080/v3/api-docs>;
- MinIO API — <http://localhost:9000>;
- MinIO Console — <http://localhost:9001>;
- PostgreSQL — `localhost:5432/configurator`.

Локальные значения `configurator/configurator`, `minioadmin/minioadmin` предназначены только для разработки.

### Запуск web-интерфейса для разработки

После запуска backend откройте второй терминал. Понадобятся Node.js 24 LTS и npm 11:

```bash
cd configurator-web
npm ci
npm run dev
```

Web-интерфейс будет доступен на <http://127.0.0.1:5173>. Dev server проксирует `/api/*` на backend
`http://127.0.0.1:8080`. Готовая однокнопочная поставка для нетехнического пользователя будет добавлена отдельной
задачей; текущий вариант предназначен для разработки.

### Запуск приложения из IDE

Поднимите инфраструктуру и запустите main-класс Spring Boot из IntelliJ IDEA:

```bash
docker compose up -d postgres minio
```

По умолчанию приложение использует локальные адреса сервисов из `application.yml`. При необходимости задайте переменные
окружения в Run Configuration.

## Конфигурация

| Переменная                 | Значение по умолчанию                           | Назначение                       |
|----------------------------|-------------------------------------------------|----------------------------------|
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/configurator` | JDBC URL                         |
| `DB_USER`                  | `configurator`                                  | пользователь БД                  |
| `DB_PASSWORD`              | `configurator`                                  | пароль БД                        |
| `IMAGE_STORAGE_ENDPOINT`   | `http://localhost:9000`                         | внутренний endpoint MinIO/S3     |
| `IMAGE_STORAGE_ACCESS_KEY` | `minioadmin`                                    | access key                       |
| `IMAGE_STORAGE_SECRET_KEY` | `minioadmin`                                    | secret key                       |
| `IMAGE_STORAGE_BUCKET`     | `configurator-components`                       | bucket изображений               |

Лимит одного загружаемого файла — 10 MB.
Содержимое изображений выдаётся через `GET /component-images/{id}/content`; MinIO не требуется публиковать для
браузера.

## Проверка проекта

### Полный локальный контур

```bash
./gradlew build
```

Команда компилирует проект, запускает unit/repository/architecture tests, local integration contracts, Spotless и JaCoCo
verification. Для Testcontainers должен быть доступен Docker daemon.

### External integration contracts

```bash
./gradlew :configurator:bootJar
docker compose up -d --build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Параметры внешнего контура можно переопределить:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest \
  -Dtest.baseUrl=http://localhost:8080 \
  -Dtest.dbUrl=jdbc:postgresql://localhost:5432/configurator \
  -Dtest.dbUser=configurator \
  -Dtest.dbPassword=configurator
```

Local и external режимы используют одни и те же контрактные сценарии; различается только transport/setup.

### Frontend

```bash
cd configurator-web
npm ci
npm run check
npm run test:coverage
```

`npm run check` проверяет generated API drift, форматирование, ESLint, Stylelint, unit/component tests, TypeScript и
production build. E2E запускаются отдельно командой `npm run test:e2e` после `npx playwright install`.

## Разработка

1. Создайте `feature/CON<версия>-<номер>` или `bugfix/CON<версия>-<номер>` от `develop`.
2. Для API сначала измените OpenAPI; для БД сначала добавьте Flyway-миграцию.
3. Соблюдайте цепочку `controller -> facade -> service -> port -> infrastructure`.
4. Добавьте unit-тесты и общий integration contract.
5. Выполните `./gradlew build` и внешний интеграционный контур.
6. Откройте pull request в `develop`.

Подробнее: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Релизы

Проект использует Semantic Versioning. До реализации безопасности выпускаются версии `0.x`.

- рабочая ветка — `develop`;
- стабильная ветка — `master`;
- релиз готовится PR из `develop` в `master`;
- тег формата `vX.Y.Z` ставится только на проверенный commit из `master`;
- workflow создаёт draft GitHub Release с JAR, OpenAPI и контрольными суммами.

История изменений: [`CHANGELOG.md`](CHANGELOG.md). Чеклист первого релиза: [
`docs/release/RELEASE_CHECKLIST.md`](docs/release/RELEASE_CHECKLIST.md).

## Безопасность и поддержка

Уязвимости не следует публиковать в обычных issues. Используйте процедуру из [`SECURITY.md`](SECURITY.md). Для ошибок и
предложений предусмотрены GitHub issue templates.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
