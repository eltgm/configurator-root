# AGENTS.md

Этот файл описывает обязательные правила работы AI-агентов в репозитории `configurator-root`.
Файл предназначен именно для агентов, а не для человеческого onboarding.

## 1. Назначение репозитория

Репозиторий содержит backend-приложение `configurator` на Spring Boot и отдельный модуль интеграционных тестов `configurator-integration-tests`.

Проект реализует backend для конфигуратора компонентов с доменами, типами компонентов, атрибутами, компонентами и совместимостью.

Структура модулей:

- `configurator`
  Основное приложение Spring Boot.
- `configurator-integration-tests`
  Отдельный модуль интеграционных тестов.
- `specs/configurator-api.yaml`
  Источник истины для OpenAPI-контрактов.

## 2. Технологический стек

- Java 21
- Spring Boot 3.4.x
- Gradle
- PostgreSQL
- Flyway
- jOOQ
- OpenAPI Generator
- Spock
- Testcontainers
- RestAssured для external integration tests

## 3. Архитектурная модель

В проекте используется слоистая архитектура с элементами гексагональной.

Обязательная логическая цепочка:

- `controller -> facade -> service -> port -> infrastructure`

Это правило считается базовым архитектурным инвариантом.

### 3.1. Слои и их обязанности

#### `api.inbounds.rest`

Назначение:

- входной HTTP-слой;
- реализация OpenAPI-интерфейсов;
- приём REST DTO;
- возврат REST DTO;
- web/advice-конфигурация.

Правила:

- контроллеры должны быть тонкими;
- контроллеры не должны содержать бизнес-логику;
- контроллеры не должны обращаться к репозиториям напрямую;
- контроллеры должны работать через facade;
- REST DTO считаются transport-моделями, а не доменными.

#### `application.facade`

Назначение:

- граница между transport DTO и бизнес-моделями;
- orchestration на уровне входного application boundary;
- валидация входа базового уровня;
- вызов сервисов;
- маппинг DTO <-> domain model.

Правила:

- facade может принимать REST DTO;
- facade не должен содержать persistence-логику;
- facade не должен обходить service и работать напрямую с инфраструктурой.

#### `application.service`

Назначение:

- реализация application use cases;
- orchestration бизнес-операций;
- вызов портов;
- контроль доменных сценариев.

Правила:

- service работает с доменными моделями;
- service не должен зависеть от HTTP-деталей;
- service не должен знать про `MockMvc`, `RestAssured`, web request/response;
- service не должен зависеть от конкретной реализации хранилища.

#### `application.port.out`

Назначение:

- абстракции для взаимодействия с внешним миром;
- репозиторные интерфейсы и другие outbound-порты.

Правила:

- интерфейсы живут здесь;
- реализации не живут здесь.

#### `application.mapper`

Назначение:

- преобразование REST DTO <-> domain model;
- преобразование immutable-моделей;
- установка технических дефолтов, если это требуется для конструирования immutable model.

Правила:

- допускается задавать технические дефолты, нужные для построения immutable-модели;
- нельзя размещать здесь бизнес-решения уровня валидности, доступности, уникальности, разрешённости сценария.

#### `application.validator`

Назначение:

- отдельные validator/policy-компоненты уровня application/domain rules.

#### `domain.model`

Назначение:

- доменные модели;
- доменные значения и enum’ы.

Правила:

- домен не должен зависеть от REST-контрактов и HTTP-слоя;
- домен не должен зависеть от инфраструктурных реализаций;
- домен должен оставаться максимально чистым.

#### `domain.exception`

Назначение:

- исключения предметной области и application-level ошибки проекта.

#### `infrastructure.persistence`

Назначение:

- реализация портов через jOOQ;
- persistence-конвертеры;
- SQL/persistence-specific логика.

Правила:

- вся конкретная реализация доступа к данным живёт здесь;
- jOOQ и database-specific детали не должны протекать вверх в controller/facade/service.

#### `common`

Назначение:

- только действительно общие утилиты без привязки к конкретному transport/persistence слою.

### 3.2. Что запрещено архитектурно

- обращаться из controller напрямую в repository;
- смешивать REST DTO с доменными моделями;
- редактировать persistence из facade/validator в обход service;
- тащить jOOQ records или generated persistence types в application/domain логику без явной необходимости;
- размещать инфраструктурные реализации рядом с доменными абстракциями, если для этого уже есть `infrastructure`.

## 4. Источники истины и generated-код

### 4.1. Generated-код редактировать вручную запрещено

Нельзя вручную редактировать:

- `build/generated/**`

Это обязательное правило.

### 4.2. Как вносить изменения правильно

#### OpenAPI

Источник истины:

- `specs/configurator-api.yaml`

Если нужно поменять REST API, DTO или интерфейсы контроллеров:

1. изменить `specs/configurator-api.yaml`;
2. пересгенерировать OpenAPI-артефакты через обычный Gradle lifecycle;
3. адаптировать ручной код приложения под новый контракт.

#### jOOQ

Источник истины:

- Flyway-миграции в `configurator/src/main/resources/db/migration`

Если нужно поменять таблицы, индексы, constraints или структуру БД:

1. добавить новую Flyway-миграцию;
2. пересгенерировать jOOQ-артефакты через обычный Gradle lifecycle;
3. адаптировать persistence/application код.

### 4.3. Generated-артефакты в проекте

OpenAPI generator:

- генерирует код из `specs/configurator-api.yaml`
- target package:
  - `ru.sultanyarov.configurator.api.inbounds.rest`
  - `ru.sultanyarov.configurator.api.inbounds.rest.dto`

jOOQ codegen:

- генерирует код из Flyway DDL
- target package:
  - `ru.sultanyarov.configurator.domain.entity.jooq`

## 5. Структура проекта

### 5.1. Корневой уровень

- `build.gradle`
  Общая Gradle-конфигурация и Spring dependency management.
- `settings.gradle`
  Подключённые модули.
- `docker-compose.yml`
  Docker-окружение для PostgreSQL и приложения.
- `Dockerfile`
  Runtime Docker image для backend-приложения.
- `specs/configurator-api.yaml`
  OpenAPI-спецификация.

### 5.2. Модуль `configurator`

Основной runtime-модуль.

Ключевые файлы:

- `configurator/build.gradle`
- `configurator/openapi.gradle`
- `configurator/jooq.gradle`
- `configurator/src/main/resources/application.yml`
- `configurator/src/main/resources/application-local.yml`
- `configurator/src/main/resources/db/migration/*`

### 5.3. Модуль `configurator-integration-tests`

Модуль интеграционных тестов.

Содержит два режима:

- локальный интеграционный контур;
- внешний интеграционный контур против уже поднятого приложения.

## 6. Сборка и запуск

### 6.1. Основные команды

Сборка проекта:

```bash
./gradlew build
```

Сборка backend-модуля:

```bash
./gradlew :configurator:build
```

Сборка jar:

```bash
./gradlew :configurator:bootJar
```

Запуск локальных интеграционных тестов:

```bash
./gradlew :configurator-integration-tests:test
```

Запуск внешних интеграционных тестов против поднятого приложения:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest
```

### 6.2. Docker-окружение

`docker-compose.yml` поднимает:

- `postgres`
- `app`

Запуск:

```bash
docker compose up -d
```

Остановка:

```bash
docker compose down
```

Приложение по умолчанию ожидается на:

- `http://localhost:8080`

PostgreSQL по умолчанию:

- host: `localhost`
- port: `5432`
- db: `configurator`
- user: `configurator`
- password: `configurator`

## 7. Тестовая стратегия

### 7.1. Локальный интеграционный контур

Команда:

```bash
./gradlew :configurator-integration-tests:test
```

Назначение:

- запуск интеграционных тестов без реального внешнего окружения;
- Spring Boot поднимается in-process;
- БД поднимается через `Testcontainers`;
- HTTP проверяется через `MockMvc`.

### 7.2. Внешний интеграционный контур

Команда:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Назначение:

- прогон тех же интеграционных сценариев против уже поднятого приложения;
- transport идёт по реальному HTTP;
- подготовка данных выполняется SQL-скриптами в PostgreSQL;
- приложение и окружение должны быть уже подняты.

Переопределяемые параметры:

- `test.baseUrl`
- `test.dbUrl`
- `test.dbUser`
- `test.dbPassword`

Пример:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest \
  -Dtest.baseUrl=http://localhost:8080 \
  -Dtest.dbUrl=jdbc:postgresql://localhost:5432/configurator \
  -Dtest.dbUser=configurator \
  -Dtest.dbPassword=configurator
```

### 7.3. Контракт тестов

Интеграционные сценарии для local и external режима должны оставаться одинаковыми.

Если добавляется новый интеграционный кейс:

- он должен быть оформлен как общий контрактный сценарий;
- он должен автоматически работать и в local, и в external режиме;
- нельзя дублировать один и тот же сценарий в двух местах с разной логикой.

### 7.4. SQL fixtures

SQL-фикстуры лежат в:

- `configurator-integration-tests/src/test/resources/sql`

Если нужен новый базовый набор данных для интеграционных тестов:

- добавлять его следует туда;
- по возможности переиспользовать существующие `clear-db.sql` и `insert-*`-скрипты;
- fixtures должны быть детерминированными и минимальными.

## 8. Definition of Done для изменений

Для завершённой задачи обязательно:

1. проект должен собираться;
2. должны проходить все интеграционные тесты на поднятом контексте:
   - `./gradlew build`
   - `./gradlew :configurator-integration-tests:externalIntegrationTest`

Если задача влияет на API, persistence или инфраструктуру тестов, агент должен исходить из того, что оба пункта обязательны.

Если внешний контур недоступен в текущей среде, агент обязан явно указать это в результате и не скрывать непроверенный статус.

## 9. Правила внесения изменений

### 9.1. Когда меняется API

Если изменяется endpoint, DTO, status code или контракт REST API:

- сначала править `specs/configurator-api.yaml`;
- затем пересобрать/перегенерировать;
- затем адаптировать controller/facade/service/tests.

### 9.2. Когда меняется БД

Если меняется схема БД:

- нельзя править jOOQ generated classes;
- нужно добавить новую Flyway-миграцию;
- затем пересобрать проект;
- затем адаптировать persistence/service/tests.

### 9.3. Когда меняются интеграционные тесты

- нельзя разводить local и external сценарии по смыслу;
- различаться должен только transport/setup;
- кейсы должны оставаться эквивалентными.

### 9.4. Когда меняется архитектура

Любые архитектурные изменения должны сохранять текущий принцип:

- layered architecture with hexagonal elements;
- `controller -> facade -> service -> port -> infrastructure`

Если изменение толкает проект к другому стилю архитектуры, это должно быть явно осознано и отдельно согласовано.

## 10. Git-правила

### 10.1. Именование веток

Для обычных задач:

- `feature/CON<версия>-<номер задачи>`

Пример:

- `feature/CON1-36`

Для багфиксов:

- `bugfix/CON<версия>-<номер задачи>`

### 10.2. Формат commit message

Формат обязателен:

- `CON<версия>-<номер задачи> <описание на английском в прошедшем времени>`

Пример:

- `CON1-36 Added external integration contract tests`
- `CON1-36 Refactored integration specs into shared contracts`

Если агент делает коммит, он должен соблюдать именно этот формат.

### 10.3. Правила ветвления

- прямой push в `main` запрещён;
- работа ведётся в отдельных ветках;
- затем изменения попадают через PR;
- feature/bugfix ветки вливаются в `dev`;
- при релизе `dev` вливается в `master`.

Формат PR в проекте отдельно не зафиксирован.

## 11. Что агент должен проверять перед правкой

Перед изменениями агент должен:

1. понять, меняется ли OpenAPI-контракт;
2. понять, меняется ли схема БД;
3. понять, меняется ли generated-код косвенно;
4. определить, затрагивается ли архитектурная граница между слоями;
5. определить, нужно ли обновить integration contracts.

## 12. Что агент должен сообщать пользователю

После значимых изменений агент должен явно сообщать:

- что именно изменено;
- затронут ли OpenAPI;
- затронута ли БД/миграции;
- какие команды проверки выполнены;
- что не удалось проверить, если такое есть.

Нельзя выдавать изменения за проверенные, если фактически `build` или `externalIntegrationTest` не были прогнаны.

## 13. Практические правила для этого репозитория

- Не редактировать `build/generated/**`.
- Не обходить facade из controller.
- Не класть persistence-реализацию обратно в domain/application слой.
- Не добавлять новые integration tests в виде дублирующих local/external сценариев.
- Не ломать существующую package-структуру без необходимости.
- Предпочитать небольшие, локальные изменения перед архитектурным “переписыванием всего”.
- При изменении тестовой инфраструктуры проверять оба режима тестов.

## 14. Приоритет правил

Если агенту нужно выбрать между скоростью и соблюдением архитектурных/генерационных правил:

- приоритет у архитектуры, source-of-truth и воспроизводимости.

Если есть сомнение, где должен жить новый код:

- transport -> `api.inbounds.rest`
- DTO boundary/mapping -> `application.facade` / `application.mapper`
- use case logic -> `application.service`
- outbound abstraction -> `application.port.out`
- domain concepts -> `domain.model`
- infra implementation -> `infrastructure.persistence`

