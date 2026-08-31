# Единое создание и уникальность имён атрибутов

Статус: выполнено 2026-08-31 в `bugfix/CON1-138`. Пользовательская БД не изменялась.

## Overview

Запретить существование нескольких определений атрибута с одинаковым `name` в одной области,
унифицировать создание и редактирование из всех точек входа, отображать записи без дедупликации
по имени и перевести демо на переиспользуемые определения атрибутов.

Под «типом атрибута» здесь понимается `AttributeDefinition`, а не перечисление `DataType`.
Значения атрибутов компонентов продолжают ссылаться на определения через `attributeDefinitionId`.

## Context

- `AttributeServiceImpl.create` проверяет имя только среди привязок выбранного типа, затем вызывает
  `createInDomain`. Последний не проверяет уникальность имени в области.
- `AttributeServiceImpl.update` проверяет новое имя только в связанных типах. Для определения без
  привязок проверка фактически отсутствует.
- V7 перенесла определения в каталог области, сохранила прежние ID, удалила старый уникальный индекс
  и создала обычный индекс `(domain_id, name)`. Ограничения уникальности в области нет.
- `AttributeFormModal` уже общий для `/settings/attributes` и `/settings/types`, но использует два
  POST endpoint: создание в каталоге и атомарное создание с привязкой к типу.
- В текущей `ComponentForm` редактируются значения существующих определений; отдельного создания
  определений в ней нет. Этот потребитель также включён в проверку отображения и переиспользования.
- `AttributeRepositoryImpl.getByDomainId/getByComponentTypeId` и frontend `sortAttributes` возвращают
  все записи. `AttachAttributeModal` скрывает записи с именами уже связанных атрибутов (`linkedNames`).
- `DemoDomainServiceImpl` создаёт 12 определений, хотя уникальных имён 9: повторяются `socket`,
  `memory_standard`, `form_factor`.
- Дубли `socket` есть в `insert-compatibility-rule-test-data.sql` и `insert-configurator-test-data.sql`;
  связанные значения и условия совместимости используют разные ID.
- Repository-тесты используют H2 и явный список V1–V7 в `AbstractJooqRepositoryTest`.
  Проверка обновления существующей БД должна отдельно выполняться на PostgreSQL 17.
- При начале планирования рабочая копия чистая; текущая ветка `master`. Коммиты и push не выполнялись.

## Решения для подтверждения

1. Уникальность определяется парой **`(domainId, name)`**, независимо от типа компонента, `label`,
   `dataType` и наличия привязок. В разных областях одинаковое имя допустимо.
2. Сравнение `name` остаётся точным, с учётом регистра: `socket` и `Socket` различаются. Изменение
   регистра, Unicode-нормализация и новая политика пробелов не входят в этот план; существующий trim
   в UI сохраняется. Если требуется другая семантика, согласовать её до реализации индекса и миграции.
3. Повторное создание и переименование в занятое имя возвращают `409 ENTITY_ALREADY_EXISTS`.
   Создание не подменяется молчаливой привязкой найденного определения. Для переиспользования есть
   явное действие «Использовать существующий атрибут».
4. Существующие одинаковые определения объединяются безопасной миграцией. Несовместимые дубли
   не удаляются и не переименовываются автоматически: миграция завершается диагностируемой ошибкой
   без частичных изменений, а конфликтующие данные требуют отдельного решения.
5. Одинаковый `label` при разных `name` допустим. Списки не скрывают такие записи и не объединяют
   разные ID по имени или подписи. В выборе для привязки уже связанные записи видны, но недоступны
   для повторного добавления; принадлежность определяется по ID.

## Pre-change check

- **OpenAPI:** изменятся описание области уникальности и ответы 409 для создания/обновления.
  Формат основных DTO и существующие URL сохраняются.
- **БД:** новая versioned migration после V7; опубликованные миграции не редактировать.
- **Generated code:** backend OpenAPI/jOOQ — через Gradle lifecycle; frontend SDK — `npm run api:generate`.
  Не редактировать generated code вручную.
- **Архитектура:** сохраняется `controller -> facade -> service -> outbound port -> infrastructure`.
  Проверка бизнес-правила — в service, SQL/распознавание нарушения ограничения — в persistence.
- **Integration contract:** нужны общие local/external сценарии атрибутов, демо и совместимости.
- **Unrelated changes:** повторить `git status` после согласования и сохранить новые пользовательские изменения.
- **Security/delivery:** схема безопасности, Compose, gateway, release policy и `delivery/**` не меняются.

## Development Approach

- После подтверждения выполнять небольшие логические этапы с регрессионными тестами.
- Использовать существующие сервис, общий UI и error contract; не вводить отдельный параллельный механизм.
- Сохранить атомарность создания определения вместе с привязкой: не заменять его двумя независимыми
  запросами браузера, оставляющими определение при ошибке второго запроса.
- После каждого этапа запускать относящиеся к нему тесты, исправлять ошибки до следующего этапа.
- Обновлять план при уточнении scope; не выдавать недоступные проверки за выполненные.
- До согласования не менять код, спецификацию, миграции, демоданные и тесты.

## Solution Overview / Technical Details

### Единая политика записи

Оба POST endpoint используют одну проверку уникальности каталога области. PUT определения проверяет
тот же инвариант с исключением собственного ID. Привязка существующего ID к другим типам разрешена;
`isRequired` и `orderIndex` остаются настройками каждой связи. Изменение общих свойств определения
по-прежнему распространяется на все его привязки.

Проверка service обеспечивает понятную ошибку, уникальное ограничение БД защищает от параллельных
запросов и прямой записи. Нарушение именно ограничения имени переводится в доменный conflict;
остальные ошибки БД не маскируются. UI одинаково показывает конфликт у поля `name` через общий
механизм нормализации ошибок, не полагаясь только на загруженный каталог.

### Обновление существующих данных

Для каждой группы `(domain_id, name)` выбрать каноническое определение с минимальным ID только
при одинаковых `label`, `data_type` и семантически одинаковом наборе `enum_values` (порядок JSON-массива
не должен мешать объединению). Отличие `created_at` само по себе не конфликт.

До изменения данных проверить возможность переноса всех ссылок:

- `component_type_attribute`: сохранить привязки и индивидуальные настройки каждого типа;
- `attribute_value`: перенести ссылки без изменения значений, включая архивные компоненты;
- `compatibility_rule_condition`: обновить обе стороны, сохранив операторы, порядок и смысл правил;
- проверить конфликты composite keys значений, связей и условий после замены ID.

Если после переноса возникнут неоднозначные настройки, несколько значений одного компонента или
коллизии условий правил, остановить миграцию с указанием области/имени/ID. Не выбирать произвольное
значение и не удалять условия ради прохождения ограничения. Операция транзакционна.

После переноса удалить только заменённые определения и установить уникальное ограничение
`(domain_id, name)`. Сохранить диагностируемое соответствие заменённых ID каноническим. Действующие
канонические ID сохраняются; ссылки на удалённые дубли вне БД потребуют обновления потребителем.
Миграция не обращается к пользовательской рабочей БД во время разработки: сценарии проверяются
на изолированных тестовых БД.

## Implementation Steps

### 1. Ограничение БД и миграция существующих дублей

**Files:**
- Create: `configurator/src/main/resources/db/migration/V8__enforce-domain-attribute-name-uniqueness.sql`
  (номер повторно проверить перед реализацией).
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/AbstractJooqRepositoryTest.java`.
- Create: PostgreSQL/Flyway upgrade test в `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/it/`.
- Modify: SQL fixtures и использующие их contracts в `configurator-integration-tests/src/test/`.

- [x] Реализовать предварительную диагностику, безопасный перенос ссылок и уникальное ограничение.
- [x] Обновить SQL fixtures с переиспользованием ID; адаптировать связанные значения/условия/contracts.
- [x] Адаптировать H2 setup так, чтобы repository-тесты проверяли актуальное ограничение; PostgreSQL
      upgrade test должен применять настоящую Flyway migration, а не её упрощённую копию.
- [x] Добавить тесты fresh install и V7 -> V8 с дублями старого демо, сохранением настроек/значений/правил
      и независимостью одинаковых имён в разных областях.
- [x] Добавить тесты несовместимых определений, коллизий ссылок и отсутствия частичных изменений при ошибке.
- [x] Запустить migration/repository/затронутые contract tests до следующего этапа.

### 2. Единая серверная проверка и контракт ошибок

**Files:**
- Modify: `specs/configurator-api.yaml`.
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/AttributeServiceImpl.java`.
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/AttributeRepository.java`.
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/AttributeRepositoryImpl.java`.
- Modify as needed: существующие domain exception/web advice для field detail `name`.
- Modify: `AttributeServiceImplTest`, `AttributeRepositoryImplTest`, tests advice и `AbstractAttributesControllerContract`.

- [x] Описать уникальность в области и 409 для обоих POST и PUT определения в OpenAPI.
- [x] Регенерировать backend API/jOOQ через Gradle и frontend SDK через `npm run api:generate`.
- [x] Вынести одну серверную проверку имени области для создания и переименования, включая определения без привязок.
- [x] Обрабатывать нарушение DB constraint как 409, в том числе при гонке запросов; сохранить rollback
      создания с привязкой и исключение собственного ID при обновлении.
- [x] Добавить unit/repository tests: оба способа создания, переименование, неизменное имя, другой домен,
      другой тип в том же домене, изменение прочих полей и явное переиспользование определения.
- [x] Расширить единый local/external contract матрицей POST/POST, POST/PUT и проверкой отсутствия лишних
      определений/связей после конфликта. Конкурентную запись проверить на PostgreSQL.
- [x] Запустить относящиеся к этапу проверки.

### 3. Демо с общими определениями

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/DemoDomainServiceImpl.java`.
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/application/service/DemoDomainServiceImplTest.java`.
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractDomainControllerContract.groovy`.

- [x] Создать 9 определений в каталоге и 12 привязок к типам; `socket`, `memory_standard`, `form_factor`
      создавать по одному разу и подключать к обоим соответствующим типам.
- [x] Сохранить индивидуальные порядки/обязательность, значения 12 компонентов, правила, ручные связи
      и сохранённую конфигурацию; правила должны использовать общий ID на обеих сторонах там, где это нужно.
- [x] Проверить тестами количество и уникальность определений каталога, общие ID между типами,
      неизменность 12 привязок и работоспособность совместимости/конфигурации.
- [x] Сохранить транзакционность и существующий отказ повторного создания демо; выполнить тесты этапа.

### 4. Общий UI и полные списки

**Files:**
- Modify: `configurator-web/src/features/attributes/ui/AttributeFormModal.tsx`.
- Modify: `configurator-web/src/features/attributes/ui/AttachAttributeModal.tsx`.
- Modify as needed: `configurator-web/src/features/attributes/api/attributes.ts`, `configurator-web/src/shared/api/errors.ts`.
- Modify: `configurator-web/src/shared/i18n/resources.ts`.
- Modify: tests страниц attributes/types, формы компонента, attributes API и attach modal.
- Modify: `configurator-web/e2e/attributes.spec.ts`, `configurator-web/e2e/fixtures/mock-api.ts` и tests mock API.

- [x] В общей форме одинаково показывать конфликт `name` в режимах создания/редактирования каталога и типа;
      объяснять возможность подключить существующий атрибут, не создавать дубликат автоматически.
- [x] Удалить фильтрацию `linkedNames`; отображать все определения каталога в выборе с disabled-состоянием
      уже привязанных ID и понятной локализованной подписью.
- [x] Сохранить полную выдачу в catalog/type/component/compatibility views: сортировка допустима,
      скрытие разных записей по `name`/`label` запрещено. Предметные фильтры совместимости не удалять.
- [x] Добавить регрессионные UI-тесты с разными ID и одинаковыми `name`/`label` в mock-ответах, чтобы
      отображение не скрывало даже некорректные данные сервера. В рабочих DB fixtures дубли не создавать.
- [x] Проверить одинаковый 409 в обоих местах, переименование, выбор общего определения и обновление
      query cache с `domainId`. Обновить mock API под те же ограничения.
- [x] Добавить E2E создания из обеих точек входа и переиспользования в нескольких типах; выполнить tests этапа.

### 5. Итоговая проверка и документация

**Files:**
- Modify: этот план; пользовательская документация/`CHANGELOG.md` при наличии относящегося описания поведения.

- [x] Выполнить `./gradlew build` и `./gradlew :configurator-integration-tests:test`.
- [x] После запуска актуального полного Compose выполнить
      `./gradlew :configurator-integration-tests:externalIntegrationTest`.
- [x] В `configurator-web` выполнить `npm ci`, `npm run api:check`, `npm run check`,
      `npm run test:coverage`, `npm run test:e2e`, `npm run test:accessibility`.
- [x] Выполнить visual regression в pinned Docker image и `npm run test:delivery` на полном Compose;
      не обновлять baselines без анализа ожидаемых изменений.
- [x] Подтвердить JaCoCo minimum line coverage 0.90 и отсутствие drift generated API.
- [x] Зафиксировать изменения OpenAPI/БД, фактически выполненные проверки, непроверенные сценарии
      и release blockers; после завершения перенести план в `docs/plans/completed/`.

## Progress Tracking

Отмечать выполненные пункты `[x]` после проверки результата. Новые задачи — `[+]`, блокеры — `[!]`.
Если согласованная политика обработки дублей меняется, сначала обновить раздел решений и migration tests.

## Post-Completion / ограничения

- Перед обновлением пользовательской БД нужна резервная копия. При обнаружении несовместимых дублей
  обновление остановится до их согласованного исправления; автоматическое удаление области/демо не предлагается.
- Изменение уникальности ужесточает прежнее API-поведение. Внешние потребители должны переиспользовать
  существующий `attributeDefinitionId`, а не создавать одноимённое определение для каждого типа.
- Runtime-аутентификация по-прежнему не реализована; статус production-ready остаётся ограниченным
  trusted-local Windows/macOS поставкой с loopback gateway.
- Выпуск/публикация, изменение branch protection и release workflows не входят в задачу.
- На этапе планирования тесты не запускались, состояние пользовательской БД и работающего приложения
  не проверялось. Никакие существующие дубли не изменены.

## Журнал реализации

- PostgreSQL 17: `AttributeNameMigrationSpec` прошёл (fresh install, безопасное объединение и шесть отказов с rollback).
- jOOQ DDL interpreter пропускает только PostgreSQL data block через стандартные ignore-маркеры; H2 применяет DDL из той же миграции.
- Найдены дополнительные дубли в repository fixtures и `feature` в configurator fixture; заменены общими ID.
- `npm ci` выполнен, audit: 0 vulnerabilities.

- Полный Gradle build прошёл; backend JaCoCo line coverage 93.31%.
- Полный external integration прошёл на изолированном Compose `configurator-con1-138` (gateway 18080, PostgreSQL 15432), без изменения рабочего Compose.
- Visual regression: 8/8, baselines без изменений.

- Backend: 435/435, local integration: 223/223, external integration: 218/218; без skipped.
- Полный E2E: 78/78 (Chromium, Firefox, WebKit); delivery smoke: 1/1.
- Изолированный Compose и его тестовые volumes удалены после проверок. Рабочий Compose не изменялся.
- Промежуточные frontend-прогоны выявили JSDOM dropdown без layout и неоднозначный E2E locator; тесты уточнены.
  При конкурентной нагрузке также наблюдался timeout существующего теста темы; финальный прогон выполняется последовательно.

## Итоговая проверка

Все запланированные проверки выполнены:

| Проверка | Результат |
| --- | --- |
| `./gradlew build` | успешно; 435 backend + 223 local integration tests |
| `externalIntegrationTest` | 218 тестов через отдельный Compose/gateway |
| Backend JaCoCo lines | 93.31%, минимум 90% соблюдён |
| `npm ci` | успешно, audit без уязвимостей |
| `npm run check` | успешно, 44 файла / 224 теста; SDK без drift, format/lint/styles/typecheck/build прошли |
| `npm run test:coverage` | 224 теста; lines 90.77%, statements 90.22%, branches 84%, functions 89.07% |
| `npm run test:e2e -- --workers=2` | 78 тестов, Chromium/Firefox/WebKit |
| `npm run test:accessibility -- --workers=2` | 36 тестов, desktop/mobile |
| `npm run test:visual` | 8 тестов в pinned Docker image, baselines не менялись |
| `npm run test:delivery` | 1 тест на изолированном Compose |
| `git diff --check` | без ошибок |

Финальные frontend check/coverage/accessibility выполнены последовательно и успешно после промежуточных
сбоев тестовых ожиданий. Production-код темы интерфейса и общие таймауты существующих тестов не менялись.

OpenAPI и схема БД обновлены, backend/frontend clients регенерированы штатными командами. Старые миграции,
архитектура, runtime security и delivery/release contracts не изменены. Для external tests использовался
временный Gradle init script с `test.baseUrl=http://127.0.0.1:18080/api`,
`test.gatewayUrl=http://127.0.0.1:18080`, `test.dbUrl=jdbc:postgresql://127.0.0.1:15432/configurator`.
Host-specific настройки остались вне репозитория. Изолированные test containers/volumes удалены;
исходные контейнеры продолжают работать.

Непроверенным остаётся обновление конкретной пользовательской БД: оно намеренно не выполнялось.
Несовместимые прежние определения или коллизии ссылок требуют исправления данных перед обновлением;
V8 выдаёт диагностику и не оставляет частичных изменений. Ограничение trusted-local сохраняется.
Публикация релиза и push не выполнялись.
