# CON1-100 — Выдача содержимого изображения через backend

## Overview

Добавить `GET /component-images/{id}/content`, чтобы браузер получал JPEG, PNG и WebP через backend и web gateway, а
MinIO оставался приватным. Object storage key становится единственным сохраняемым значением `component_image.file_path`;
REST поле `ComponentImage.url` формируется как `/component-images/{id}/content`.

## Context (from discovery)

- REST source of truth: `specs/configurator-api.yaml`.
- Existing HTTP boundary: `ComponentController` implements generated `ComponentsApi`.
- Existing flow: `ComponentFacade -> ComponentService -> ComponentRepository / ComponentImageStorage`.
- Persistence: jOOQ with Flyway migrations; `component_image.file_path` currently stores a public MinIO URL.
- Storage: `MinioComponentImageStorage` currently supports store/delete, but not read.
- Uploaded images are limited to 10 MiB, so a defensive in-memory byte array is acceptable for this endpoint.
- Existing local user changes in `configurator-integration-tests/src/test/resources/testcontainers.properties` are out of
  scope and must not be staged.

## Development Approach

- **Testing approach:** Regular — update contract and implementation first, then add tests for every changed layer.
- Complete each task fully before moving to the next.
- Make small, focused changes and keep the architecture
  `controller -> facade -> service -> outbound port -> infrastructure`.
- Every task includes success and error tests.
- All relevant tests must pass before moving to the next task.
- Update this plan immediately when scope or implementation decisions change.
- Generated OpenAPI and jOOQ sources are never edited manually.

## Testing Strategy

- Controller unit tests for status, headers and response body.
- Facade/service unit tests for delegation, missing metadata and storage propagation.
- Repository tests for image lookup by identifier and object-key persistence.
- MinIO adapter tests for exact bytes/media type and translated storage failures.
- Shared local/external integration contract for image content retrieval and missing image.
- Full `./gradlew build` and external integration contracts as Definition of Done.

## Progress Tracking

- Mark completed items with `[x]` immediately.
- Add newly discovered tasks with `[+]`.
- Mark blockers with `[!]`.
- Keep this file synchronized with the actual implementation.

## Solution Overview

1. A Flyway data migration converts legacy URL/path values containing `/components/` to `components/...` object keys.
2. Domain `ComponentImage` stores `objectKey`; REST mapping creates the stable backend content URL.
3. Repository lookup resolves image metadata by image identifier.
4. Storage port reads an object and returns immutable bytes plus its media type.
5. Service resolves metadata and delegates binary retrieval to storage.
6. A dedicated generated `ComponentImagesApi` and thin controller return the resource with `Content-Type`,
   `Content-Length`, `Content-Disposition: inline` and `Cache-Control: private, no-cache`.

## Technical Details

- Endpoint: `GET /component-images/{id}/content`.
- Success: `200`, original bytes and original JPEG/PNG/WebP media type.
- Unknown metadata: `404`.
- Invalid identifier: `400` through existing validation/error handling.
- External storage failure or inconsistent missing object: `503`.
- Archived component images remain readable.
- No redirect and no public MinIO URL are exposed.
- Database schema shape is unchanged; migration changes stored `file_path` semantics/data and triggers the normal Gradle
  generation lifecycle.

## Implementation Steps

### Task 1: Update REST contract and object-key persistence semantics

**Files:**
- Modify: `specs/configurator-api.yaml`
- Create: `configurator/src/main/resources/db/migration/V6__CON1-100-store-component-image-object-key.sql`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/domain/model/ComponentImage.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/domain/model/StoredImage.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/mapper/ComponentMapper.java`

- [x] define binary content endpoint and response/error contract in OpenAPI
- [x] add backward-compatible data migration from legacy URLs to object keys
- [x] change domain models and REST mapping to persist object keys and expose backend content URLs
- [x] update mapper/domain tests for the new semantics
- [x] run contract compilation and affected tests before task 2

### Task 2: Add repository metadata lookup

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ComponentRepository.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ComponentRepositoryImpl.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/infrastructure/persistence/jooq/ComponentRepositoryImplTest.java`

- [x] persist object key in `FILE_PATH`
- [x] add lookup of `ComponentImage` by image identifier
- [x] test create/read success and missing image
- [x] verify nested component image mapping still returns deterministic order
- [x] run repository tests before task 3

### Task 3: Add binary read to the storage port

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/port/out/ComponentImageStorage.java`
- Create: `configurator/src/main/java/ru/sultanyarov/configurator/domain/model/ComponentImageContent.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/infrastructure/storage/minio/MinioComponentImageStorage.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/infrastructure/storage/minio/MinioComponentImageStorageTest.java`

- [x] read exact bytes and content type by object key
- [x] close MinIO response resources reliably
- [x] translate MinIO failures to `ExternalStorageException`
- [x] test JPEG/PNG/WebP-compatible content and failure paths
- [x] run storage tests before task 4

### Task 4: Expose the application use case and HTTP controller

**Files:**
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentService.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ComponentServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacade.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ComponentFacadeImpl.java`
- Create: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ComponentImageController.java`
- Modify: corresponding service, facade and controller tests

- [x] resolve image metadata and read content through outbound ports
- [x] keep archived image content readable
- [x] implement the generated API in a thin controller
- [x] return media type, content length, inline disposition and private/no-cache policy
- [x] test success, unknown ID and propagated storage failure
- [x] run affected unit tests before task 5

### Task 5: Add shared integration contract

**Files:**
- Modify/Create: image contract files under `configurator-integration-tests/src/test/groovy`
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/it/ComponentImageStorageTestConfiguration.groovy`
- Modify/Create: deterministic SQL fixtures under `configurator-integration-tests/src/test/resources/sql`

- [x] add deterministic test storage read support
- [x] verify successful content response bytes and content type in local and external transports
- [x] verify unknown image returns 404
- [x] verify storage failure returns 503 in the shared contract
- [x] run local integration tests

### Task 6: Verify acceptance criteria

- [x] run `./gradlew :configurator:test`
- [x] run `./gradlew build`
- [x] build/start external Docker contour
- [x] run `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [x] verify JaCoCo threshold and architecture tests
- [x] inspect diff for generated code, credentials and unrelated local files

### Task 7: Final documentation

- [x] update README/AGENTS for the changed operational image contract
- [x] mark the completed implementation items in this plan
- [x] move this plan to `docs/plans/completed/`

## Post-Completion

- Publish/push only after owner review.
- The user-facing Docker/web gateway remains part of later Epic 9 tasks.
- Runtime authentication and authorization remain a release blocker outside CON1-100.
