# Fix Maintenance Bind-Mount Ownership

## Overview

- Исправить падение GitHub Actions job `External integration contracts` после успешного packaged lifecycle scenario.
- Устранить расхождение UID/GID между host user и `postgres-maintenance`/`minio-maintenance`, из-за которого контейнер
  оставляет во вложенных bind-mounted backup-каталогах недоступные для удаления файлы.
- Сохранить backup format v1, текущие команды Backup/Restore/Update и совместимость Windows PowerShell 5.1 и macOS
  Bash 3.2.

## Context (from discovery)

- GitHub job `97362624915` завершился на шаге `Run packaged lifecycle contract`: EXIT trap в
  `delivery/tests/docker-lifecycle-contract.sh` получил `Permission denied` при удалении трёх MinIO marker files.
- Сценарий до cleanup создаёт обычный backup, safety backup перед Restore и pre-update backup; все три содержат один
  marker, поэтому одинаковая ошибка воспроизводится в трёх каталогах.
- `delivery/common/compose.yaml` запускает maintenance services без согласования пользователя контейнера с host user.
- `delivery/macos/scripts/configurator.sh` разрешает контейнеру запись в корень bind mount, но после операции меняет
  mode только у корня backup и `minio/`; вложенный `minio/delivery-contract` остаётся созданным container user.
- `delivery/tests/docker-lifecycle-contract.sh` удаляет временный каталог обычным host-side `rm -rf`.
- В рабочей копии есть unrelated user change в
  `configurator-integration-tests/src/test/resources/testcontainers.properties`; его нельзя изменять или включать в
  будущий commit.
- OpenAPI, Flyway/jOOQ, backend architecture и общий local/external HTTP contract не затрагиваются.

## Development Approach

- **testing approach:** regular — сначала минимальная правка ownership contract, затем targeted и полный набор delivery
  tests.
- Выполнять задачи последовательно небольшими локальными изменениями.
- Для каждого изменения обновлять соответствующий shell/Compose contract test и запускать его до следующей задачи.
- При изменении scope обновлять этот план.
- Не использовать `sudo`, runner-specific cleanup или автоматическое удаление пользовательских backups.

## Testing Strategy

- Static/package contracts проверят Compose default и override maintenance user.
- macOS script contract проверит формирование UID:GID без нарушения Bash 3.2 semantics.
- Docker lifecycle contract проверит реальный bind mount, Backup/Restore/Update и успешный host-side cleanup.
- Полный delivery suite обязателен по `AGENTS.md`; backend/external checks будут отмечены отдельно, если окружение их не
  позволит выполнить.

## Progress Tracking

- Завершённые пункты сразу отмечать `[x]`.
- Новые задачи добавлять с `[+]`, blockers — с `[!]`.
- План держать синхронизированным с фактической реализацией.

## Solution Overview

- Добавить в maintenance services Compose numeric user, управляемый переменной с совместимым default.
- В macOS/Linux dispatcher вычислять numeric UID:GID текущего host user и передавать его во все Compose invocations.
- В реальном Docker lifecycle contract использовать тот же ownership contract и явно проверять, что созданное backup
  tree доступно host user.
- Windows dispatcher не вычисляет POSIX UID/GID и продолжает использовать Compose default; формат архива и Windows
  scripts не меняются.

Отклонённый узкий вариант: удалять test artifacts через `sudo` или root container в cleanup. Он сделал бы job зелёным,
но сохранил бы источник проблемы и зависимость backup ownership от container image user.

## Technical Details

- Новая Compose interpolation должна быть строкой вида `<uid>:<gid>` и иметь явный default для сред без POSIX IDs.
- Переменная передаётся как process environment, чтобы иметь приоритет над packaged `.env` и не записывать
  host-specific values в архив.
- `postgres-maintenance` и `minio-maintenance` получают одинакового пользователя: оба пишут в `/backup`.
- Проверка должна покрыть каталоги с пробелами и все три вида backup, уже создаваемые lifecycle scenario.

## What Goes Where

- **Implementation Steps:** Compose ownership, dispatcher propagation, contract assertions и обязательные проверки.
- **Post-Completion:** rerun GitHub Actions job/PR CI после commit и push выполняется только по отдельному указанию.

## Implementation Steps

### Task 1: Define maintenance user in packaged Compose

**Files:**

- Modify: `delivery/common/compose.yaml`
- Modify: `delivery/tests/package-contract.sh`
- Modify: `delivery/tests/archive-contract.sh` if required by rendered Compose assertions

- [x] add a quoted maintenance user interpolation to both maintenance services with a Windows-compatible default
- [x] keep application services, volumes, backup paths and maintenance profile unchanged
- [x] add contract assertions for default and explicit UID:GID override
- [x] run `delivery/tests/package-contract.sh`
- [x] run `delivery/tests/archive-contract.sh` before Task 2

### Task 2: Propagate host UID:GID from macOS/Linux dispatcher

**Files:**

- Modify: `delivery/macos/scripts/configurator.sh`
- Modify: `delivery/tests/macos-scripts-test.sh`

- [x] resolve numeric UID and GID with commands available in macOS Bash 3.2 environments
- [x] pass the value to every Compose invocation without persisting host-specific data in `configurator.env` or logs
- [x] preserve Windows dispatcher behavior and CRLF/BOM files unchanged
- [x] add tests for propagation and paths containing spaces
- [x] run `delivery/tests/macos-scripts-test.sh` before Task 3

### Task 3: Enforce host-writable backups in the real lifecycle contract

**Files:**

- Modify: `delivery/tests/docker-lifecycle-contract.sh`

- [x] pass the same numeric host user to direct maintenance Compose calls in the contract
- [x] assert that completed, pre-restore and pre-update MinIO directory trees are writable/removable by the host user
- [x] retain ordinary `rm -rf` cleanup so the original failure remains covered without privileged workarounds
- [x] run `delivery/tests/docker-lifecycle-contract.sh` against built app/gateway images
- [x] confirm the test exits zero and leaves no `delivery-output/docker-contract.*` directory

### Task 4: Verify acceptance criteria

- [x] run `delivery/tests/package-contract.sh`
- [x] run `delivery/tests/macos-scripts-test.sh`
- [x] run `delivery/tests/archive-contract.sh`
- [x] run `delivery/tests/release-assets-contract.sh`
- [x] run `delivery/tests/release-workflow-contract.sh`
- [x] run `delivery/tests/docker-lifecycle-contract.sh`
- [x] run `./gradlew build`
- [x] run `./gradlew :configurator-integration-tests:externalIntegrationTest` when the external Compose contour is available
- [x] confirm `git diff --check` and that the unrelated `testcontainers.properties` change is unstaged/unmodified by this work

### Task 5: Finalize documentation and status

- [x] confirm no delivery documentation change is needed because the maintenance user is an internal implementation detail
- [x] record actual test results and any unverified checks in the handoff
- [x] report that OpenAPI and database schema were not changed
- [x] report remaining release blockers, including the known lack of runtime authentication/authorization
- [x] move this completed plan to `docs/plans/completed/`

## Post-Completion

**Manual verification**

- Rerun the failed GitHub Actions workflow/job on an Ubuntu GitHub-hosted runner and verify `Run packaged lifecycle
  contract` completes without cleanup errors.

**External system updates**

- Commit/push/PR or GitHub Actions rerun require separate user direction; this plan does not authorize them.
