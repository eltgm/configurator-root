# CON1-129 — CI, Supply Chain and Local Preview Release

## Overview

Пункт 9.30 завершает epic 9 и превращает локально проверенные результаты 9.28–9.29 в воспроизводимый пользовательский
preview release. Tag workflow должен собрать и проверить release candidate, опубликовать публичные multi-platform
образы backend и gateway, сформировать Windows/macOS архивы, добавить checksums и supply-chain attestations, проверить
анонимное получение артефактов и создать повторно запускаемый draft GitHub pre-release.

Конечный пользователь по-прежнему устанавливает только Docker Desktop, скачивает один архив и запускает `Start` двойным
кликом. JDK, Gradle, Node.js, npm, Git, registry login и терминал не требуются. Runtime остаётся локальным
однопользовательским preview без authentication/authorization и не описывается как production-ready.

OpenAPI, generated clients, backend/frontend behavior, Flyway, jOOQ и схема БД в 9.30 не меняются.

## Context (from discovery)

- `docs/requirements/epic-9-frontend.md` содержит только название 9.30; подробный acceptance contract ещё не
  зафиксирован.
- `.github/workflows/ci.yml` уже выполняет frontend static/unit/browser gates, backend build, local/external contracts,
  Windows/macOS script contracts, сборку reviewable archives и реальный packaged Docker lifecycle.
- `.github/workflows/release.yml` валидирует tag и принадлежность commit ветке `master`, собирает versioned JAR,
  выполняет external contracts и создаёт draft release только с JAR, OpenAPI и `SHA256SUMS`.
- `delivery/common/configurator.env` уже использует ожидаемые public channel references
  `ghcr.io/eltgm/configurator-app:preview` и `ghcr.io/eltgm/configurator-web:preview`.
- `scripts/release/build-delivery-packages.sh` создаёт versioned Windows/macOS archives и checksum file, но не принимает
  release image namespace/channel как явный input и не формирует image digest manifest.
- Root `Dockerfile` остаётся runtime-only и требует заранее собранный JAR; его base image пока не pinned по digest и
  OCI metadata минимальна. Gateway Dockerfile pinned и уже содержит source labels, но не release version/revision.
- App и gateway Dockerfiles не содержат architecture-specific artifacts; release workflow должен доказать единые
  `linux/amd64`/`linux/arm64` manifests.
- `CHANGELOG.md` и `docs/release/RELEASE_AUDIT_v0.1.0.md` описывают backend-only состояние до epic 9 и требуют
  актуализации перед первым фактическим `v0.1.0` preview release.
- GHCR допускает anonymous pull только для public container packages. Workflow обязан проверить pull без registry
  credentials; initial package visibility остаётся явным GitHub repository prerequisite.
- GitHub рекомендует OIDC-based artifact attestations для provenance и SBOM verification. Для public repository они
  доступны без долгоживущего signing secret.
- Amplicode IDE model сообщил Spring Boot 4.1.0, но project source of truth (`configurator/build.gradle`, `AGENTS.md`)
  фиксирует Spring Boot 3.4.11; план опирается на project source of truth.
- Единственное unrelated local изменение —
  `configurator-integration-tests/src/test/resources/testcontainers.properties`; его нельзя редактировать, индексировать
  или включать в commit.

Official references:

- GitHub Container Registry and anonymous public pull:
  https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry
- GitHub Docker image publishing:
  https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images
- GitHub artifact attestations:
  https://docs.github.com/en/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations
- Docker multi-platform GitHub Actions:
  https://docs.docker.com/build/ci/github-actions/
- Docker SBOM and provenance attestations:
  https://docs.docker.com/build/ci/github-actions/attestations/

## Proposed Product Contract

1. Release automation запускается только для SemVer tag `vX.Y.Z` или совместимого prerelease tag, указывающего на
   commit, достижимый из `master`; annotated tag остаётся release checklist requirement.
2. До любой публикации workflow выполняет release-version validation, backend build/local contracts, frontend quality
   gates, external contracts и delivery/package contracts.
3. Публикуются два OCI image repository: `ghcr.io/eltgm/configurator-app` и
   `ghcr.io/eltgm/configurator-web`.
4. Для каждого image создаётся единый manifest list с платформами `linux/amd64` и `linux/arm64`.
5. Каждый image получает immutable exact version tag `X.Y.Z`, traceability tag по commit SHA и mutable channel tag
   `preview`; tag `latest` до production-ready release line не публикуется.
6. `preview` обновляется только tag workflow после успешных quality gates. Exact version tag никогда намеренно не
   перемещается на другой digest.
7. OCI labels включают source URL, revision, version, title, description, licenses и created timestamp; release metadata
   не содержит credentials или host-specific paths.
8. Root backend image использует reviewed digest-pinned multi-arch Java 21 runtime base. Gateway сохраняет
   digest-pinned Node/NGINX bases и unprivileged/read-only runtime contract.
9. Multi-platform build создаёт BuildKit provenance `mode=max` и SPDX SBOM attestations для каждого image.
10. GitHub OIDC keyless artifact attestation подписывает published image digest. Долгоживущие cosign private keys и
    repository secrets для signing не создаются.
11. Workflow имеет job-scoped minimum permissions: `contents: read` для verification, `packages: write` только для
    image publication, `id-token: write`/`attestations: write` только для signing, `contents: write` только для draft
    release update.
12. Все external actions и reusable dependencies pinned на full commit SHA с version comment; Dependabot продолжает
    отслеживать GitHub Actions и Docker dependencies.
13. Release assets содержат versioned JAR, `configurator-api.yaml`, Windows ZIP, macOS TAR.GZ, `IMAGE_DIGESTS` и единый
    `SHA256SUMS` для всех скачиваемых файлов, кроме самого checksum file.
14. Windows/macOS archives сохраняют versioned package/scripts contract, но channel images остаются `:preview`, чтобы
    пользовательская команда Update загружала новое совместимое preview без скачивания нового package.
15. `IMAGE_DIGESTS` фиксирует exact app/gateway manifest digests и полные version references, чтобы конкретный release
    можно было воспроизвести и проверить независимо от mutable `preview`.
16. Downloadable release assets получают GitHub keyless provenance attestation; документация содержит команды
    `sha256sum`/`shasum`, PowerShell `Get-FileHash` и `gh attestation verify` для технической проверки.
17. После публикации images workflow использует credential-free Docker config и проверяет anonymous manifest inspect/
    pull, наличие обеих платформ, expected OCI labels, attached SBOM/provenance и соответствие digest manifest file.
18. Draft release создаётся или обновляется идемпотентно: повторный запуск workflow не требует удаления draft вручную и
    заменяет только assets того же tag.
19. Release остаётся draft pre-release. Его публикация — явное действие владельца после проверки assets, package
    visibility и clean-machine smoke.
20. Workflow не пытается менять GHCR visibility через broad PAT. Перед первым tag владелец проверяет, что app/gateway
    packages public; anonymous-pull gate блокирует release при неверной visibility.
21. Фактическое создание tag, публикация GHCR images, draft release и clean-machine Windows/macOS audit не выполняются
    из feature branch и перечисляются как post-completion release operations.
22. Пользовательские пакеты продолжают публиковать только `127.0.0.1:8080`; PostgreSQL, MinIO и backend не получают LAN
    или public bind.
23. Runtime authentication/authorization, TLS, hosted deployment, self-update package scripts и rollback Flyway не
    входят в 9.30.
24. OpenAPI paths/DTO, generated code, backend/frontend business behavior, Flyway, jOOQ и DB schema не меняются.

## Considered Approaches

### A. Existing tag workflow + official Docker actions + GitHub attestations — recommended

- Existing `release.yml` остаётся единственным release entry point, но разделяется на verification, image publication,
  public verification и draft release jobs.
- Multi-platform images собираются `docker/setup-qemu-action`, `docker/setup-buildx-action`,
  `docker/metadata-action` и `docker/build-push-action`; GitHub `actions/attest` подписывает digest/assets через OIDC.
- Плюсы: явная orchestration в репозитории, минимальные permissions по jobs, отсутствие signing secrets, понятные
  outputs/digests и простая интеграция с текущим workflow.
- Минусы: workflow длиннее; при сбое после image push registry может содержать exact version image без готового draft,
  поэтому rerun должен быть идемпотентным.

### B. Docker-maintained `docker/github-builder` reusable workflow

- Multi-platform fan-out, signing, SBOM и manifest assembly делегируются централизованному reusable workflow.
- Плюсы: меньше YAML, native platform fan-out и централизованная supply-chain реализация.
- Минусы: более новый внешний abstraction layer, сложнее проследить точные permissions/outputs, требуется отдельно
  согласовать его lifecycle и full-SHA pin; избыточен для двух небольших images первого release.

### C. Explicit Cosign keyless signing plus custom Buildx scripts

- Workflow устанавливает Cosign, вручную собирает manifests и подписывает image/assets через GitHub OIDC.
- Плюсы: широко узнаваемая Sigstore verification model и полный контроль.
- Минусы: дополнительный toolchain/version surface и дублирование GitHub native attestations; долгоживущие keys всё равно
  запрещены. Может быть добавлен позднее, если нужен consumer contract именно `cosign verify`.

## Development Approach

- **Testing approach:** Regular — небольшой workflow/script contract, затем его focused tests; full verification перед
  завершением.
- Полностью завершать каждый task и его tests до перехода к следующему.
- Не запускать tag/release workflow и не публиковать packages из feature branch.
- Не ослаблять существующие backend/frontend/browser/external/delivery quality gates.
- Любое изменение release tags, public namespace, mutable channel, signing model или automatic release publication
  сначала отражать в этом плане и согласовывать.
- Для shell release helpers обязательны success/error/edge contracts; workflow policy проверяется статическим contract
  test и фактической GitHub Actions schema validation после push.
- Все actions pin на full SHA; job permissions остаются минимальными и проверяемыми.
- После каждого блока сверять git diff и не включать unrelated `testcontainers.properties`, generated outputs,
  credentials, `.DS_Store`, reports или `delivery-output`.

## Testing Strategy

- **Release helper unit/contracts:** version/tag validation, image reference injection, deterministic asset inventory,
  checksum/digest manifests, invalid/missing inputs, duplicate/rerun behavior.
- **Workflow policy contract:** triggers, branch/tag validation, minimum job permissions, full-SHA action pins, required
  platforms/tags, absence of `latest`, required assets, attestations and draft-only publication.
- **Docker:** build both images locally, inspect OCI config/labels/non-root gateway, validate current architecture and
  perform multi-platform Buildx validation where the local builder supports it.
- **Delivery:** existing package/archive/macOS/Windows/lifecycle contracts plus new release image/digest injection cases.
- **Backend:** `./gradlew build` and external integration contracts through gateway.
- **Frontend:** `npm ci`, `npm run check`, `npm run test:coverage`; browser suites remain CI-required and rerun locally if
  Docker/browser environment is available or relevant source changes occur.
- **Hygiene:** YAML/action validation, `git diff --check`, generated/OpenAPI/DB drift audit, secret/host-path scan and
  explicit staged-file review.
- **Remote post-completion:** actual tag workflow, public anonymous pulls on both platforms, GitHub attestation
  verification and clean-machine Windows/macOS smoke cannot be proven before merge/tag.

## Progress Tracking

- Mark completed items with `[x]` immediately after implementation and verification.
- Add newly discovered work with `[+]`; mark blockers with `[!]`.
- Update this plan whenever scope or architecture changes.
- Do not start the next task while focused checks for the current task fail.
- Move the completed plan to `docs/plans/completed/` only after all repository work and available verification pass.

## Solution Overview

```text
annotated vX.Y.Z tag on master
  -> validate tag/version/commit and changelog
  -> backend + frontend + external + delivery verification
  -> build/push app and gateway manifests
       -> linux/amd64 + linux/arm64
       -> X.Y.Z + sha-* + preview
       -> OCI labels + SPDX SBOM + max provenance
       -> GitHub OIDC digest attestations
  -> anonymous pull/manifest/attestation verification
  -> build Windows/macOS packages
  -> IMAGE_DIGESTS + unified SHA256SUMS
  -> attest downloadable assets
  -> create/update draft GitHub pre-release
  -> owner clean-machine audit and explicit publish
```

## Technical Details

### Job and permission boundaries

1. `verify-release-candidate`: `contents: read`; validates tag/master ancestry, builds release JAR, runs project and
   delivery checks, uploads internal handoff artifacts.
2. `publish-app-image` / `publish-gateway-image`: `contents: read`, `packages: write`, `id-token: write`,
   `attestations: write`; builds and pushes multi-platform manifests and returns digests.
3. `verify-public-images`: `contents: read`; uses an empty temporary Docker config and registry HTTP/Buildx inspection,
   without inherited login credentials.
4. `prepare-release-assets`: `contents: read`, `id-token: write`, `attestations: write`; downloads verified JAR/digest
   metadata, builds archives/checksums and attests files.
5. `draft-release`: `contents: write`; downloads only final assets and idempotently creates/updates a draft pre-release.

Job split may be reduced when GitHub Actions output/attestation limitations require it, but elevated permissions must not
leak into build/test steps.

### Image tags and promotion

- Exact tag: `ghcr.io/eltgm/configurator-{app|web}:X.Y.Z`.
- Traceability tag: `sha-<short commit>` or full metadata-action equivalent.
- Update channel: `ghcr.io/eltgm/configurator-{app|web}:preview`.
- No `latest` tag before a separately agreed stable production release policy.
- `preview` and exact tag resolve to the same manifest digest during the release run.
- `IMAGE_DIGESTS` records exact `name@sha256:...` references for both manifests.

### Release assets and attestations

```text
configurator-X.Y.Z.jar
configurator-api.yaml
configurator-windows-vX.Y.Z.zip
configurator-macos-vX.Y.Z.tar.gz
IMAGE_DIGESTS
SHA256SUMS
```

`SHA256SUMS` is generated last over the five non-checksum assets in stable order. Image SPDX SBOM/provenance are
attached to OCI images; downloadable assets receive GitHub provenance attestations. Release documentation distinguishes
checksum integrity from signed provenance and does not claim that either proves absence of vulnerabilities.

### Rerun and partial-failure behavior

- Quality gates run before first registry mutation.
- Exact image tag is expected to be immutable by policy; rerun must verify an existing exact tag matches the expected
  release source or fail rather than silently replace an unrelated digest.
- Mutable `preview` may be promoted to the verified release digest.
- Draft creation uses create-or-update/upload-with-clobber semantics for the same tag.
- A partial registry publication is reported explicitly; cleanup/deletion is not automatic because destructive package
  deletion requires broader permissions and may remove evidence needed for diagnosis.

## What Goes Where

- Requirements and decision record: `docs/requirements`, this plan.
- Release orchestration: `.github/workflows/release.yml`; ordinary PR/push gates remain in `.github/workflows/ci.yml`.
- Deterministic local release assembly: `scripts/release/`.
- Static/fake release contracts: `delivery/tests/`.
- Image metadata/hardening: root and frontend Dockerfiles.
- User/operator/repository documentation: README, CHANGELOG, AGENTS, CONTRIBUTING and `docs/release/`.

## Implementation Steps

### Task 1: Finalize the 9.30 release contract

**Files:**

- Modify: `docs/requirements/epic-9-frontend.md`
- Modify: `docs/plans/20260824-local-release-publishing.md`

- [x] add detailed 9.30 acceptance criteria from Proposed Product Contract
- [x] confirm `v0.1.0`, GHCR namespace, exact/sha/preview tag policy and draft-only release behavior
- [x] confirm GitHub OIDC attestations plus BuildKit SBOM/provenance as the selected signing model
- [x] document non-goals, public visibility prerequisite and partial-failure behavior
- [x] run Markdown formatting/style checks before Task 2

### Task 2: Make release asset assembly deterministic and testable

**Files:**

- Modify: `scripts/release/build-delivery-packages.sh`
- Create: `scripts/release/prepare-release-assets.sh`
- Modify: `delivery/tests/archive-contract.sh`
- Create: `delivery/tests/release-assets-contract.sh`

- [x] parameterize validated public app/gateway references and inject package version/channel without host-specific state
- [x] assemble JAR, OpenAPI, both archives and `IMAGE_DIGESTS` into an explicit clean output directory
- [x] generate stable unified `SHA256SUMS` and reject missing, extra, duplicate or malformed inputs
- [x] add success/reproducibility tests for version/image/digest injection and checksum verification
- [x] add failure/edge tests for invalid SemVer, invalid image digest, missing artifacts and unsafe output paths
- [x] run package/archive/release-asset contracts before Task 3

### Task 3: Add reproducible OCI release metadata

**Files:**

- Modify: `Dockerfile`
- Modify: `configurator-web/Dockerfile`
- Modify: `.github/dependabot.yml` if the final base-image dependency contract requires it
- Create or Modify: `delivery/tests/release-assets-contract.sh`

- [x] pin the Java 21 runtime base by reviewed digest while retaining amd64/arm64 support
- [x] add version/revision/source/license OCI ARG/labels to backend and gateway images
- [x] preserve runtime-only backend build and unprivileged/read-only gateway behavior
- [x] test Dockerfile labels, absence of secrets/host paths and current-platform runtime behavior
- [x] validate both Dockerfiles with Buildx and run focused image/gateway delivery checks before Task 4

### Task 4: Add release workflow policy contracts

**Files:**

- Create: `delivery/tests/release-workflow-contract.sh`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/PULL_REQUEST_TEMPLATE.md`

- [x] assert tag trigger, master ancestry and SemVer/version consistency contract
- [x] assert full-SHA action pins and minimum job-scoped permissions
- [x] assert both image names/platforms, exact/sha/preview tags and explicit absence of `latest`
- [x] assert required SBOM/provenance/attestation, anonymous verification and draft release steps
- [x] add release contracts to ordinary CI and PR checklist
- [x] run workflow/static/package contracts before Task 5

### Task 5: Build and verify the release candidate before publication

**Files:**

- Modify: `.github/workflows/release.yml`

- [x] keep strict tag syntax, annotated-tag expectation, master ancestry and release version outputs
- [x] run backend build/local contracts with `-PreleaseVersion`, frontend quality gates and delivery contracts
- [x] run full external integration and production gateway browser smoke for the release candidate
- [x] hand off the exact versioned JAR and immutable source metadata between jobs without rebuilding different bits
- [x] preserve failure logs/cleanup and prevent package/image write permissions in verification steps
- [x] exercise all locally testable workflow contracts before Task 6

### Task 6: Publish and attest multi-platform images

**Files:**

- Modify: `.github/workflows/release.yml`

- [x] add pinned official QEMU, Buildx, login, metadata and build/push actions
- [x] build/push app and gateway for `linux/amd64,linux/arm64` with exact/sha/preview tags and OCI labels
- [x] enable max provenance and SPDX SBOM OCI attestations without build-arg secrets
- [x] create GitHub OIDC provenance attestations for both manifest digests
- [x] expose immutable digests to downstream release asset jobs
- [x] add credential-free manifest/platform/pull/label/attestation verification and clear visibility failure output
- [x] run workflow policy and available local multi-platform checks before Task 7

### Task 7: Create an attested, rerunnable draft release

**Files:**

- Modify: `.github/workflows/release.yml`
- Modify: `scripts/release/prepare-release-assets.sh`
- Modify: `delivery/tests/release-assets-contract.sh`

- [x] assemble the exact final asset inventory from verified JAR and image digests
- [x] generate and verify `IMAGE_DIGESTS` plus unified `SHA256SUMS`
- [x] create GitHub OIDC provenance attestations for downloadable release assets
- [x] create or update the same draft pre-release idempotently and upload assets with controlled clobber semantics
- [x] ensure no automatic public release, `latest` tag, destructive registry cleanup or broad PAT is introduced
- [x] test first-run/rerun asset behavior and all local failure paths before Task 8

### Task 8: Update user, contributor and release documentation

**Files:**

- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `AGENTS.md`
- Modify: `CONTRIBUTING.md`
- Modify: `docs/release/LOCAL_DELIVERY.md`
- Modify: `docs/release/RELEASE_CHECKLIST.md`
- Modify: `docs/release/RELEASE_AUDIT_v0.1.0.md`
- Create if needed: `docs/release/RELEASE_NOTES_v0.1.0.md`

- [x] replace “9.30 pending” wording with the implemented release and verification contract
- [x] document minimum end-user quick start and local-only/no-auth warning prominently
- [x] document checksums, image digests, SBOM/provenance and `gh attestation verify` without overstating security
- [x] update v0.1.0 changelog/audit/release notes to match the actually verified epic 9 functionality
- [x] separate automated evidence from manual GHCR visibility, repository settings and clean-machine checks
- [x] run Markdown/link/format checks before Task 9

### Task 9: Verify all repository acceptance criteria

**Files:**

- Modify: `docs/plans/20260824-local-release-publishing.md`
- Move after completion: `docs/plans/completed/20260824-local-release-publishing.md`

- [x] run `npm ci`, `npm run check` and `npm run test:coverage`
- [x] run applicable Playwright functional/accessibility/visual/delivery suites
- [x] run `./gradlew build`
- [x] build/start the full gateway topology and run external integration contracts
- [x] run package, Windows, macOS, archive, release-assets, release-workflow and real Docker lifecycle contracts
- [x] locally build/inspect app and gateway images and validate multi-platform configuration where supported
- [x] verify YAML/action pins/permissions, checksums, OCI metadata, no credentials and no public/LAN exposure
- [x] run `git diff --check` and verify OpenAPI/generated client/Flyway/jOOQ/DB drift is absent
- [x] confirm `testcontainers.properties`, `delivery-output`, reports, credentials, host paths and IDE/OS metadata are
      unstaged
- [x] record actual test counts, coverage, image IDs/platforms and all remaining remote/manual checks
- [x] move the completed plan to `docs/plans/completed/`

## Completed Verification — 2026-08-24

- Backend: 410 tests; local integration: 200 tests; external integration: 204 tests; all failures/errors/skips 0.
- Backend JaCoCo: 3059/3259 lines, 93.86% against the 90% gate.
- Frontend: 207 unit/component tests in 41 files; line coverage 2004/2206, 90.84%.
- Playwright: 69 functional E2E, 34 accessibility, 7 visual and 1 production delivery scenarios passed.
- Full `./gradlew --no-daemon build --rerun-tasks`, `npm ci`, `npm run check` and `npm run test:coverage` passed.
- Full same-origin Compose topology, external contracts and packaged Start/Stop/Backup/Restore/Update lifecycle passed.
- App and gateway OCI output each contained `linux/amd64`, `linux/arm64` and two `unknown/unknown` BuildKit
  attestation manifests; backend ran as UID 10001 and gateway as UID 101.
- Package/archive/release helper contracts, Actionlint, YAML parsing, Markdown/Prettier, shell syntax, action-pin,
  permission, checksum, metadata and workspace hygiene checks passed.
- No OpenAPI, generated client, Flyway, jOOQ or DB schema change was introduced.
- The pre-existing local `configurator-integration-tests/src/test/resources/testcontainers.properties`, `.DS_Store`,
  generated reports and `delivery-output` remain unstaged and outside the intended change set.
- GitHub tag execution, GHCR public visibility/anonymous remote pulls, remote OIDC attestation verification, draft asset
  inspection, native Windows PowerShell and clean-machine Windows/macOS acceptance remain Post-Completion owner checks.

## Post-Completion

These actions require merged trusted code and GitHub owner/repository authority; they are not implementation checkboxes:

- Configure/check both GHCR packages as public and linked to `eltgm/configurator-root`.
- Ensure repository Actions, packages and artifact attestation settings permit the minimum workflow permissions.
- Merge `feature/CON1-129 -> develop`, then release PR `develop -> master`; require green CI on `master`.
- Create and push the reviewed annotated `v0.1.0` tag on a commit reachable from `master`.
- Observe the release workflow, verify both public multi-platform manifests and run GitHub attestation verification.
- Inspect the draft release assets, checksums, warnings and generated notes, then publish it explicitly as pre-release.
- Perform clean-machine download/extract/Start/repeated Start/Stop/Update/Backup/Restore on Windows 10/11 x86-64,
  macOS Intel and macOS Apple Silicon.
- Perform final smoke from downloaded release assets with no registry login and retain evidence in the release audit.

## Risks and Mitigations

- **GHCR package defaults to private:** anonymous-pull gate fails clearly; owner changes visibility before publication.
- **Partial publication after registry push:** exact digest evidence is retained, workflow is rerunnable and draft creation
  is idempotent; automatic deletion is avoided.
- **Mutable `preview` moves:** exact version/sha tags and `IMAGE_DIGESTS` preserve auditability; package Update intentionally
  follows preview.
- **Cross-platform image regression:** manifest inspection plus amd64/arm64 pull/run checks in trusted release workflow;
  host clean-machine smoke remains mandatory.
- **Supply-chain metadata leaks build args:** credentials are never passed as build args; max provenance is inspected for
  secret/host-path absence.
- **Attestation is mistaken for vulnerability proof:** docs explicitly describe provenance/integrity scope and keep
  vulnerability management separate.
- **Workflow permission creep:** separate jobs and static permission contract prevent `contents/packages/id-token` writes
  from becoming global.
- **Action tag compromise:** every external action is pinned to full reviewed commit SHA and tracked by Dependabot.
- **Unreleased source differs from released JAR:** one verified versioned JAR is handed to the image and release-assets
  jobs; it is not independently rebuilt downstream.

## Decision Gate Before Implementation

Implementation starts only after confirmation of this plan, including:

1. release remains the first `v0.1.0` local preview and updates the existing backend-only changelog/audit;
2. images are `ghcr.io/eltgm/configurator-app` and `ghcr.io/eltgm/configurator-web`;
3. exact `X.Y.Z`, commit SHA and mutable `preview` tags are published, but `latest` is not;
4. selected signing model is GitHub OIDC artifact attestations plus BuildKit max provenance/SPDX SBOM, without private
   signing keys or separate Cosign workflow;
5. workflow creates/updates only a draft pre-release; owner publication remains manual;
6. GHCR public visibility and clean-machine checks remain explicit owner/post-merge steps;
7. OpenAPI, application behavior, generated code and database remain unchanged unless implementation uncovers a real
   delivery defect requiring separate approval.
