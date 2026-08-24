# CON1-127 — Web Gateway, Reverse Proxy and Production Docker Image

## Overview

Пункт 9.28 добавляет production web gateway для уже реализованного React/Vite frontend. Gateway раздаёт собранную SPA,
проксирует same-origin запросы `/api/*` во внутренний Spring Boot backend и становится единственной внешней точкой входа
`http://127.0.0.1:8080`.

Основной Docker Compose переводится на production-like topology: backend, PostgreSQL и MinIO доступны только внутри
Docker network. Отдельный development override возвращает loopback-доступ к PostgreSQL, MinIO и backend на порту 8081,
не ослабляя default topology. Пользовательские Start/Stop/Update/Backup/Restore пакеты относятся к 9.29, а публикация
release multi-platform images и финальный release pipeline — к 9.30.

OpenAPI, backend business logic, Flyway, jOOQ и схема БД в 9.28 не меняются.

## Context (from discovery)

- В `docs/requirements/epic-9-frontend.md` зафиксированы название 9.28 и общая архитектура поставки, но подробные
  acceptance criteria для gateway пока отсутствуют.
- `configurator-web` уже создаёт production bundle в `dist`, а generated Fetch client использует относительный
  `baseUrl=/api`. Runtime-подмена URL или CORS не требуются.
- Vite development proxy сейчас удаляет `/api` и обращается к backend на `127.0.0.1:8080`; этот IDE-сценарий должен
  сохраниться.
- Spring Boot публикует endpoint-ы от корня (`/domains`, `/components`, `/v3/api-docs`), поэтому production gateway
  должен детерминированно удалять только ведущий `/api`.
- Текущий `docker-compose.yml` публикует backend `8080`, PostgreSQL `5432`, MinIO `9000/9001`. Это не соответствует
  принятому product contract с одной loopback entry point.
- Текущий root `Dockerfile` является runtime-only backend image и требует заранее собранный boot JAR. Его conversion в
  source-building image не входит в 9.28: release packages 9.29 будут использовать заранее опубликованные images, а
  их multi-arch build/publish pipeline относится к 9.30.
- Backend не использует Actuator и не имеет отдельного health endpoint. Gateway может иметь собственный lightweight
  liveness endpoint, но фактическая API readiness должна проверяться через proxied `/api/v3/api-docs`.
- External integration contracts и CI сейчас обращаются прямо к `http://localhost:8080`; после изменения topology они
  должны проходить через gateway prefix `/api`, сохраняя общий local/external contract.
- Официальный `nginx/docker-nginx-unprivileged` image работает non-root, по умолчанию слушает 8080, поддерживает
  `linux/amd64` и `linux/arm64` и рекомендует digest pinning для воспроизводимости.
- Amplicode IDE model сообщил Spring Boot 4.1.0, но project source of truth (`build.gradle`, `AGENTS.md`, README) фиксирует
  Spring Boot 3.4.11; план опирается на source of truth.
- Единственное unrelated local изменение —
  `configurator-integration-tests/src/test/resources/testcontainers.properties`; его нельзя редактировать, форматировать,
  индексировать или включать в commit.

Official references:

- NGINX unprivileged image: https://github.com/nginx/docker-nginx-unprivileged
- NGINX `proxy_pass`: https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass
- NGINX `try_files`: https://nginx.org/en/docs/http/ngx_http_core_module.html#try_files
- Docker multi-platform builds: https://docs.docker.com/build/building/multi-platform/
- Docker multi-stage builds: https://docs.docker.com/build/building/multi-stage/

## Proposed Product Contract

1. Production frontend поставляется отдельным multi-stage image: Node 24 builder выполняет reproducible `npm ci` и
   `npm run build`, final stage содержит только static bundle и unprivileged NGINX.
2. Node/npm и исходный frontend отсутствуют в runtime layer; оба base image задаются exact version и digest.
3. Gateway слушает непривилегированный порт 8080 и запускается non-root. Compose включает `read_only`,
   `no-new-privileges`, `cap_drop: ALL` и только необходимые tmpfs/write paths.
4. Единственный default host bind — `127.0.0.1:8080:8080` у gateway. Backend, PostgreSQL, MinIO API и MinIO Console не
   имеют host ports в основном Compose.
5. `docker-compose.dev.yml` добавляет только loopback developer ports: backend `8081`, PostgreSQL `5432`, MinIO
   `9000/9001`. Gateway продолжает занимать `8080`, поэтому full development overlay не создаёт port conflict.
6. `/api/anything` проксируется в `http://app:8080/anything`; `/api` и `/api/` обрабатываются явно и не попадают в SPA
   fallback.
7. HTTP method, query, body, multipart upload, response status/content type/body и streaming image content проходят
   через gateway без прикладного преобразования.
8. `client_max_body_size` учитывает backend limit 10 MB и multipart overhead; предлагаемый gateway limit — 16 MB.
   Backend по-прежнему остаётся окончательным источником file validation.
9. Любой существующий static asset отдаётся как файл; неизвестный non-API deep link возвращает `index.html`, чтобы
   React Router мог восстановить маршрут после refresh/direct navigation.
10. Неизвестный или ошибочный `/api/*` возвращает backend/proxy error и никогда не маскируется HTML-страницей.
11. `index.html` и SPA fallback используют `Cache-Control: no-cache`; hashed `/assets/*` — долгий
    `public, max-age=31536000, immutable` cache.
12. Gateway включает gzip для текстовых ресурсов и корректные MIME types, но не преобразует уже сжатые binary images.
13. Security headers задаются централизованно и проверяются контрактом: `X-Content-Type-Options`, frame protection,
    `Referrer-Policy`, `Permissions-Policy` и совместимая с текущим UI Content Security Policy.
14. CSP разрешает только same-origin application/API resources и необходимые `data:`/`blob:` image previews; любые
    `unsafe-inline` исключения должны быть минимальными и подтверждены фактическим production bundle.
15. Gateway предоставляет lightweight `/healthz` для собственной liveness. Полная readiness проверяется через
    `/api/v3/api-docs`, потому что backend Actuator не добавляется.
16. Container logs пишутся в stdout/stderr; access log не содержит request bodies, credentials или response payloads.
17. Same-origin gateway устраняет необходимость production CORS configuration. Forwarded headers передаются явно, но
    публичный API contract не меняется.
18. External integration tests выполняют существующие REST contracts через gateway. Отдельный delivery contract
    проверяет SPA root/deep links, API-prefix isolation, headers, cache policy, liveness и статический asset.
19. Development Vite proxy и IDE backend на 8080 остаются работоспособными. Backend container из development overlay
    доступен напрямую на 8081 для Swagger/diagnostics, но не является пользовательской entry point.
20. Image должен собираться на текущей архитектуре и не содержать architecture-specific artifacts; публикация единого
    `linux/amd64` + `linux/arm64` manifest выполняется в 9.30.
21. Docker Dependabot получает отдельную запись для `/configurator-web`, чтобы обновлять nested Dockerfile base images.
22. Runtime authentication/authorization не появляется: приложение остаётся local-only preview и не описывается как
    production-ready для недоверенной сети.

## Considered Approaches

### A. Separate unprivileged NGINX gateway — selected

- Multi-stage frontend image собирает Vite bundle и копирует его в unprivileged NGINX runtime.
- NGINX раздаёт SPA и проксирует `/api` во внутренний backend service.
- Плюсы: соответствует принятой архитектуре, минимальный runtime, same-origin API, независимые frontend/backend images,
  зрелые cache/proxy controls, готовность к amd64/arm64.
- Минусы: появляется отдельная NGINX configuration и ещё один container, которые нужно тестировать как delivery code.

### B. Caddy gateway

- Caddy раздаёт static bundle и reverse-proxy API.
- Плюсы: короткая конфигурация, удобный automatic HTTPS.
- Минусы: automatic HTTPS не нужен для loopback HTTP, runtime крупнее и менее знаком текущему проекту; преимуществ для
  local-only сценария недостаточно.

### C. Embed frontend into Spring Boot JAR

- Vite bundle копируется в Spring resources и отдаётся backend.
- Плюсы: один application container.
- Минусы: нарушает уже принятое решение об отдельном gateway, связывает npm и Gradle lifecycle, усложняет cache/SPA
  fallback и будущие независимые обновления. Не выбран.

## Development Approach

- **Testing approach:** Regular — сначала локальная реализация каждого инфраструктурного блока, затем его focused
  container/delivery contracts; все проверки этапа должны пройти до перехода дальше.
- Выполнять задачи небольшими логическими шагами и сразу отмечать фактический прогресс в этом плане.
- Не ослаблять существующие frontend/backend quality gates ради прохождения gateway tests.
- Любое отклонение в public URL, Compose topology, security headers, upload limit или release boundary сначала
  согласовать с разработчиком.
- Для каждого изменённого исполняемого контракта добавить автоматическую success/error/edge проверку.
- Не изменять unrelated user file `testcontainers.properties` и не включать его в stage/commit.

## Solution Overview

```text
browser
  |
  | http://127.0.0.1:8080
  v
unprivileged NGINX gateway
  |-- /, /assets/*, SPA deep links --> Vite static bundle
  |
  `-- /api/* --strip /api---------> Spring Boot app:8080
                                           |
                                           +--> PostgreSQL:5432
                                           `--> MinIO:9000

default Compose host ports: gateway 8080 only
development override: app 8081, PostgreSQL 5432, MinIO 9000/9001 (loopback only)
```

## Technical Details

### URL routing

| External request                      | Gateway behavior                                      |
| ------------------------------------- | ----------------------------------------------------- |
| `GET /`                               | return `index.html`, no-cache                         |
| `GET /assets/<content-hash>.js`       | return static asset, immutable cache                  |
| `GET /components/123`                 | SPA fallback to `index.html`, keep browser URL        |
| `GET /api/domains?page=0&size=100`    | proxy to `app:8080/domains?page=0&size=100`           |
| `POST /api/components/123/images`     | proxy multipart body, gateway limit 16 MB             |
| `GET /api/component-images/7/content` | proxy status, content type, length/body from backend  |
| `GET /api/unknown`                    | backend/proxy response; never `index.html`            |
| `GET /healthz`                        | gateway liveness response, no backend business access |

The trailing slash on `proxy_pass http://app:8080/` is intentional: NGINX replaces the matching `/api/` URI prefix
with `/`. Tests must lock this behavior because changing either slash silently changes every API route.

### Image construction

- Build context: `configurator-web` only; root secrets, Gradle output and unrelated files cannot enter the build context.
- Builder copies `package.json`/`package-lock.json`, runs `npm ci`, then copies frontend sources/generated client and
  executes `npm run build`.
- Runtime copies only `dist` and the reviewed gateway config into a digest-pinned unprivileged NGINX Alpine image.
- A frontend-local `.dockerignore` excludes `node_modules`, `dist`, reports, coverage, test results, IDE/OS metadata and
  local environment files.
- Runtime image exposes 8080 and contains a deterministic liveness check only if its required binary is guaranteed by
  the pinned base image; otherwise Compose performs the check without installing extra packages.

### Compose topology

- Base Compose builds `app` from the existing root Dockerfile and `gateway` from `configurator-web/Dockerfile`.
- `gateway` depends on `app`, publishes only `127.0.0.1:8080:8080`, uses read-only filesystem/tmpfs and receives no DB
  or storage credentials.
- `app`, `postgres` and `minio` use internal service discovery and named volumes without base host ports.
- Development override adds loopback port mappings only. It does not change credentials, volumes or service names.
- Compose service names, not fixed `container_name` values, are the internal DNS contract; removal of unnecessary fixed
  names is allowed only if scripts/tests do not depend on them.

### External contracts

- External REST base changes from direct backend to gateway API prefix. Prefer one shared configuration point instead of
  duplicating a new literal across every spec; IDE and Gradle launches must receive the same default.
- Existing local integration tests continue exercising Spring Boot in-process without NGINX.
- Existing external tests exercise the same business scenarios through NGINX, proving method/query/body/response
  transparency.
- A focused gateway external spec validates static HTML, one real built asset, deep-link fallback, API 404 isolation,
  cache/security headers and liveness.
- CI/release readiness URL becomes `/api/v3/api-docs`; a successful `/healthz` alone is insufficient.

## What Goes Where

- `configurator-web/Dockerfile` — reproducible multi-stage frontend/gateway image.
- `configurator-web/.dockerignore` — minimal frontend build context.
- `configurator-web/nginx/default.conf` — SPA, proxy, cache, compression, limits and headers.
- `docker-compose.yml` — default production-like internal topology and gateway entry point.
- `docker-compose.dev.yml` — optional loopback-only developer ports.
- `configurator-integration-tests/src/externalTest` — gateway and proxied external contracts.
- `.github/workflows/ci.yml`, `.github/workflows/release.yml` — gateway readiness path and actual Compose contract.
- `.github/dependabot.yml` — nested frontend Dockerfile dependency updates.
- `docs/requirements/epic-9-frontend.md` — detailed 9.28 acceptance criteria.
- `README.md`, `CONTRIBUTING.md`, `AGENTS.md` — production-like and development workflows.

## Testing Strategy

- **Static/frontend:** `npm ci`, `npm run api:check`, `npm run check`, `npm run test:coverage`.
- **Backend/local contracts:** `./gradlew build`; no business logic is expected to change, but shared delivery and docs
  changes must not break the existing build.
- **Image:** build gateway from clean frontend context, validate NGINX config/startup and inspect runtime user/layers.
- **Compose:** validate merged base/development models and inspect actual host port bindings.
- **External REST:** build boot JAR, start full base Compose, wait for `/api/v3/api-docs`, run all existing external
  integration contracts through `/api`.
- **Gateway delivery:** assert root HTML, built asset, deep link, `/api` pass-through/error isolation, upload allowance,
  cache/security headers and `/healthz`.
- **Browser smoke:** open the built application through `127.0.0.1:8080` and confirm that the initial domains request
  uses `/api`; existing mock-based functional/axe/visual suites remain unchanged unless a runtime regression appears.
- **Platform:** inspect/build for current platform in 9.28; `linux/amd64` + `linux/arm64` manifest publication is verified
  in 9.30.
- **Hygiene:** `git diff --check`, image history/config inspection, no secrets/host paths, no unrelated staged files.

## Progress Tracking

- Mark completed items with `[x]` immediately after implementation and verification.
- Add newly discovered work with `[+]`; mark blockers with `[!]`.
- Update this plan whenever scope or architecture changes.
- Do not move to the next task while focused checks for the current task fail.

## Implementation Steps

### Task 1: Finalize the 9.28 delivery contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260824-web-gateway-production-image.md`

- [x] add detailed 9.28 acceptance criteria from Proposed Product Contract
- [x] explicitly separate 9.28 image/topology work from 9.29 scripts and 9.30 publishing/release automation
- [x] document OpenAPI/DB/security non-goals and local-only trust boundary
- [x] verify requirement wording against accepted epic-level architecture and 9.27/9.29 boundaries
- [x] run documentation formatting check before Task 2

### Task 2: Build the unprivileged frontend gateway image

**Files:**

- Create: `configurator-web/Dockerfile`
- Create: `configurator-web/.dockerignore`
- Create: `configurator-web/nginx/default.conf`
- Modify: `.github/dependabot.yml`
- Create/Modify: focused gateway configuration tests or validation scripts as implementation requires

- [x] add digest-pinned Node 24 builder with lockfile-first `npm ci` and production build
- [x] add digest-pinned unprivileged NGINX runtime containing only `dist` and gateway config
- [x] configure explicit `/api` prefix stripping, SPA fallback, 16 MB request limit and liveness
- [x] configure deterministic caching, compression, MIME behavior, proxy headers and security headers
- [x] keep runtime non-root and compatible with read-only root filesystem/tmpfs
- [x] add nested Docker Dependabot coverage for `/configurator-web`
- [x] add success/error checks for NGINX config, runtime user and build-context exclusions
- [x] build and start the gateway image successfully before Task 3

### Task 3: Introduce production-like and development Compose topologies

**Files:**

- Modify: `docker-compose.yml`
- Create: `docker-compose.dev.yml`
- Modify as needed: `.dockerignore`
- Create/Modify: focused Compose topology validation script/test if needed

- [x] add gateway service built from `configurator-web` and bind only `127.0.0.1:8080`
- [x] remove base host ports from app, PostgreSQL and MinIO without changing named data volumes
- [x] harden gateway with `read_only`, `no-new-privileges`, dropped capabilities and minimum tmpfs paths
- [x] add loopback-only development ports including backend `8081` without default conflicts
- [x] preserve internal DNS, service dependencies and backend DB/MinIO environment contract
- [x] validate base and merged development Compose models
- [x] add checks proving base publishes only gateway and development override publishes only documented loopback ports
- [x] start both intended service selections successfully before Task 4

### Task 4: Route external integration contracts through the gateway

**Files:**

- Modify: `configurator-integration-tests/build.gradle`
- Modify as needed: `configurator-integration-tests/src/externalTest/groovy/**`
- Create: `configurator-integration-tests/src/externalTest/groovy/ru/sultanyarov/configurator/external/GatewayExternalIntegrationSpec.groovy`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/release.yml`

- [x] centralize external gateway base URI/base path while preserving `test.baseUrl` override compatibility
- [x] run host-side SQL fixture setup with the loopback-only development Compose override while keeping all REST
      contracts on the gateway API base
- [x] run existing external API scenarios through `/api` without changing shared business assertions
- [x] update CI/release readiness to wait for proxied `/api/v3/api-docs`
- [x] add gateway contract for root HTML, built asset, deep link and liveness success cases
- [x] add API-prefix isolation and unknown API/proxy error cases proving no SPA masking
- [x] assert cache, content type and security headers for representative responses
- [x] verify multipart upload and image content response through gateway
- [x] run all external contracts successfully before Task 5

### Task 5: Verify the production browser boundary

**Files:**

- Create/Modify as needed: `configurator-web/e2e/delivery/**`
- Create/Modify as needed: `configurator-web/playwright.delivery.config.ts`
- Modify: `configurator-web/package.json`
- Modify: `.github/workflows/ci.yml`

- [x] add a focused Playwright delivery smoke against an already running production gateway without mock routes
- [x] verify SPA bootstrap and first `/api` request from `http://127.0.0.1:8080`
- [x] verify direct navigation/refresh on representative nested route
- [x] keep delivery suite separate from deterministic mock functional/accessibility/visual suites
- [x] add failure diagnostics without credentials or persistent user data
- [x] run delivery smoke against the full Compose stack before Task 6

### Task 6: Document the gateway and development workflows

**Files:**

- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `AGENTS.md`
- Create/Modify as needed: `docs/testing/FRONTEND_TESTING.md`

- [x] document default production-like entry point and internal-only services
- [x] document development override ports and commands, including backend container on 8081
- [x] preserve the Vite + IDE backend workflow on 8080 and explain when the gateway is or is not started
- [x] document image build, gateway delivery checks and troubleshooting for 502/startup/cache issues
- [x] repeat current authentication limitation and prohibit exposure to untrusted networks
- [x] update project/testing Definition of Done for Docker delivery changes
- [x] run documentation formatting and link/path checks before Task 7

### Task 7: Verify all acceptance criteria

**Files:**

- Modify: `docs/plans/20260824-web-gateway-production-image.md`
- Move after completion: `docs/plans/20260824-web-gateway-production-image.md` to `docs/plans/completed/`

- [x] run `npm ci`, `npm run check` and `npm run test:coverage`
- [x] run `./gradlew build`
- [x] build boot JAR and both Docker images from clean contexts
- [x] start base Compose and wait for `/api/v3/api-docs`
- [x] run all external integration contracts through gateway
- [x] run production delivery Playwright smoke
- [x] verify SPA/cache/security/upload/image/unknown-API gateway cases
- [x] inspect runtime user, health, published ports, read-only filesystem and image contents
- [x] validate merged development Compose and loopback-only port bindings
- [x] run `git diff --check` and verify OpenAPI/generated client/DB migration drift is absent
- [x] confirm `testcontainers.properties`, credentials, host paths, reports and OS/IDE metadata are unstaged
- [x] record actual test counts, image identifiers, platform verified and remaining 9.29/9.30 work
- [x] move the completed plan to `docs/plans/completed/`

## Verification Results

- `npm ci` completed from the committed lockfile; `npm run check` passed API drift, formatting, ESLint, Stylelint,
  TypeScript, production build and 207 unit/component tests in 41 files.
- `npm run test:coverage` passed the same 207 tests with 90.25% statements, 83.94% branches, 88.79% functions and
  90.84% lines.
- `./gradlew --no-daemon build -PspotlessRatchetFrom=origin/develop` passed 410 backend tests and 200 local integration
  contracts.
- All 204 external integration contracts passed through `http://127.0.0.1:8080/api`, including 4 focused gateway
  contracts and existing multipart/image scenarios.
- The production Playwright delivery smoke passed against the real Compose gateway and backend; no API mocks were
  active.
- Gateway image `sha256:8d7c953ed1213f195ab13ffc4231da51b68c71bfdc77a2b1c1e95dd08a944bd9` was built and
  inspected on `linux/arm64`; size is 55,651,232 bytes, runtime user is UID 101, health is green, Node and the source
  tree are absent from the runtime layer.
- The running container was verified with a read-only root filesystem, `cap_drop: ALL`,
  `no-new-privileges:true` and a 32 MB `noexec,nosuid` `/tmp` tmpfs.
- Base Compose publishes only gateway `127.0.0.1:8080`. The development overlay additionally publishes backend 8081,
  PostgreSQL 5432 and MinIO 9000/9001, all on `127.0.0.1`.
- `/api/v3/api-docs`, SPA root/direct deep link, immutable hashed assets, no-cache HTML, security headers, `/healthz`,
  prefix isolation and proxy error non-masking were verified. OpenAPI reports the public gateway origin with its port.
- OpenAPI source, generated clients, Flyway, jOOQ and database schema were not changed; `npm run api:check` found no
  generated-client drift.
- The unrelated local `testcontainers.properties` change remained untouched and unstaged. No credentials, host paths,
  reports or IDE/OS metadata were added.
- The backend currently returns its existing 500 response for an unknown API route; the gateway preserves that response
  and proves it is not replaced with SPA HTML. Converting this backend behavior to 404 is outside 9.28.
- Full mock browser, accessibility and visual suites were not rerun because no UI source or visual baseline changed;
  the new real production delivery boundary was covered by its dedicated Playwright smoke.
- Windows x86-64 and macOS Intel clean-machine verification remains manual. Packaging/scripts are 9.29; published
  `linux/amd64` + `linux/arm64` manifests, signing, provenance and release automation are 9.30.

## Non-Goals

- Start/Stop/Update/Backup/Restore scripts and Windows/macOS release archives — 9.29.
- Registry login, signing, provenance, SBOM publication and multi-platform image push — 9.30.
- TLS certificates or LAN/public deployment; the supported entry point remains loopback HTTP.
- Runtime authentication/authorization, JWT or user registration implementation.
- Adding Spring Boot Actuator solely for gateway health.
- Changing OpenAPI paths, REST DTOs, backend business logic, Flyway, jOOQ or database schema.
- Replacing the existing mock-based E2E/accessibility/visual suites with real-backend tests.
- Embedding frontend resources in the Spring Boot JAR.

## Risks and Mitigations

- **Incorrect prefix stripping:** lock both slash-sensitive NGINX configuration and real external REST contracts.
- **SPA masks API failure:** `/api` locations have higher priority and never use `try_files ... /index.html`.
- **Multipart rejected at gateway:** 16 MB gateway limit leaves overhead above the backend 10 MB file limit; test real upload.
- **Stale frontend after update:** no-cache HTML plus immutable content-hashed assets.
- **Gateway reports healthy while backend starts:** liveness and API readiness are separate; CI waits on proxied API docs.
- **Read-only NGINX failure:** use the official unprivileged image conventions and explicit tmpfs paths; start under hardened
  Compose in tests.
- **Developer port conflict:** gateway stays 8080; development backend uses 8081; all override ports bind loopback.
- **External contracts accidentally bypass gateway:** centralize external base path and remove direct-backend default.
- **CSP breaks Mantine/previews:** begin with same-origin policy, test production bundle and allow only the minimum
  required `style-src`/`img-src` sources.
- **Moving tags reduce reproducibility:** exact version plus digest, with Dependabot responsible for reviewed updates.
- **False production-ready impression:** documentation repeats missing auth and local-only trust boundary.
- **User local changes:** never touch or stage `configurator-integration-tests/src/test/resources/testcontainers.properties`.

## Decision Gate Before Implementation

Implementation starts only after confirmation of this plan, including:

1. separate digest-pinned unprivileged NGINX image is the selected gateway;
2. main Compose publishes only `127.0.0.1:8080`, development override publishes backend on 8081 and infrastructure on
   loopback;
3. external contracts and a focused production browser smoke run through the gateway;
4. existing backend Dockerfile remains runtime-only in 9.28; release image publishing stays in 9.30;
5. 16 MB gateway body limit, SPA/cache/security behavior and no-Actuator health model are accepted;
6. OpenAPI, backend business logic and DB remain unchanged unless a real delivery defect requires separate approval.

## Post-Completion

9.29 must consume the stable service/image contract created here when implementing Windows/macOS Start, Stop, Update,
Backup and Restore packages. 9.30 must build and publish both backend and gateway images for `linux/amd64` and
`linux/arm64`, verify manifests, attach release artifacts/checksums and run the final clean-machine release audit.

Manual verification remains required on Docker Desktop for Windows x86-64, macOS Intel and macOS Apple Silicon. A local
single-platform build in 9.28 does not prove all three host environments or a published multi-platform manifest.
