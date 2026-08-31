# Configurator

[![CI](https://github.com/eltgm/configurator-root/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/eltgm/configurator-root/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4.11](https://img.shields.io/badge/Spring%20Boot-3.4.11-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0-6BA539?logo=openapiinitiative&logoColor=white)](specs/configurator-api.yaml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Backend-first конфигуратор компонентов с React-интерфейсом: каталог предметных областей и компонентов, ручные и
атрибутивные правила совместимости, поиск совместимых наборов и сохранение конфигураций.

> [!IMPORTANT]
> `v1.2.0` — подготовленный release candidate локального продукта для одного доверенного пользователя. Поддерживаемая
> поставка — только Windows/macOS пакет с Docker Desktop и loopback-адресом `127.0.0.1`; LAN, публичная сеть и
> серверное развёртывание не входят в контракт этой версии.

## Возможности

| Область          | Состояние   | Возможности                                                                      |
|------------------|-------------|----------------------------------------------------------------------------------|
| Домены           | Реализовано | создание, чтение, обновление, удаление, пагинация                                |
| Типы компонентов | Реализовано | CRUD внутри домена                                                               |
| Атрибуты         | Реализовано | доменный каталог, reuse между типами, независимые настройки связей, удаление     |
| Компоненты       | Реализовано | создание, получение, обновление, архивирование, списки и фильтрация              |
| Изображения      | Реализовано | загрузка в MinIO, выбор заглавного изображения, уменьшенные превью                          |
| Совместимость    | Реализовано | ручные связи, граф, CRUD атрибутивных правил, блокирующие решения и объяснения   |
| Конфигуратор     | Реализовано | assembly-aware подбор, замена, direct/transitive поиск и пересечение наборов     |
| Конфигурации     | Реализовано | создание, список, получение и экспорт в JSON                                     |
| Web-интерфейс    | Реализовано | каталог, совместимость, конфигуратор, конфигурации, настройки, mobile/desktop UX |

Web-интерфейс покрывает полный локальный сценарий: создание пустой или демонстрационной предметной области, управление
типами, общим каталогом атрибутов и компонентами, переиспользование атрибутов между типами, изображения, ручные и
автоматические правила совместимости, граф, assembly-aware подбор с объяснением блокировок,
direct/transitive/batch/intersection поиск, сохранение, копирование, экспорт и удаление конфигураций. Поддерживаются
светлая, тёмная и системная темы, карточки/таблица, desktop/mobile navigation и reflow от 320 CSS px.

Системное имя атрибута (`name`) уникально внутри предметной области с учётом регистра. Чтобы использовать
характеристику в нескольких типах компонентов, подключайте одно определение из каталога. Создание дубликата
или переименование в занятое имя возвращает `409` с указанием поля `name`.

При обновлении до схемы V8 одинаковые прежние определения объединяются с сохранением значений и ссылок.
Сделайте резервную копию перед обновлением. Если определения или их ссылки конфликтуют, миграция остановится
без частичных изменений и укажет область, имя и ID для согласованного исправления данных. Автоматическое
удаление значений и переименование несовместимых атрибутов не выполняются.

## Удаление предметной области

Удаление предметной области требует точного ввода её названия в интерфейсе. Наличие любой сохранённой
конфигурации (включая пустую, чужую или содержащую архивные компоненты) блокирует удаление с
`409 DOMAIN_HAS_CONFIGURATIONS`: сначала нужно удалить конфигурации. Иначе все оставшиеся типы, атрибуты,
активные и архивные компоненты, значения, изображения, правила и связи удаляются вместе с областью.
Изменения PostgreSQL и постановка заданий очистки выполняются в одной транзакции. Миграция V9 дополнительно
запрещает каскадное удаление конфигураций на уровне FK; существующие конфигурации сохраняются.

Оригиналы и превью MinIO удаляет фоновый обработчик очереди `component_image_cleanup`: до 100 заданий за
проход, по умолчанию каждые 5 секунд; неудачная попытка откладывается на минуту без ограничения числа повторов.
Очередь переживает перезапуски. Недоступность MinIO не отменяет успешное удаление области; файлы могут
оставаться в хранилище до восстановления доступа. Для диагностики доступны `attempts`, `next_attempt_at`
и предупреждения в журнале. Настройки Spring: `app.storage.component-images.cleanup.enabled` (по умолчанию
`true`) и `app.storage.component-images.cleanup.delay-ms` (по умолчанию `5000`). При отключённом обработчике
задания сохраняются, но не исполняются до его включения.

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
- OpenAPI 3.0 и OpenAPI Generator;
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
├── delivery/                       # шаблоны Windows/macOS пользовательских пакетов
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

## Запуск пользовательского пакета

Пакет рассчитан на Windows 10/11 x86-64 и macOS Intel/Apple Silicon. Пользователю нужен только Docker Desktop и
интернет при первом запуске и обновлении; JDK, Gradle, Node.js, npm и Git не требуются.

1. Откройте [GitHub Releases](https://github.com/eltgm/configurator-root/releases), скачайте архив для своей ОС и
   полностью распакуйте папку `Configurator`.
2. Запустите `Start.cmd` в Windows или `Start.command` в macOS двойным кликом.
3. Дождитесь сообщения о готовности: браузер откроет <http://127.0.0.1:8080>.

Рядом доступны `Stop`, `Update`, `Backup` и `Restore`. Update всегда создаёт backup до загрузки stable-образов;
Restore проверяет контрольные суммы и сначала создаёт страховочный backup. Backups не зашифрованы. На macOS при
первом предупреждении Gatekeeper используйте правый клик → «Открыть», не отключая системную защиту.

Tag workflow собирает публичные multi-platform app/gateway images, прикладывает оба архива, `IMAGE_DIGESTS` и
`SHA256SUMS` к draft release. Для локальной проверки структуры архивов разработчик может выполнить:

```bash
scripts/release/build-delivery-packages.sh 1.1.3
```

Подробности эксплуатации и recovery: [`docs/release/LOCAL_DELIVERY.md`](docs/release/LOCAL_DELIVERY.md).

> Пакет предназначен только для доверенного пользователя на локальном компьютере. Не меняйте loopback-привязку порта
> 8080 и не публикуйте приложение в LAN или интернет.

## Быстрый старт из исходного кода

Для запуска из исходников понадобятся JDK 21 и Docker Desktop с Compose plugin. Отдельно устанавливать Gradle и
Node.js не нужно: frontend собирается внутри gateway image.

```bash
./gradlew :configurator:bootJar
docker compose up -d --build
```

После запуска доступны:

- приложение — <http://127.0.0.1:8080>;
- OpenAPI JSON через gateway — <http://127.0.0.1:8080/api/v3/api-docs>.

Основной Compose публикует только loopback-порт gateway. Backend, PostgreSQL и MinIO доступны внутри Docker network.
Это является обязательной границей поддерживаемого локального сценария.

### Development Compose override

Для прямого доступа к инфраструктуре и container backend используйте development override:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

- backend и Swagger UI — <http://127.0.0.1:8081/swagger-ui/index.html>;
- PostgreSQL — `127.0.0.1:5432/configurator`;
- MinIO API — <http://127.0.0.1:9000>;
- MinIO Console — <http://127.0.0.1:9001>.

Локальные значения `configurator/configurator`, `minioadmin/minioadmin` предназначены только для разработки.

### Запуск web-интерфейса для разработки

После запуска backend откройте второй терминал. Понадобятся Node.js 24 LTS и npm 11:

```bash
cd configurator-web
npm ci
npm run dev
```

Web-интерфейс будет доступен на <http://127.0.0.1:5173>. Dev server проксирует `/api/*` на backend
`http://127.0.0.1:8080`. Эта команда предназначена только для разработки; пользовательский сценарий описан выше.

### Запуск приложения из IDE

Поднимите инфраструктуру и запустите main-класс Spring Boot из IntelliJ IDEA:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres minio
```

Не запускайте gateway в этом сценарии: backend из IDE занимает `127.0.0.1:8080`, а Vite proxy удаляет `/api` и
обращается к нему напрямую. При необходимости задайте переменные окружения в Run Configuration.

## Конфигурация

| Переменная                 | Значение по умолчанию                           | Назначение                   |
| -------------------------- | ----------------------------------------------- | ---------------------------- |
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/configurator` | JDBC URL                     |
| `DB_USER`                  | `configurator`                                  | пользователь БД              |
| `DB_PASSWORD`              | `configurator`                                  | пароль БД                    |
| `IMAGE_STORAGE_ENDPOINT`   | `http://localhost:9000`                         | внутренний endpoint MinIO/S3 |
| `IMAGE_STORAGE_ACCESS_KEY` | `minioadmin`                                    | access key                   |
| `IMAGE_STORAGE_SECRET_KEY` | `minioadmin`                                    | secret key                   |
| `IMAGE_STORAGE_BUCKET`     | `configurator-components`                       | bucket изображений           |

Лимит одного загружаемого файла — 10 MB.
Заглавное изображение — первое в галерее. Кнопка «Сделать заглавным» перемещает выбранное фото в начало.
Каталог и конфигуратор используют PNG-превью до 512 × 512 пикселей; увеличение открывает оригинал.
[Правила выбора, хранения и генерации превью](docs/requirements/component-primary-images.md).

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
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
./gradlew :configurator-integration-tests:externalIntegrationTest
```

Параметры внешнего контура можно переопределить:

```bash
./gradlew :configurator-integration-tests:externalIntegrationTest \
  -Dtest.baseUrl=http://127.0.0.1:8080/api \
  -Dtest.dbUrl=jdbc:postgresql://localhost:5432/configurator \
  -Dtest.dbUser=configurator \
  -Dtest.dbPassword=configurator
```

Local и external режимы используют одни и те же контрактные сценарии; external transport проходит через web gateway,
а development override публикует PostgreSQL на loopback для deterministic SQL fixtures.

### Frontend

```bash
cd configurator-web
npm ci
npm run check
npm run test:coverage
```

`npm run check` проверяет generated API drift, форматирование, ESLint, Stylelint, unit/component tests, TypeScript и
production build. После `npx playwright install` функциональные E2E запускаются `npm run test:e2e`, автоматическая
проверка доступности — `npm run test:accessibility`. Visual regression требует Docker Desktop и запускается
`npm run test:visual`; подробный процесс описан в [`docs/testing/FRONTEND_TESTING.md`](docs/testing/FRONTEND_TESTING.md).
При запущенном полном Compose `npm run test:delivery` проверяет production bundle, reverse proxy и deep-link fallback
без HTTP mocks.

## Разработка

1. Создайте `feature/CON<версия>-<номер>` или `bugfix/CON<версия>-<номер>` от `develop`.
2. Для API сначала измените OpenAPI; для БД сначала добавьте Flyway-миграцию.
3. Соблюдайте цепочку `controller -> facade -> service -> port -> infrastructure`.
4. Добавьте unit-тесты и общий integration contract.
5. Выполните `./gradlew build` и внешний интеграционный контур.
6. Откройте pull request в `develop`.

Подробнее: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Релизы

Проект использует Semantic Versioning. `v1.0.0` зафиксировал стабильный контракт локальной Windows/macOS поставки;
готовящийся релиз — `v1.2.0`.

- рабочая ветка — `develop`;
- стабильная ветка — `master`;
- релиз готовится PR из `develop` в `master`;
- тег формата `vX.Y.Z` ставится только на проверенный commit из `master`;
- workflow проверяет полный backend/frontend/delivery контур;
- публикует public `linux/amd64`/`linux/arm64` app и gateway images в GHCR с exact, commit и `stable` tags;
- добавляет OCI SBOM/provenance и GitHub OIDC keyless attestations;
- создаёт или обновляет draft release с JAR, OpenAPI, Windows/macOS archives, `IMAGE_DIGESTS` и `SHA256SUMS`;
- публикация draft остаётся явным действием владельца после anonymous-pull и clean-machine проверки.

История изменений: [`CHANGELOG.md`](CHANGELOG.md). Актуальный release checklist: [
`docs/release/RELEASE_CHECKLIST.md`](docs/release/RELEASE_CHECKLIST.md).

## Безопасность и поддержка

Уязвимости не следует публиковать в обычных issues. Используйте процедуру из [`SECURITY.md`](SECURITY.md). Границы
поддержки и данные для bug report описаны в [`SUPPORT.md`](SUPPORT.md); для ошибок и предложений предусмотрены issue
templates.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
