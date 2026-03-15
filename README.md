# Configurator Root

Short English summary: `configurator-root` is a backend-first MVP for a component configurator.  
The repository contains a Spring Boot application, OpenAPI and jOOQ code generation, PostgreSQL/Flyway migrations, and a dedicated integration test module with both in-process and external test modes.

## О проекте

`configurator-root` — это backend-first MVP для конфигуратора компонентов.

Проект предназначен для хранения и управления:
- доменами (`Domain`)
- типами компонентов (`ComponentType`)
- определениями атрибутов (`AttributeDefinition`)
- компонентами (`Component`)
- значениями атрибутов компонентов (`AttributeValue`)

Репозиторий ориентирован на разработчиков и содержит не только само приложение, но и весь необходимый контур для локальной разработки:
- миграции БД
- генерацию OpenAPI-кода
- генерацию jOOQ-кода
- unit-тесты
- интеграционные тесты в отдельном модуле
- Docker Compose для локального окружения

Статус проекта: `MVP in progress`

## Что есть в репозитории

### Модули

- `configurator`
  Основное Spring Boot приложение.
- `configurator-integration-tests`
  Отдельный модуль интеграционных тестов.
- `specs/configurator-api.yaml`
  Источник истины для OpenAPI-контрактов.

### Технологический стек

- Java 21
- Gradle
- Spring Boot 3.4.x
- Spring Web
- Spring Validation
- PostgreSQL
- Flyway
- jOOQ
- OpenAPI Generator
- MapStruct
- Lombok
- JUnit 5
- Spock
- Testcontainers
- RestAssured

## Текущее состояние

Сейчас проект находится в стадии активной разработки MVP. В репозитории уже есть рабочая backend-архитектура, миграции, генерация кода и покрытие тестами, но функциональность реализована не полностью.

### Уже реализовано

- CRUD для доменов
- CRUD для типов компонентов
- создание и обновление атрибутов типов компонентов
- создание компонента через `POST /components`
- централизованная обработка ошибок REST API
- unit-тесты c порогом покрытия `>= 90%` для модуля `configurator`
- интеграционные тесты в двух режимах:
  - in-process через Spring Boot + Testcontainers
  - external against running app

### Текущие ограничения

- frontend в этом репозитории отсутствует
- проект сейчас backend-first
- не все endpoint’ы из OpenAPI уже реализованы
- в `ComponentController` часть endpoint’ов пока остаются заглушками
- основная зрелая часть приложения сейчас:
  - `domains`
  - `component-types`
  - `attributes`
  - `components` creation flow

## Архитектура

В проекте используется слоистая архитектура с элементами гексагональной.

Базовая логическая цепочка:

`controller -> facade -> service -> port -> infrastructure`

### Основные пакеты

- `api.inbounds.rest`
  HTTP-слой, OpenAPI controllers, REST DTO, exception handling
- `application.facade`
  граница между transport DTO и бизнес-моделями
- `application.service`
  application use cases и orchestration
- `application.port.out`
  outbound ports / repository interfaces
- `application.validator`
  отдельные validator-компоненты уровня application rules
- `application.mapper`
  MapStruct-мэппинг между transport и domain model
- `domain.model`
  доменные модели
- `domain.exception`
  доменные и application-level исключения
- `infrastructure.persistence`
  jOOQ-реализации и persistence-специфичный код

### Архитектурные правила

- контроллеры должны быть тонкими
- контроллеры не должны ходить в репозитории напрямую
- фасады принимают/возвращают REST DTO и делегируют в сервисы
- сервисы работают с доменными моделями
- интерфейсы репозиториев живут в `application.port.out`
- реализации репозиториев живут в `infrastructure.persistence`
- generated-код не редактируется вручную

## Структура репозитория

```text
configurator-root
├── configurator
│   ├── src/main/java
│   ├── src/main/resources
│   ├── build.gradle
│   ├── jooq.gradle
│   └── openapi.gradle
├── configurator-integration-tests
│   ├── src/test
│   ├── src/externalTest
│   └── build.gradle
├── specs
│   └── configurator-api.yaml
├── docker-compose.yml
├── Dockerfile
├── build.gradle
└── settings.gradle
```

## Источники истины

### OpenAPI

Источник истины:

- [specs/configurator-api.yaml](/Users/eltgm/IdeaProjects/configurator-root/specs/configurator-api.yaml)

Если нужно изменить REST API:
1. изменить OpenAPI-спецификацию
2. пересгенерировать код через Gradle lifecycle
3. адаптировать ручной код приложения

### База данных и jOOQ

Источник истины:

- `configurator/src/main/resources/db/migration/*`

Если нужно изменить структуру БД:
1. добавить новую Flyway-миграцию
2. пересгенерировать jOOQ-код
3. адаптировать persistence/application код

### Generated-код

Нельзя редактировать вручную:

- `build/generated/**`

## Требования для локальной разработки

Нужно установить:

- JDK 21
- Docker / Docker Desktop
- Gradle Wrapper используется из репозитория, отдельная установка Gradle не нужна

## Быстрый старт

### Вариант 1. Основной путь: запуск через Docker Compose

Этот путь рекомендуется для первого запуска.

Важно: текущий `Dockerfile` является runtime-only и ожидает уже собранный JAR.

#### 1. Собрать JAR

```bash
./gradlew :configurator:bootJar
```

#### 2. Поднять приложение и PostgreSQL

```bash
docker compose up --build
```

После запуска:

- приложение: [http://localhost:8080](http://localhost:8080)
- PostgreSQL:
  - host: `localhost`
  - port: `5432`
  - db: `configurator`
  - user: `configurator`
  - password: `configurator`

### Вариант 2. Локальный запуск приложения без Docker app

Можно поднять только PostgreSQL в Docker, а приложение запустить локально.

#### 1. Поднять PostgreSQL

```bash
docker compose up -d postgres
```

#### 2. Запустить приложение

```bash
./gradlew :configurator:bootRun
```

По умолчанию приложение использует:

- `DB_URL=jdbc:postgresql://localhost:5432/configurator`
- `DB_USER=configurator`
- `DB_PASSWORD=configurator`

Эти параметры можно переопределить через переменные окружения.

## Конфигурация приложения

Основной конфиг:

- [application.yml](/Users/eltgm/IdeaProjects/configurator-root/configurator/src/main/resources/application.yml)

Локальный профиль:

- [application-local.yml](/Users/eltgm/IdeaProjects/configurator-root/configurator/src/main/resources/application-local.yml)

Поддерживаемые переменные окружения:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

## OpenAPI и документация API

OpenAPI-спецификация лежит в:

- [configurator-api.yaml](/Users/eltgm/IdeaProjects/configurator-root/specs/configurator-api.yaml)

После запуска приложения стоит проверить:

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Если пути будут изменены конфигурацией, ориентируйтесь на `springdoc`-настройки проекта.

## Тесты

### Unit tests

Основной модуль:

```bash
./gradlew :configurator:test
```

Особенности:

- вместе с unit-тестами запускается JaCoCo
- для модуля `configurator` зафиксирован порог покрытия `>= 90%`
- часть пакетов исключена из coverage policy:
  - `common.util`
  - `domain`
  - `build`
  - `infrastructure.persistence.jooq.config`
  - generated REST/API DTO

### In-process integration tests

Интеграционные тесты, которые сами поднимают Spring context и используют Testcontainers:

```bash
./gradlew :configurator-integration-tests:test
```

Этот режим:

- поднимает PostgreSQL через Testcontainers
- стартует Spring Boot in-process
- гоняет HTTP-контур через `MockMvc`

### External integration tests

Интеграционные тесты против уже поднятого приложения:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Перед запуском нужно:

1. поднять PostgreSQL и приложение
2. убедиться, что приложение доступно по `http://localhost:8080`

При необходимости можно переопределить параметры:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest \
  -Dtest.baseUrl=http://localhost:8080 \
  -Dtest.dbUrl=jdbc:postgresql://localhost:5432/configurator \
  -Dtest.dbUser=configurator \
  -Dtest.dbPassword=configurator
```

### Полная проверка перед завершением работы

Рекомендуемый минимум:

```bash
./gradlew build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

## Что обычно меняют разработчики

### Изменение API

1. изменить [specs/configurator-api.yaml](/Users/eltgm/IdeaProjects/configurator-root/specs/configurator-api.yaml)
2. пересобрать проект
3. адаптировать controllers / facade / service / tests

### Изменение БД

1. добавить Flyway-миграцию в `configurator/src/main/resources/db/migration`
2. пересобрать проект
3. адаптировать jOOQ-based persistence слой
4. обновить тестовые SQL-фикстуры при необходимости

### Добавление новой backend-фичи

Ожидаемый маршрут:

1. OpenAPI контракт
2. facade
3. service
4. port
5. infrastructure
6. unit tests
7. in-process integration tests
8. external integration tests

## Реализованные API-области

На текущий момент в коде наиболее полно реализованы:

- `Domains`
- `Component Types`
- `Attributes`
- `POST /components`

Часть endpoint’ов из `Components`, а также другие продуктовые области из OpenAPI пока не завершены.

## Git flow

### Ветки

Формат веток:

- задачи: `feature/CON<версия>-<номер>`
- багфиксы: `bugfix/CON<версия>-<номер>`

Пример:

- `feature/CON1-36`
- `bugfix/CON1-42`

### Коммиты

Формат commit message:

`CON<версия>-<номер> <description in English, past tense>`

Пример:

`CON1-36 Added integration tests for component creation`

### Merge flow

- прямой push в `main/master` запрещён
- работа ведётся в отдельных ветках
- изменения сливаются через PR в `dev`
- релизная версия сливается из `dev` в `master`

## Полезные команды

### Сборка всего проекта

```bash
./gradlew build
```

### Сборка runtime JAR

```bash
./gradlew :configurator:bootJar
```

### Пересборка OpenAPI и jOOQ через обычный lifecycle

```bash
./gradlew :configurator:compileJava
```

### Только unit-тесты backend-модуля

```bash
./gradlew :configurator:test
```

### Только интеграционные тесты

```bash
./gradlew :configurator-integration-tests:test
```

### Только внешние интеграционные тесты

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest
```

## Важные замечания

- не редактируйте generated-код вручную
- не воспринимайте весь OpenAPI как полностью реализованный runtime-функционал
- если меняете БД, не забывайте обновлять миграции и тестовые фикстуры
- если меняете API, не забывайте обновлять контрактные интеграционные тесты

## Связанные документы

- [AGENTS.md](/Users/eltgm/IdeaProjects/configurator-root/AGENTS.md)
- [configurator-api.yaml](/Users/eltgm/IdeaProjects/configurator-root/specs/configurator-api.yaml)

