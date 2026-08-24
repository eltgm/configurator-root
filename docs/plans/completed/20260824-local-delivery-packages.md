# CON1-128 — Local Delivery Packages, Update, Backup and Restore

## Overview

Пункт 9.29 превращает production-like Docker topology из 9.28 в два пользовательских пакета: Windows x86-64 и
macOS Intel/Apple Silicon. После установки Docker Desktop конечному пользователю не нужны JDK, Gradle, Node.js, npm,
Git или работа с терминалом: Start, Stop, Update, Backup и Restore запускаются двойным кликом либо через подтверждённый
macOS-сценарий «Открыть» из контекстного меню.

Пакеты содержат только image-based Compose, пользовательские wrappers, maintenance scripts, краткую инструкцию и
лицензию. Исходники и build tools в архив не входят. Контейнеры используют стабильное Compose project name, поэтому
именованные PostgreSQL/MinIO volumes не зависят от имени или расположения распакованной папки.

Подтверждённая модель обновления — mutable release channel `preview`: Update обязательно создаёт backup, выполняет
`docker compose pull` для app/gateway и запускает новые образы. При неуспешной readiness-проверке app/gateway
останавливаются; автоматический запуск старых образов запрещён, потому что новый backend мог уже применить
необратимую Flyway migration.

Backup является переносимым логическим снимком: PostgreSQL custom-format dump, текущие объекты MinIO, manifest и
SHA-256 checksums. Restore сначала валидирует весь backup, создаёт страховочный pre-restore backup и только затем
заменяет данные. OpenAPI, backend business logic, frontend UI, Flyway, jOOQ и схема БД в 9.29 не меняются.

## Context (from discovery)

- `docs/requirements/epic-9-frontend.md` фиксирует пользовательскую цель и название 9.29, но отдельные acceptance
  criteria для Start/Stop/Update/Backup/Restore ещё отсутствуют.
- 9.28 реализовал unprivileged gateway, единственную entry point `http://127.0.0.1:8080`, internal-only backend,
  PostgreSQL/MinIO и отдельный development override.
- Текущий root `docker-compose.yml` предназначен для сборки из исходников и содержит `build`; он не должен попадать в
  пользовательский архив. Нужен отдельный delivery Compose, который только загружает опубликованные images.
- 9.30 отвечает за фактическую публикацию `linux/amd64` + `linux/arm64` app/gateway images, release assets, signing,
  provenance и GitHub Release integration. В 9.29 image references параметризуются и проверяются локальными tags/
  test registry; готовый пользовательский пакет станет полностью устанавливаемым после 9.30.
- GitHub release workflow сейчас публикует только JAR, OpenAPI и `SHA256SUMS`; его расширение пользовательскими
  архивами относится к 9.30.
- В репозитории нет существующих Start/Stop/Update/Backup/Restore scripts или package builder.
- Windows target — поддерживаемая 64-bit Windows 10/11 с Linux containers в Docker Desktop; Windows ARM не входит.
- macOS target — поддерживаемая Docker Desktop версия на Intel или Apple Silicon. Один macOS archive достаточен,
  потому что shell scripts architecture-neutral, а Docker выбирает platform image.
- Windows PowerShell runtime должен оставаться совместимым со встроенным Windows PowerShell 5.1. `.cmd` wrappers
  запускают его с process-scoped `ExecutionPolicy Bypass`; системная execution policy не меняется.
- macOS scripts должны работать со штатным `/bin/bash` 3.2, использовать LF и сохранять executable bit в tar archive.
- Пути к распакованному пакету могут содержать пробелы и не-ASCII символы; scripts обязаны использовать только
  абсолютный путь от собственного расположения (`%~dp0`, `$PSScriptRoot`, script directory).
- Единственное unrelated local изменение —
  `configurator-integration-tests/src/test/resources/testcontainers.properties`; его нельзя редактировать, индексировать
  или включать в commit.

Official references:

- Docker Compose pull: https://docs.docker.com/reference/cli/docker/compose/pull/
- Docker Compose up: https://docs.docker.com/reference/cli/docker/compose/up/
- Docker Compose profiles: https://docs.docker.com/compose/how-tos/profiles/
- Docker Desktop for Windows: https://docs.docker.com/desktop/setup/install/windows-install/
- Docker Desktop for macOS: https://docs.docker.com/desktop/setup/install/mac-install/
- PostgreSQL 17 `pg_dump`: https://www.postgresql.org/docs/17/app-pgdump.html
- PostgreSQL 17 `pg_restore`: https://www.postgresql.org/docs/17/app-pgrestore.html
- MinIO `mc mirror`: https://docs.min.io/aistor/reference/cli/mc-mirror/

## Confirmed Product Contract

1. Публикуются два архива: `configurator-windows-vX.Y.Z.zip` и `configurator-macos-vX.Y.Z.tar.gz`.
2. В пользовательском архиве нет исходников, JAR, Java, Node.js, npm, Gradle или локальной сборки image.
3. Единственная внешняя entry point остаётся `http://127.0.0.1:8080`; PostgreSQL, MinIO и backend не получают host
   ports.
4. Compose project name фиксирован и не зависит от имени папки, поэтому Update/новая распаковка используют те же named
   volumes.
5. App и gateway references задаются через package environment и по умолчанию используют публичные GHCR images канала
   `preview`; конкретные repository names остаются overridable для CI и 9.30.
6. Start проверяет OS/architecture, наличие Docker CLI и Compose plugin, пытается запустить Docker Desktop, ждёт daemon,
   выполняет `docker compose up -d`, ждёт gateway liveness и proxied API readiness, затем открывает браузер.
7. Повторный Start идемпотентен. Он не выполняет update и не требует internet после успешной загрузки images.
8. Stop останавливает только project containers, не Docker Desktop; volumes, backups и images сохраняются. Повторный
   Stop завершается успешно.
9. Update использует канал `preview`, всегда вызывает полный Backup до pull и обновляет только app/gateway images.
10. Update запускает новый stack и проверяет `/healthz` и `/api/v3/api-docs`; успех сообщается только после readiness.
11. При failure Update app/gateway останавливаются, старые images автоматически не запускаются, backup не удаляется;
    выводится путь к backup и инструкция Restore.
12. Backup временно останавливает gateway/app, оставляет PostgreSQL и MinIO доступными для maintenance tools, создаёт
    PostgreSQL custom dump и копию всех текущих объектов application bucket, затем возвращает исходное running state.
13. Backup сначала пишется в `.partial` directory и становится видимым пользователю только после успешного dump,
    mirror, manifest и checksum generation. Неуспешный partial backup нельзя выбрать для Restore.
14. Backup manifest format version равен `1` и содержит UTC timestamp, package version/channel, resolved app/gateway
    image identifiers, Compose project name и перечень artifacts; credentials в manifest/logs отсутствуют.
15. Backup хранится в `backups/YYYYMMDD-HHMMSS/` внутри пользовательской папки и содержит `database.dump`, `minio/`,
    `manifest.properties` и `SHA256SUMS`. Автоматическое удаление старых backups не выполняется.
16. MinIO backup сохраняет текущие objects. Bucket versioning в приложении не используется; исторические object versions
    в format 1 не поддерживаются.
17. Restore по умолчанию показывает только завершённые backups, отсортированные от нового к старому, и предлагает
    выбрать номер. Для automation доступен явный `--backup <path>`.
18. До любых изменений Restore проверяет canonical path, manifest format, обязательные artifacts и каждый SHA-256.
19. Restore принимает только backup, созданный доверенным Configurator package. Это явно показывается пользователю:
    PostgreSQL archive нельзя считать безопасным импортным форматом из неизвестного источника.
20. После validation Restore запрашивает явное подтверждение, создаёт отдельный pre-restore safety backup, останавливает
    gateway/app, пересоздаёт application database и восстанавливает MinIO через `--overwrite --remove`.
21. При успешном Restore запускается текущий `preview` stack и выполняется readiness; Flyway может мигрировать старый
    backup вперёд. Downgrade из backup новой версии в старые images не поддерживается.
22. При failure Restore app/gateway остаются остановленными; safety backup и diagnostics сохраняются. Автоматический
    повторный destructive restore не выполняется.
23. Mutating operations используют process lock; параллельные Update/Backup/Restore/Start/Stop не изменяют один project.
24. Каждая операция пишет отдельный timestamped UTF-8 log без credentials/request payloads и завершает работу
    стабильным ненулевым exit code при prerequisites, validation, backup, restore, update или readiness error.
25. Windows предоставляет `.cmd` wrappers для Start/Stop/Update/Backup/Restore; PowerShell source хранится в UTF-8 BOM,
    wrappers используют CRLF и не требуют постоянного изменения execution policy.
26. macOS предоставляет executable `.command` wrappers; при Gatekeeper warning инструкция использует подтверждённый
    right-click/Open сценарий и не предлагает отключать Gatekeeper глобально.
27. Пользовательские сообщения первого релиза — русские; command names остаются короткими английскими для стабильных
    filenames.
28. Start/Stop/Update/Backup/Restore работают из пути с пробелами; tests также проверяют representative Unicode path.
29. Backups не шифруются и содержат пользовательские данные. Инструкция требует хранить их в доверенном месте; package
    best-effort ограничивает local permissions, но не обещает защиту от администратора/доступа к Docker daemon.
30. Пакет проверяет минимально необходимый Docker Desktop/Compose contract и выдаёт понятное сообщение со ссылкой на
    официальную установку, но не устанавливает Docker Desktop и не принимает license за пользователя.
31. Runtime authentication/authorization не появляется. Пакет остаётся local-only preview и запрещает wildcard/LAN
    exposure.
32. OpenAPI, backend/frontend behavior, Flyway, jOOQ и DB schema в 9.29 не меняются.

## Considered Approaches

### A. Versioned archives with mutable `preview` image channel — selected

- Package scripts и Compose versioned, app/gateway обновляются командой `docker compose pull` по `preview` tag.
- Плюсы: минимальное действие пользователя, Update работает без собственного GitHub API client, одинаково на Windows и
  macOS, соответствует принятому internet-required preview.
- Минусы: scripts/Compose не self-update; breaking delivery change требует скачать новый archive. Mutable tag слабее
  immutable digest для воспроизводимости, поэтому backup/readiness и 9.30 supply-chain проверки обязательны.

### B. Download a new archive for every update

- Пользователь вручную скачивает новую версию; стабильное project name сохраняет volumes.
- Плюсы: scripts, Compose и images обновляются одним versioned contract.
- Минусы: больше ручных действий и выше риск запуска не из той папки. Не выбран как основной UX, но остаётся recovery
  path при breaking package format change.

### C. GitHub Releases API self-updater

- Update находит новый prerelease, скачивает archive, проверяет checksum и заменяет package files.
- Плюсы: обновляет весь package автоматически.
- Минусы: сложное и хрупкое JSON/archive/self-replacement поведение в PowerShell 5.1 и Bash 3.2; prerelease discovery,
  proxy и interrupted replacement требуют отдельного updater продукта. Отложено.

### Backup alternatives

- Выбран logical backup: portable PostgreSQL `-Fc` archive плюс MinIO objects.
- Raw tar named volumes отклонён: PostgreSQL volume нельзя безопасно копировать online, format тесно связан с engine и
  архитектурой, а consistency между двумя volumes сложнее доказать.
- Database-only backup отклонён: metadata изображений без MinIO objects создаст повреждённые пользовательские данные.

## Development Approach

- **Testing approach:** Regular — реализовать небольшой lifecycle block, затем сразу добавить и запустить его
  success/error/edge contracts.
- Полностью завершать каждый task и его tests до перехода к следующему.
- Scripts остаются совместимыми с Windows PowerShell 5.1 и macOS Bash 3.2; нельзя использовать syntax только новых
  PowerShell/Bash releases.
- Не копировать бизнес-логику backup/restore в wrappers: на каждой OS один dispatcher/helper, пять коротких launchers.
- Любое изменение user-visible filenames, backup format, image channel, destructive confirmation или failure rollback
  сначала согласовать с разработчиком.
- Не ослаблять 9.28 gateway/internal-port hardening и существующие backend/frontend quality gates.
- Для каждого нового/изменённого script path обязательны success, failure и edge tests.
- После каждого изменения сверять staged files и не включать unrelated `testcontainers.properties`.
- Если scope/architecture меняется, немедленно обновлять этот plan и требования 9.29.

## Solution Overview

```text
GitHub Release (publication in 9.30)
  |
  +-- configurator-windows-vX.Y.Z.zip
  |     |-- compose.yaml + configurator.env
  |     |-- Start/Stop/Update/Backup/Restore.cmd
  |     `-- scripts/configurator.ps1
  |
  `-- configurator-macos-vX.Y.Z.tar.gz
        |-- compose.yaml + configurator.env
        |-- Start/Stop/Update/Backup/Restore.command
        `-- scripts/configurator.sh

Start -> Docker Desktop -> image-only Compose -> http://127.0.0.1:8080
Update -> Backup -> pull app/gateway:preview -> up -> readiness
Backup -> quiesce writes -> pg_dump -Fc + mc mirror -> manifest + checksums
Restore -> validate -> safety backup -> pg_restore + mc mirror --overwrite --remove -> readiness
```

## Technical Details

### Repository template layout

```text
delivery/
  common/
    compose.yaml
    configurator.env
    README.txt
  windows/
    Start.cmd
    Stop.cmd
    Update.cmd
    Backup.cmd
    Restore.cmd
    scripts/configurator.ps1
  macos/
    Start.command
    Stop.command
    Update.command
    Backup.command
    Restore.command
    scripts/configurator.sh
  tests/
    package-contract.sh
    docker-lifecycle-contract.sh
    macos-scripts-test.sh
    WindowsScripts.Tests.ps1
scripts/release/
  build-delivery-packages.sh
```

Generated archive root is a single `Configurator/` directory. It additionally contains `LICENSE.txt`, `VERSION`,
empty writable `backups/` and `logs/` directories. Build output goes to ignored `delivery-output/`.

### Delivery Compose

- Top-level stable project name: `configurator`.
- Core services: `postgres`, `minio`, `app`, `gateway`; no `build` keys.
- App/gateway images are environment-driven, default to public GHCR `preview` references finalized in 9.30.
- PostgreSQL, MinIO and maintenance client images use reviewed exact versions/digests in the package contract.
- Only gateway binds `127.0.0.1:8080:8080`; hardening from 9.28 is preserved.
- Named volumes remain `postgres_data` and `minio_data` under the stable project.
- One-off `postgres-maintenance` and `minio-maintenance` services use profile `maintenance`, never start on ordinary
  `docker compose up`, and receive a validated bind-mounted backup directory.
- Package environment contains only local-preview settings; it is not described as production configuration.

### Script command model

- Five launcher files pass `start|stop|update|backup|restore` to one OS-specific dispatcher.
- Direct dispatcher options for automated/support use: `--non-interactive`, `--no-open`, `--yes`,
  `--backup <absolute-or-package-relative-path>`.
- Every invocation resolves package root from script location and invokes Compose with explicit `--project-directory`
  and `--env-file`; current working directory is irrelevant.
- A lock directory is acquired atomically and removed in guaranteed cleanup/finally/trap logic.
- Logs use a controlled operation/timestamp filename; stdout shows concise Russian progress and recovery action.
- Wrapper keeps the window readable on Windows/macOS completion/failure without trapping CI non-interactive mode.

### Start and Stop flow

Start:

1. Validate supported OS/architecture and required package files.
2. Locate Docker CLI and Compose v2 plugin.
3. If daemon is unavailable, attempt to open Docker Desktop from official per-user/all-user Windows locations or
   `/Applications/Docker.app`, then wait with a bounded timeout.
4. Validate port 8080 conflict and package Compose config.
5. Run `docker compose up -d --remove-orphans` without a build.
6. Wait first for `/healthz`, then `/api/v3/api-docs`; on timeout print sanitized service status/log tail.
7. Open `http://127.0.0.1:8080` unless `--no-open`.

Stop uses `docker compose stop` for core services, preserves named volumes/images and succeeds if services are already
stopped or not yet created. It never quits Docker Desktop.

### Backup format and flow

Backup directory:

```text
backups/20260824-123456/
  database.dump
  minio/
  manifest.properties
  SHA256SUMS
```

Flow:

1. Acquire operation lock and determine which core services were running.
2. Start/wait PostgreSQL and MinIO if required, then stop gateway/app to prevent cross-store writes.
3. Create a sibling `.partial` directory with restrictive best-effort host permissions.
4. Run pinned PostgreSQL client container with `pg_dump -Fc --no-owner --no-privileges --file` into mounted backup.
5. Run pinned MinIO client with `mc mirror` from application bucket to `minio/`; an absent empty bucket is represented
   deterministically, not silently treated as a transport error.
6. Create manifest and sorted SHA-256 list, verify dump readability and every artifact.
7. Atomically rename `.partial` to final timestamp directory.
8. Restore the pre-operation service state even when backup fails; report backup only after finalization.

No secrets, environment dump, container logs or unrelated MinIO buckets enter the backup.

### Restore flow

1. Discover finalized backups and select interactively, or accept explicit `--backup`.
2. Resolve canonical path, reject partial/symlink escape where the platform can detect it, validate format and
   checksums before prompting.
3. Require explicit destructive confirmation unless `--yes` was supplied by automation.
4. Create a full `pre-restore-<timestamp>` safety backup using the same internal function without taking a nested lock.
5. Stop gateway/app; keep PostgreSQL/MinIO maintenance-ready.
6. Recreate the database from `template0`, restore custom dump with `--exit-on-error --no-owner --no-privileges`.
7. Ensure application bucket exists; mirror backup objects using `--overwrite --remove` so deleted post-backup objects
   do not survive.
8. Start current stack and pass both readiness checks.
9. On failure, stop gateway/app and retain both selected and safety backups plus diagnostics.

Only backups created by this application are supported. The UI has no import semantics in 9.29.

### Update flow

1. Acquire lock and complete full backup; abort before pull if backup fails.
2. Record current resolved app/gateway image IDs in backup manifest/log.
3. Run `docker compose pull app gateway` without `--ignore-pull-failures`.
4. Recreate via `docker compose up -d --remove-orphans` and wait liveness/readiness.
5. On success retain the backup and old local image layers; no automatic image prune.
6. On failure stop app/gateway, retain infrastructure and backup, and show Restore instruction. Do not retag or start old
   images automatically.

The `preview` tag updates runtime images only. Scripts/Compose self-update is a non-goal; breaking package changes use a
new manually downloaded archive.

### Exit codes and diagnostics

Stable categories (exact numeric values finalized in Task 2 and shared by both OS implementations):

- success/idempotent success;
- unsupported OS/architecture or missing package file;
- Docker CLI/Compose/Desktop/daemon unavailable;
- port conflict or invalid Compose;
- backup validation/creation failure;
- restore validation/execution failure;
- update pull/readiness failure;
- concurrent operation lock.

No error path prints DB/MinIO credentials. Diagnostics include operation, safe command description, service status,
gateway/app log tail and recovery path.

### Compatibility and line endings

- `.gitattributes` pins `.cmd` to CRLF, `.command`/`.sh` to LF and preserves binary dump/archive behavior.
- PowerShell source containing Russian strings uses UTF-8 BOM for Windows PowerShell 5.1.
- macOS tar preserves executable modes; package tests inspect modes after extraction.
- Windows zip wrappers are tested with paths containing spaces and Cyrillic characters.
- Scripts do not require `jq`, Homebrew, Chocolatey, Python, Node.js or GNU-only host utilities.

## What Goes Where

- `docs/requirements/epic-9-frontend.md` — detailed 9.29 requirements and 9.30 boundary.
- `delivery/common/compose.yaml` — image-only end-user topology and maintenance profiles.
- `delivery/common/configurator.env` — overridable `preview` image/channel contract and local-only settings.
- `delivery/windows` — `.cmd` launchers and PowerShell 5.1 dispatcher.
- `delivery/macos` — `.command` launchers and Bash 3.2 dispatcher.
- `delivery/tests` — archive, fake-Docker OS script and real Docker lifecycle contracts.
- `scripts/release/build-delivery-packages.sh` — deterministic Windows/macOS archive assembly and local checksums.
- `.gitattributes`, `.gitignore` — line endings, executable/archive safety and generated output exclusion.
- `.github/workflows/ci.yml` — cross-platform script/package gates and Linux Docker lifecycle test.
- `README.md` — minimal end-user download/start flow and source developer flow separation.
- `docs/release/LOCAL_DELIVERY.md` — maintenance, backup trust, recovery and troubleshooting runbook.
- `CONTRIBUTING.md`, `AGENTS.md`, `.github/PULL_REQUEST_TEMPLATE.md` — delivery Definition of Done.
- `.github/workflows/release.yml` — not changed for publishing in 9.29; 9.30 consumes the verified builder/artifacts.

## Testing Strategy

### Static/package contracts

- validate Bash 3.2-compatible syntax and Windows PowerShell 5.1 parser compatibility;
- assert CRLF/LF, PowerShell BOM and macOS executable modes;
- assemble both archives from a clean checkout and inspect exact allowlisted contents;
- reject source, credentials beyond declared local defaults, `.DS_Store`, IDE metadata, reports and host paths;
- validate archive checksums and reproducibility expectations;
- run `docker compose config` with default and overridden image references;
- assert no `build`, no wildcard binds, only gateway loopback port and maintenance profile isolation.

### Fake-Docker OS script contracts

- prepend a controlled fake `docker` executable to PATH and assert command order/arguments without changing production
  scripts;
- success/error/idempotency for Start and Stop;
- Docker missing, Compose missing, daemon timeout, invalid package, port conflict and readiness timeout;
- lock contention and guaranteed cleanup;
- Backup partial cleanup, source running-state restoration and manifest/checksum validation;
- Restore selection, confirmation, invalid path/manifest/checksum and strict failure state;
- Update aborts on backup/pull/readiness failure and never automatically starts old images;
- paths with spaces/Unicode and non-interactive flags.

### Real Docker lifecycle contract

- build current boot JAR and gateway/backend local images;
- override package image references without modifying package templates;
- run extracted package Compose, wait through gateway and execute existing external/delivery contracts;
- seed deterministic PostgreSQL data and an image object;
- Backup, destructively change both stores, Restore and prove exact data/object recovery;
- use a disposable local OCI registry or equivalent controlled image source to verify real `compose pull` update success;
- publish an intentionally non-ready test image to verify strict update failure, stopped app/gateway and preserved backup;
- ensure lifecycle cleanup removes test containers/volumes only, never developer/user volumes.

### OS matrix and manual boundary

- Linux CI: full Compose/backup/restore/update lifecycle plus archive contract.
- Windows CI: Windows PowerShell 5.1 parser, fake-Docker lifecycle, archive extraction/path/encoding checks.
- macOS CI: Bash 3.2/fake-Docker lifecycle, archive extraction/mode/path checks.
- Manual Docker Desktop: Windows x86-64, macOS Intel and macOS Apple Silicon double-click/Start/Stop/Update/Backup/
  Restore and browser opening. Hosted CI does not prove Docker Desktop GUI behavior.

### Existing project gates

- `./gradlew build` and full external contracts remain mandatory because delivery operates on real application data.
- `npm ci`, `npm run check`, `npm run test:coverage` and `npm run test:delivery` remain mandatory if shared delivery,
  gateway or frontend files change.
- OpenAPI/client drift and migration drift must remain empty.
- `git diff --check`, archive content audit and staged-file audit are mandatory.

## Progress Tracking

- Mark completed items with `[x]` immediately after implementation and verification.
- Add newly discovered work with `[+]`; mark blockers with `[!]`.
- Update this plan whenever scope, archive format, exit codes or recovery behavior changes.
- Do not move to the next task while focused tests for the current task fail.

## Implementation Steps

### Task 1: Finalize the 9.29 user and maintenance contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260824-local-delivery-packages.md`

- [x] add detailed Start/Stop/Update/Backup/Restore acceptance criteria from Confirmed Product Contract
- [x] define archive names/content, supported OS/architectures and Docker Desktop prerequisites
- [x] define backup format v1, trust boundary, checksums, consistency and downgrade limitations
- [x] define `preview` update channel, mandatory pre-update backup and strict no-auto-rollback failure behavior
- [x] separate 9.29 scripts/package verification from 9.30 image publication/release automation
- [x] document OpenAPI/backend/frontend/DB/security non-goals
- [x] format and review requirements before Task 2

### Task 2: Create the image-only delivery Compose contract

**Files:**

- Create: `delivery/common/compose.yaml`
- Create: `delivery/common/configurator.env`
- Create: `delivery/common/README.txt`
- Create: `delivery/tests/package-contract.sh`
- Modify: `.gitignore`

- [x] add stable project name, named PostgreSQL/MinIO volumes and core image-only services
- [x] parameterize public `preview` app/gateway image references for 9.30 and local CI overrides
- [x] preserve gateway non-root/read-only hardening and publish only `127.0.0.1:8080`
- [x] add pinned one-off PostgreSQL/MinIO maintenance services behind a profile
- [x] define validated backup bind path without exposing infrastructure ports
- [x] define shared exit-code, readiness, timeout, backup-format and local-only configuration constants
- [x] write package/Compose success tests for default and overridden images
- [x] write error/edge tests for missing variables, invalid paths, profiles, builds and forbidden port binds
- [x] run focused Compose/package tests before Task 3

### Task 3: Implement cross-platform Start and Stop

**Files:**

- Create: `delivery/windows/Start.cmd`
- Create: `delivery/windows/Stop.cmd`
- Create: `delivery/windows/scripts/configurator.ps1`
- Create: `delivery/macos/Start.command`
- Create: `delivery/macos/Stop.command`
- Create: `delivery/macos/scripts/configurator.sh`
- Create: `delivery/tests/WindowsScripts.Tests.ps1`
- Create: `delivery/tests/macos-scripts-test.sh`
- Modify: `.gitattributes`

- [x] implement thin Windows/macOS launchers and one dispatcher per OS
- [x] resolve package root independently of current directory and safely quote space/Unicode paths
- [x] implement prerequisite/architecture/Compose/port checks and bounded Docker Desktop startup wait
- [x] implement atomic operation lock, sanitized logging and stable exit categories
- [x] implement idempotent Start, liveness/readiness wait and browser open
- [x] implement idempotent Stop without deleting volumes/images or stopping Docker Desktop
- [x] ensure Windows PowerShell 5.1/UTF-8 BOM/CRLF and macOS Bash 3.2/LF/executable compatibility
- [x] write Start/Stop success and idempotency tests for both OS implementations
- [x] write missing Docker/daemon/port/readiness/lock/path error and edge tests
- [x] run Windows/macOS focused script tests before Task 4

### Task 4: Implement portable Backup

**Files:**

- Modify: `delivery/windows/scripts/configurator.ps1`
- Modify: `delivery/macos/scripts/configurator.sh`
- Create: `delivery/windows/Backup.cmd`
- Create: `delivery/macos/Backup.command`
- Modify: `delivery/tests/WindowsScripts.Tests.ps1`
- Modify: `delivery/tests/macos-scripts-test.sh`
- Modify: `delivery/tests/docker-lifecycle-contract.sh`

- [x] implement finalized timestamp/partial directory lifecycle and best-effort private permissions
- [x] capture and restore pre-operation running state while quiescing gateway/app writes
- [x] invoke maintenance services for PostgreSQL custom dump and all current MinIO objects
- [x] generate manifest format 1 without secrets and sorted SHA-256 checksums
- [x] validate dump/artifacts before atomic finalization and never expose failed partial backup as restorable
- [x] implement non-interactive internal backup function reusable by Update/Restore without nested lock
- [x] write success/empty-store/running-state/manifest/checksum tests for both OS implementations
- [x] write dump/mirror/disk/path/cleanup/lock failure and edge tests
- [x] run focused fake-Docker and real Docker backup tests before Task 5

### Task 5: Implement strict Restore with safety backup

**Files:**

- Modify: `delivery/windows/scripts/configurator.ps1`
- Modify: `delivery/macos/scripts/configurator.sh`
- Create: `delivery/windows/Restore.cmd`
- Create: `delivery/macos/Restore.command`
- Modify: `delivery/tests/WindowsScripts.Tests.ps1`
- Modify: `delivery/tests/macos-scripts-test.sh`
- Modify: `delivery/tests/docker-lifecycle-contract.sh`

- [x] implement newest-first finalized backup discovery, numbered selection and explicit `--backup`
- [x] validate canonical path, trusted manifest format, required files and all checksums before data changes
- [x] require destructive confirmation and create complete pre-restore safety backup
- [x] recreate database from clean template and restore with fail-fast/no-owner/no-privileges options
- [x] create/replace MinIO bucket content with overwrite/remove semantics
- [x] start current stack and verify liveness/readiness only after both stores restore successfully
- [x] keep app/gateway stopped and preserve diagnostics/safety backup on any restore/readiness failure
- [x] write selection/confirmation/validation/success tests for both OS implementations
- [x] write untrusted/partial/checksum/database/MinIO/readiness/downgrade-warning error and edge tests
- [x] run focused fake-Docker and real destructive restore tests before Task 6

### Task 6: Implement `preview` channel Update

**Files:**

- Modify: `delivery/windows/scripts/configurator.ps1`
- Modify: `delivery/macos/scripts/configurator.sh`
- Create: `delivery/windows/Update.cmd`
- Create: `delivery/macos/Update.command`
- Modify: `delivery/tests/WindowsScripts.Tests.ps1`
- Modify: `delivery/tests/macos-scripts-test.sh`
- Modify: `delivery/tests/docker-lifecycle-contract.sh`

- [x] require successful full backup before image pull and record current resolved image identifiers
- [x] pull only app/gateway channel images and recreate the stack without build or volume replacement
- [x] verify gateway liveness and API readiness before reporting update success
- [x] retain backups and old local image layers without automatic prune
- [x] on pull/start/readiness failure stop app/gateway, preserve infrastructure and print Restore recovery path
- [x] prove no old-image retag/restart or automatic destructive rollback occurs
- [x] write success/unchanged-image/idempotency tests for both OS implementations
- [x] write backup/pull/start/readiness/lock failure and strict-state edge tests
- [x] run focused fake-registry and real Docker update tests before Task 7

### Task 7: Assemble deterministic Windows and macOS archives

**Files:**

- Create: `scripts/release/build-delivery-packages.sh`
- Modify: `delivery/common/README.txt`
- Modify: `.gitignore`
- Modify: `.gitattributes`
- Modify: `delivery/tests/package-contract.sh`

- [x] validate semantic package version and assemble allowlisted `Configurator/` roots from clean templates
- [x] include Compose/environment, matching OS scripts, LICENSE, VERSION and writable backup/log directories only
- [x] produce `configurator-windows-vX.Y.Z.zip` and `configurator-macos-vX.Y.Z.tar.gz`
- [x] preserve Windows encoding/line endings and macOS executable modes
- [x] generate local `SHA256SUMS` for both archives without implementing GitHub publication
- [x] make assembly deterministic where host archive formats permit and record unavoidable metadata boundaries
- [x] write archive content/name/checksum/mode/encoding/path success tests
- [x] write invalid version, missing file, forbidden content and stale output error tests
- [x] extract and validate both packages before Task 8

### Task 8: Add CI package and lifecycle gates

**Files:**

- Modify: `.github/workflows/ci.yml`
- Modify: `.github/PULL_REQUEST_TEMPLATE.md`
- Modify: `delivery/tests/package-contract.sh`
- Modify: `delivery/tests/docker-lifecycle-contract.sh`
- Modify: `delivery/tests/WindowsScripts.Tests.ps1`
- Modify: `delivery/tests/macos-scripts-test.sh`

- [x] add SHA-pinned minimal-permission Ubuntu package/real-Docker lifecycle job
- [x] add Windows runner check using Windows PowerShell 5.1, extracted zip and fake Docker
- [x] add macOS runner check using `/bin/bash`, extracted tar and fake Docker
- [x] build current local app/gateway images and override package image references without template edits
- [x] verify real Start/Stop, seed, Backup, mutation, Restore and exact data/object recovery
- [x] verify controlled `preview` pull success and non-ready update strict failure via disposable test registry
- [x] upload reviewable generated archives/logs only with short retention and no user data/secrets
- [x] ensure jobs clean only uniquely named test project containers/volumes/registry
- [x] run all three CI-equivalent commands locally where the host supports them before Task 9

### Task 9: Document end-user and maintainer workflows

**Files:**

- Modify: `README.md`
- Modify: `CONTRIBUTING.md`
- Modify: `AGENTS.md`
- Create: `docs/release/LOCAL_DELIVERY.md`
- Modify: `docs/release/RELEASE_CHECKLIST.md`
- Modify: `delivery/common/README.txt`

- [x] replace source-build quick start for end users with download/extract/Start minimal flow while preserving developer
      instructions separately
- [x] document Windows/macOS first run, Docker Desktop license/startup and macOS right-click/Open
- [x] document exact Start/Stop/Update/Backup/Restore behavior and recovery messages
- [x] document unencrypted backup storage, trusted-archive rule, no downgrade and strict update/restore failure states
- [x] document local-only/no-auth boundary and prohibit LAN/public exposure
- [x] document package builder, image override, OS matrix and delivery Definition of Done
- [x] update release checklist with 9.29/9.30 boundary and clean-machine verification matrix
- [x] run documentation formatting and link/path checks before Task 10

### Task 10: Verify all 9.29 acceptance criteria

**Files:**

- Modify: `docs/plans/20260824-local-delivery-packages.md`
- Move after completion: `docs/plans/20260824-local-delivery-packages.md` to `docs/plans/completed/`

- [!] macOS Bash suite выполнен локально; Windows PowerShell 5.1 suite добавлен в Windows CI, но Windows runner из
      текущего macOS host не запускался
- [x] build/extract/audit both versioned archives and verify checksums/modes/encodings
- [x] validate delivery Compose default/override/profile/security/port contracts
- [x] run full real Docker Start/Stop/Backup/Restore/Update success and failure lifecycle
- [x] prove PostgreSQL rows and MinIO image bytes recover exactly after destructive mutation
- [x] run `./gradlew build` and full external integration contracts through gateway
- [x] run `npm ci`, `npm run check`, `npm run test:coverage` and production delivery build
- [x] run `git diff --check` and verify OpenAPI/generated client/Flyway/jOOQ drift is absent
- [x] confirm archives contain no source, build tools, secrets, reports, host paths, IDE/OS metadata or unrelated files
- [x] confirm `testcontainers.properties` remains unstaged and unchanged by implementation
- [x] record test counts, archive checksums/sizes, image identifiers, current host/platform and unverified manual matrix
- [x] move completed plan to `docs/plans/completed/`

## Verification Results

- Host: macOS Darwin 25.3.0, Apple Silicon `arm64`; Docker 29.2.1.
- `./gradlew --no-daemon build --rerun-tasks`: success; backend 410 tests and local integration 200 tests, no failures;
  JaCoCo gate passed.
- `:configurator-integration-tests:externalIntegrationTest --rerun-tasks`: success; 204 external tests, no failures,
  through isolated gateway/PostgreSQL/MinIO Compose.
- `npm ci`, `npm run check`, `npm run test:coverage`: success; 41 files / 207 tests in each Vitest run; statements
  90.25%, branches 83.94%, functions 88.79%, lines 90.84%.
- `package-contract.sh`, `macos-scripts-test.sh`, `archive-contract.sh`: success.
- `docker-lifecycle-contract.sh`: success with isolated project and disposable registry; PostgreSQL row and exact MinIO
  bytes recovered, preview Update succeeded, intentionally non-ready Update returned 60 and stopped app/gateway.
- Windows dispatcher/test files parsed successfully in official PowerShell runtime. Native Windows PowerShell 5.1 and
  double-click UX remain pending Windows CI/manual verification.
- Final local images tested: app `sha256:0f7ea745da3e6c01d94e4c85abba85da685d3e9dd3cb136b02e705d41f4dea0b`,
  gateway `sha256:ded6aea51e28750e607b8bd0b5e70ecf55dcf9838b339f9996bd145e4c8e66d1`, both `linux/arm64`.
- Local `v0.1.0` archives are reproducible on the current host: Windows zip 11,681 bytes,
  SHA-256 `71d5a287d4881ce5f4ec71edae5ddc8aa66ef505d6e85f7e3daf3ed1428a59c6`; macOS tar.gz 8,723 bytes,
  SHA-256 `ff3402755986c3f60ec1eaf2c9259df4871ca6ac35c10a7d9ae4e377c2cc8a10`. CI rebuilds and audits archives independently.
- OpenAPI, generated client, Flyway, jOOQ and DB schema have no source changes.
- Unrelated local `configurator-integration-tests/src/test/resources/testcontainers.properties` remains modified only
  by the user and must stay unstaged.
- Manual release matrix still required: Windows x86-64, macOS Intel and clean macOS Apple Silicon Docker Desktop,
  including browser opening and OS security prompts. Public images/release assets remain 9.30.

## Non-Goals

- Publishing app/gateway multi-platform images or GHCR package visibility — 9.30.
- Adding archives to GitHub Release, signing, SBOM, provenance or release attestations — 9.30.
- Full GitHub API self-update of scripts/Compose/archive.
- Automatic rollback to old app images after a failed migration/update.
- Encrypted backups, cloud backup, scheduling, retention policy or incremental backup.
- Restore/import from arbitrary PostgreSQL dumps, S3 buckets or third-party tools.
- Backing up historical MinIO object versions; application bucket versioning is not enabled.
- Installing/updating Docker Desktop or accepting its license for the user.
- Windows ARM, Linux desktop package or native application installer.
- TLS, LAN/public deployment, multi-user mode or production secret management.
- Runtime authentication/authorization, Spring Security or JWT.
- Changing REST API, application/domain behavior, frontend UI, Flyway, jOOQ or DB schema.

## Risks and Mitigations

- **Mutable `preview` tag:** mandatory pre-update backup, resolved image IDs, readiness and no image prune; versioned
  immutable publication remains 9.30.
- **Migration makes image rollback unsafe:** never auto-start old images after failed update; use validated backup and
  explicit Restore.
- **Cross-store inconsistency:** stop gateway/app before PostgreSQL dump and MinIO mirror; restore both stores before
  starting application traffic.
- **Partial backup appears valid:** write to `.partial`, validate, then atomic rename; discovery ignores partial paths.
- **Untrusted database archive:** accept only Configurator-created manifest/checksum format and display trust warning.
- **Restore deletes current data:** validate first, explicit confirmation, mandatory pre-restore safety backup and fail-fast
  tools.
- **MinIO stale objects after restore:** use both `--overwrite` and `--remove`; verify representative byte checksum.
- **Path quoting/encoding:** root-from-script resolution, CRLF/LF/BOM policy and OS tests under space/Unicode paths.
- **Docker Desktop first-run GUI/license:** bounded launch/wait with clear instruction; never bypass acceptance.
- **Package folder rename creates new volumes:** stable Compose project name.
- **Maintenance tools start with application:** isolate under profile and test ordinary `up` service set.
- **Credentials leak through logs/manifest:** centralized redaction and allowlisted manifest fields; inspect artifacts in CI.
- **Backup is unencrypted:** explicit documentation and best-effort permissions; no false security claim.
- **macOS Gatekeeper:** documented right-click/Open only; do not remove quarantine or disable system protection.
- **Hosted CI differs from Docker Desktop:** manual Windows x86-64/macOS Intel/Apple Silicon matrix remains release gate.
- **9.29 depends on unpublished images:** local overrides/test registry verify behavior; 9.30 must publish matching public
  GHCR `preview` images before user release.
- **User local changes:** never touch or stage `configurator-integration-tests/src/test/resources/testcontainers.properties`.

## Decision Gate Before Implementation

Implementation starts only after confirmation of this plan, including:

1. versioned Windows zip and macOS tar.gz with image-only delivery Compose;
2. mutable public `preview` image channel, mandatory pre-update backup and no self-update of scripts;
3. strict failed-update state: app/gateway stopped and no automatic old-image rollback;
4. logical PostgreSQL+MinIO backup format 1 with manifest/checksums and no encryption;
5. trusted-backup-only Restore with destructive confirmation and mandatory safety backup;
6. `.cmd` + Windows PowerShell 5.1 and `.command` + macOS Bash 3.2 UX;
7. maintenance services behind Compose profile and stable project name/volumes;
8. cross-platform fake-Docker tests plus real Linux Docker lifecycle and manual Docker Desktop matrix;
9. 9.29 does not publish images/releases and does not change OpenAPI, application logic or database schema.

## Post-Completion

9.30 must publish public app/gateway images for both `linux/amd64` and `linux/arm64` under the references consumed by
`configurator.env`, apply `preview` and immutable semantic-version tags, verify manifests, build the two archives with
this task's builder, attach them and `SHA256SUMS` to the draft release, and add signing/SBOM/provenance as accepted.

Before publishing the first user release, the owner must manually execute the extracted packages on Windows x86-64,
macOS Intel and macOS Apple Silicon with Docker Desktop: first Start, repeated Start, Stop, restart, Update success,
Backup, destructive data change, Restore, browser opening and documented failure recovery. Docker Desktop license terms
and supported OS versions must be checked at release time because they are external and can change.
