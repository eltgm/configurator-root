# Production-ready local release v1.0.0

## Overview

- Подготовить Configurator `v1.0.0` как локальный продукт для Windows 10/11 и macOS Intel/Apple Silicon с Docker Desktop.
- Проверить полноту заявленного функционала по OpenAPI, backend, frontend, delivery, тестам и документации.
- Устранить функциональные пробелы, actionable warnings, техническое дублирование и доказанно неиспользуемые артефакты.
- Authentication/authorization и серверное развёртывание не входят в scope. Целевая среда — доверенная локальная машина.
- Совместимость с предварительными установками и данными не обязательна; несовместимые изменения должны требовать явно документированный clean reinstall.

## Context (from discovery)

- Modules: `configurator`, `configurator-integration-tests`, `configurator-web`, `delivery`.
- Source of truth API: `specs/configurator-api.yaml`; generated backend/frontend sources вручную не редактируются.
- Persistence: PostgreSQL 17, Flyway, jOOQ; storage: MinIO.
- REST inventory: 44 generated operations covering domains, component types, attributes, components/images, compatibility, configurator search and saved configurations.
- Existing audit reports functional and automated coverage for the preview candidate, but documents `v1.0.0` as blocked under the previous release policy.
- Source build files use Spring Boot `3.4.11`; IDE metadata reported `4.1.0`, so build source and reproducibility must remain authoritative.
- Existing user change in `configurator-integration-tests/src/test/resources/testcontainers.properties` must be preserved and excluded from commits.

## Development Approach

- **Testing approach:** regular — audit/change first, then tests for each local change.
- Complete each task fully before moving to the next.
- Make small, focused changes and keep architecture/source-of-truth boundaries intact.
- Every code change must include new or updated tests for success and error/edge paths.
- All relevant tests must pass before starting the next task.
- Update this plan whenever scope changes; mark completed items immediately.
- Pre-release data compatibility is optional, but changes remain reproducible through versioned migrations and documented clean-install behavior.

## Testing Strategy

- Backend unit/repository/architecture tests: `./gradlew :configurator:test`.
- Local integration contracts: `./gradlew :configurator-integration-tests:test`.
- External contracts: Compose plus `./gradlew :configurator-integration-tests:externalIntegrationTest`.
- Frontend static/unit/coverage: `npm ci`, `npm run check`, `npm run test:coverage`.
- Browser gates: functional E2E, accessibility, pinned-container visual regression and production delivery smoke.
- Delivery gates: package, macOS, archive, release-assets, release-workflow and Docker lifecycle contracts; native Windows PowerShell test remains a required release check.
- Backend line coverage must remain at least 90%; frontend configured thresholds must pass.

## Progress Tracking

- `[x]` completed; `[ ]` pending; `[+]` newly discovered; `[!]` blocked or requiring owner/external action.
- Keep this document synchronized with actual checks and their results.
- Do not record a command as passed unless it was actually run in the current release work.

## Solution Overview

Use a stage-gated release-hardening flow: establish a reproducible baseline, build an evidence-based feature matrix, close release blockers, refactor backend/frontend in small tested slices, clean only proven-unused content, harden local delivery, synchronize documentation and GitHub automation, then run the complete candidate matrix. External repository settings, registry visibility, trusted tag execution and clean-machine checks are retained as explicit owner actions.

## What Goes Where

- Runtime/API changes: `specs/`, `configurator/`, `configurator-integration-tests/`.
- Frontend changes: `configurator-web/src`, generated client only via `npm run api:generate`.
- Delivery changes: `delivery/`, Compose/Docker files and `scripts/release/`.
- Documentation and policy: root community files, `docs/release/`, `docs/testing/`, `.github/`.
- Evidence and progress: this plan and `docs/release/RELEASE_AUDIT_v1.0.0.md`.

## Implementation Steps

### Task 1: Capture reproducible baseline and inventory

**Files:**
- Create: `docs/release/RELEASE_AUDIT_v1.0.0.md`
- Modify: `docs/plans/20260824-production-ready-local-release-v1.md`
- Inspect: build files, OpenAPI, migrations, frontend package scripts, delivery and GitHub workflows

- [x] record branch, toolchain versions, working-tree exceptions and authoritative Spring Boot version
- [x] inventory tracked source, generated boundaries, REST operations, frontend routes/features and delivery contracts
- [x] scan TODO/FIXME/suppressions, deprecated APIs, generated drift, secrets, host paths and unexpected artifacts
- [x] run baseline backend, frontend and delivery static/unit checks and record all failures/warnings
- [x] create a severity-ranked findings register in the v1.0.0 audit
- [x] rerun targeted diagnostics needed to reproduce every baseline failure before task 2

### Task 2: Prove functional completeness

**Files:**
- Modify: `docs/release/RELEASE_AUDIT_v1.0.0.md`
- Inspect: `specs/configurator-api.yaml`, backend controllers/facades/services/ports, `configurator-web/src/features`, test suites and requirements

- [x] build a contract → backend → persistence/storage → UI → tests → user-flow matrix for every declared feature
- [x] distinguish implemented, partial, unexposed, obsolete and documentation-only behavior
- [x] verify success, validation, not-found/conflict and destructive-operation scenarios
- [x] verify local delivery workflows cover first run, repeat run, update, backup, restore and failure recovery
- [x] rank missing behavior as release blocker, required polish or deferred non-scope work
- [x] run targeted tests/smoke scenarios that substantiate every matrix verdict before task 3

### Task 3: Close confirmed functional release gaps

**Files:**
- Modify as discovered: `specs/configurator-api.yaml`, `configurator/src`, `configurator-integration-tests/src`, `configurator-web/src`
- Create as discovered: versioned migrations and tests; never edit `build/generated/**`

- [x] implement only P0/P1 gaps confirmed by the feature matrix — none were found
- [x] update OpenAPI first and regenerate backend/frontend clients for any transport change — no semantic transport change required
- [x] add a new versioned Flyway migration and regenerate jOOQ for any schema change — no schema change required
- [x] add/update backend unit and integration tests for success and error/edge paths — existing coverage proves all declared flows
- [x] add/update frontend unit and browser tests for changed user flows — no missing user flow required implementation
- [x] run all affected module tests and generated-drift checks before task 4

[+] Quality findings promoted into Tasks 4–6: stable OpenAPI operation IDs, mapper/converter compiler warnings,
frontend lazy route splitting, Firefox React/Mantine console errors, WebKit mock/proxy race and explicit npm install-script policy.

### Task 4: Refactor and normalize backend quality

**Files:**
- Modify: `configurator/src`, backend Gradle configuration and related tests
- Modify if justified: `specs/configurator-api.yaml`

- [x] enforce controller → facade → service → outbound port → infrastructure boundaries
- [x] remove duplication, dead branches, unnecessary abstractions and unsafe resource/transaction handling
- [x] resolve MapStruct unmapped warnings and actionable compiler/OpenAPI warnings
- [x] resolve Gradle deprecations without masking diagnostics
- [x] review jOOQ queries for determinism, batching, indexes and avoidable query amplification
- [x] write/update backend tests for every changed path, including failures and boundaries
- [x] run `:configurator:test`, architecture tests, Spotless and coverage before task 5

### Task 5: Refactor and normalize frontend quality

**Files:**
- Modify: `configurator-web/src`, frontend configuration and tests
- Modify generated client only through `npm run api:generate`

- [x] enforce app/shared/features boundaries and generated-API import rules
- [x] verify domain-scoped query keys, mutation behavior and normalized error states
- [x] simplify components/hooks/forms and remove proven-unused exports, styles and assets
- [x] eliminate project-owned ESLint, TypeScript, Stylelint and browser-console warnings
- [x] improve justified route/vendor splitting and retain accessible loading/error/empty states
- [x] write/update unit, component, E2E, accessibility and visual tests for changed behavior
- [x] run `npm run check`, coverage and affected Playwright gates before task 6

### Task 6: Audit dependencies and supply chain

**Files:**
- Modify if justified: Gradle dependency files, `configurator-web/package*.json`, Dockerfiles, Compose and `.github/dependabot.yml`
- Modify tests/contracts for every dependency or image change

- [x] inventory direct dependencies, plugins, base images and GitHub Actions against current supported releases
- [x] assess known vulnerabilities, license compatibility, lockfile integrity and reproducibility
- [x] apply small compatible upgrades with a recorded rationale; isolate any justified major upgrade
- [x] verify non-root images, minimal runtime content, SBOM/provenance and immutable release metadata
- [x] update dependency/build tests and supply-chain contracts for changed behavior
- [x] run backend, frontend, image and workflow checks affected by upgrades before task 7

### Task 7: Remove proven-unused code and artifacts

**Files:**
- Delete/modify only items proven unused by usages, contracts, coverage and builds
- Modify: `.gitignore`, documentation indexes or build inputs where required

- [x] classify candidates as generated, historical evidence, required fixture, runtime input or removable content
- [x] remove dead code, duplicate fixtures, stale scripts/assets and obsolete documents only with evidence
- [x] retain useful completed-plan history under `docs/plans/completed/`; remove only duplication or false/stale guidance
- [x] verify archives and repository contain no build output, IDE metadata, OS metadata, secrets or host-specific settings
- [x] write/update tests or contracts for any removed runtime/build path
- [x] run affected builds, tests and package inventory contracts before task 8

### Task 8: Harden Windows/macOS local delivery

**Files:**
- Modify: `delivery/`, `docker-compose*.yml`, Dockerfiles and `scripts/release/`
- Modify: delivery contract tests

- [x] verify image-only Compose, loopback-only exposure, stable project/volumes and clear readiness/errors
- [x] preserve PowerShell 5.1/CRLF/BOM and Bash 3.2/LF compatibility
- [x] verify Start/Stop/Update/Backup/Restore locks, strict failure states and interruption recovery
- [x] define and test v1.0.0 clean-install/reset/uninstall behavior for incompatible preview data
- [x] verify amd64/arm64 package/image metadata, archives, checksums, digests, SBOM and provenance
- [x] add/update delivery tests for success and failure/edge paths
- [x] run all local delivery contracts and Docker lifecycle before task 9

### Task 9: Synchronize product and release documentation

**Files:**
- Modify: `README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `SECURITY.md`
- Modify: `docs/release/LOCAL_DELIVERY.md`, `docs/release/RELEASE_CHECKLIST.md`
- Create: `docs/release/RELEASE_NOTES_v1.0.0.md`
- Modify: testing/accessibility/frontend documentation as required

- [x] describe v1.0.0 as a trusted-local Windows/macOS Docker Desktop product and remove obsolete preview policy
- [x] make feature lists, prerequisites, ports, lifecycle, limitations and troubleshooting match verified behavior
- [x] document download, install, update, backup/restore, logs, clean reinstall, reset and uninstall
- [x] synchronize commands, versions, filenames, links, screenshots/badges and support expectations
- [x] validate documentation examples and links as tests where automation is practical
- [x] rerun documentation/package/release contracts before task 10

### Task 10: Bring repository and automation to GitHub release standards

**Files:**
- Modify: `.github/workflows`, `.github/ISSUE_TEMPLATE`, `.github/CODEOWNERS`, PR template and Dependabot config
- Modify: root community and metadata documentation as required

- [x] verify minimum token permissions, full-SHA pins, safe triggers, concurrency, timeouts and failure artifacts
- [x] verify CI covers backend, frontend browsers, external contracts and platform-native delivery scripts
- [x] update release validation and artifact naming for annotated `v1.0.0`
- [x] verify issue forms, PR template, CODEOWNERS, license, conduct, security and support information
- [x] produce owner-only checklist for rulesets, repository metadata/topics, security settings and public GHCR visibility
- [x] add/update workflow and release contract tests for every automation change
- [x] run action/workflow parsers and all release contracts before task 11

### Task 11: Verify release acceptance criteria

**Files:**
- Modify: `docs/release/RELEASE_AUDIT_v1.0.0.md`, `docs/release/RELEASE_CHECKLIST.md`

- [x] run `./gradlew --no-daemon clean build --rerun-tasks`
- [x] run the full external integration contract through production gateway topology
- [x] run `npm ci`, API drift, static/unit/coverage, E2E, accessibility, visual and delivery browser gates
- [x] run package, macOS, archive, release-assets, release-workflow and Docker lifecycle contracts
- [x] verify coverage thresholds, warnings register, secrets/artifact scan and reproducible archive inventory
- [!] record native Windows and clean-machine Windows/macOS checks as passed only when actually performed — pending owner/native execution
- [x] verify every requirement from Overview and every feature-matrix row has evidence

### Task 12: Finalize release handoff

**Files:**
- Modify: `docs/release/RELEASE_AUDIT_v1.0.0.md`, `docs/release/RELEASE_CHECKLIST.md`, this plan

- [x] finalize exact changed/OpenAPI/DB/test/unverified/release-blocker summary
- [x] verify the release changeset excludes the user's local `testcontainers.properties` change and unrelated files
- [x] prepare feature → develop and develop → master PR instructions
- [x] prepare annotated tag, draft release, GHCR visibility, digest/checksum/attestation verification instructions
- [x] document clean-machine artifact smoke and publication decision procedure
- [x] move this plan to `docs/plans/completed/` only after all in-repository work and required automated checks pass

## Post-Completion

**Owner/GitHub actions:** configure branch rulesets and repository/security metadata, make first-created GHCR packages public if necessary, execute the trusted tag workflow from a commit reachable from `master`, inspect and publish the draft release.

**Native clean-machine verification:** download release artifacts and run the full lifecycle on Windows 10/11 x86-64, macOS Intel and macOS Apple Silicon. These external checks cannot be marked complete from non-native/local substitutes.

**Publication rule:** do not publish `v1.0.0` until the release audit has no unresolved in-scope blockers and all external checks are either completed or explicitly accepted by the owner.
