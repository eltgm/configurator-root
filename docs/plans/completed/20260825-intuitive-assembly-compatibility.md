# Intuitive Assembly Compatibility

## Status

- Approved by the user on 2026-08-25.
- Completed on 2026-08-25.

## Overview

Replace the current complete-graph requirement with assembly-aware compatibility:

- a candidate can join a non-empty assembly when it has at least one positive relationship to the assembly;
- any applicable blocking rule prevents the candidate from joining, even when another manual relationship is positive;
- a saved multi-component assembly must be connected through positive relationships and contain no blocked pair;
- a single component remains a valid starting assembly;
- the existing mathematical intersection endpoint keeps its current contract.

The change removes artificial domain relationships such as CPU-to-case while preserving meaningful constraints such as CPU socket-to-motherboard socket and CPU TDP-to-PSU power.

## Context From Discovery

### Current save flow

`ConfigurationController -> ConfigurationFacadeImpl -> ConfigurationServiceImpl -> ConfigurationCompatibilityValidator`

`ConfigurationCompatibilityValidator.validatePairwiseDirectCompatibility` currently requires a direct positive edge for every pair. A configuration with `N` components therefore requires `N * (N - 1) / 2` edges.

### Current candidate flow

`ConfiguratorController -> ConfiguratorFacadeImpl -> ConfiguratorServiceImpl -> CompatibilityGraphSearchEngine -> ConfiguratorResultAssembler`

The intersection flow removes a candidate when it is not compatible with every base component. This is valid for an explicitly named intersection operation, but unsuitable as the main assembly-editing suggestion policy.

### Current rule semantics

- A manual link is a positive undirected relationship.
- Conditions inside one rule set use `AND`.
- Multiple enabled rule sets for the same component-type pair are alternatives using `OR`.
- A matching rule set creates a positive relationship.
- A failed rule set currently only produces no edge.
- A manual link currently overrides failed automatic rules (`Manual mismatch override`).
- There is no persisted `ALLOW/DENY` effect.

### Demo-domain evidence

The demo creates ten artificial manual links solely to complete the graph, including CPU-to-case, CPU-to-memory, and CPU-to-PSU. These links are modeling artifacts rather than real compatibility knowledge.

### Local changes to preserve

`configurator-integration-tests/src/test/resources/testcontainers.properties` has an unrelated user modification and must not be edited or included in task commits.

## Recommended Solution

### Three-state pair decision

Introduce a single reusable pair evaluator returning:

- `ALLOWED`: at least one enabled automatic rule set matches, or a manual link exists and no automatic constraint rejects the pair;
- `DENIED`: enabled rule sets apply to the pair's component types, but none matches;
- `UNKNOWN`: neither a manual relationship nor an applicable enabled rule exists.

Precedence:

1. If at least one applicable rule set matches, the pair is `ALLOWED` (existing OR semantics).
2. If applicable enabled rule sets exist and none matches, the pair is `DENIED`.
3. Otherwise a manual relationship makes the pair `ALLOWED`.
4. Otherwise the pair is `UNKNOWN`.

This intentionally removes the current manual-mismatch override: an automatic constraint failure must not be bypassed by an unrelated manual positive link.

### Candidate policy

For a non-empty assembly, a candidate is available when:

```text
exists ALLOWED(candidate, selectedComponent)
and
not exists DENIED(candidate, selectedComponent)
```

Special cases:

- the first component can be added to an empty assembly;
- during replacement, remove the replaced component from the evaluation base before checking the candidate;
- `UNKNOWN` neither supports nor blocks a candidate;
- the same component ID cannot be added twice and existing component-type/cardinality policies remain in force.

### Saved-assembly policy

A configuration is valid when:

- zero/one-component behavior remains consistent with the existing contract;
- the graph formed only by `ALLOWED` edges is connected for two or more components;
- no selected pair is `DENIED`.

Connectivity is required instead of merely checking that every component has one neighbor. The latter would incorrectly accept two disconnected subassemblies such as CPU-to-motherboard and GPU-to-PSU.

### API and UX policy

Keep `POST /domains/{id}/configurator/compatible/intersection` unchanged.

Add an assembly-aware candidates operation in `specs/configurator-api.yaml`, tentatively:

`POST /domains/{id}/configurator/candidates`

The request contains the current component IDs and, when relevant, the component ID being replaced. The response should expose enough structured evidence for UX:

- candidate component;
- status (`AVAILABLE`, `BLOCKED`, `UNRELATED`) or equivalent grouped result;
- positive supporting relationships and explanations;
- blocking component pairs and failed rule-set explanations.

Primary UI suggestions show `AVAILABLE` candidates. `BLOCKED` candidates may be shown in a separate unavailable section with an exact reason. `UNRELATED` candidates are hidden from normal suggestions. The backend remains the source of truth for the decision.

### Explicit negative rules

Persisted `effect = ALLOW | DENY` is deliberately out of scope for this iteration. The current positive constraint rule model can represent the requested examples: for `cpu.tdp <= psu.power`, a failed applicable rule yields `DENIED`.

If future requirements need unconditional bans or a mixture of independently matching allow and deny rules, add `effect` in a separate schema/API change.

## Examples Where This Matters

- PC: CPU connects to motherboard by socket; RAM connects to motherboard by memory standard; neither needs a direct relation to the case. CPU TDP above PSU capacity or cooler capacity blocks the build.
- Server: NIC and RAM independently connect to the motherboard; NIC does not need a CPU relation. PSU limits can block a high-load component.
- Camera kit: lens and flash each connect to the camera; lens and flash need no mutual edge. An incompatible teleconverter blocks the lens combination.
- Smart home: sensors and relays connect to a hub, not to every other device. Region/frequency or protocol-version rules can block a device.
- Audio: source connects to amplifier and amplifier connects to speakers. Source-to-speaker compatibility is irrelevant, while unsafe impedance blocks the assembly.
- Vehicle: engine connects to gearbox and gearbox to drivetrain. Engine-to-wheel relation is unnecessary, while power or torque limits may block brakes or transmission.
- Industrial automation: modules attach through controller/backplane relationships. Voltage or bus-generation constraints block unsafe modules without requiring all-pairs links.

## Development Approach

- Testing approach: TDD for the changed compatibility invariants, followed by implementation.
- Complete each task and its tests before moving to the next task.
- Keep controller -> facade -> service -> outbound port -> infrastructure boundaries unchanged.
- Update the OpenAPI source before generated backend/frontend code.
- Do not edit `build/generated/**` or frontend generated API files manually.
- Update this plan if implementation discovery changes the scope.

## Implementation Steps

The checklist below preserves the originally approved plan. Actual completion and verification are recorded in **Completion Results**.

### Task 1: Confirm frontend flow and freeze acceptance cases

**Files:**

- Inspect: `configurator-web/src/features/**`
- Inspect: `configurator-web/src/shared/api/**`
- Inspect: `specs/configurator-api.yaml`
- Modify: this plan if exact file paths or DTO decisions change

- [ ] identify whether configuration create/edit currently calls direct search, search, or intersection
- [ ] identify the exact UI location for candidate suggestions and replacement behavior
- [ ] document final request/response shape and compatibility evidence fields
- [ ] confirm empty assembly, replacement, disconnected assembly, and duplicate/type-policy behavior
- [ ] confirm that aggregate constraints such as total system power are out of scope for pairwise rules

### Task 2: Add three-state pair evaluation with deny precedence

**Files:**

- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/DirectCompatibilityResolver.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/CompatibilityRuleEvaluatorImpl.java`
- Modify as required: graph context/builder domain models in `configurator/src/main/java/ru/sultanyarov/configurator/application/service/`
- Create or modify: focused decision/result model under the domain or application service boundary
- Modify: corresponding tests under `configurator/src/test/java/ru/sultanyarov/configurator/application/service/`

- [ ] write tests for manual-only `ALLOWED`, matching-rule `ALLOWED`, failed-rule `DENIED`, and no-knowledge `UNKNOWN`
- [ ] write tests proving rule-set OR semantics: one match among alternatives is `ALLOWED`
- [ ] replace the manual-mismatch-override expectation with deny precedence
- [ ] implement one reusable evaluator used by validation and candidate selection
- [ ] preserve deterministic supporting and blocking explanations
- [ ] run the focused unit tests and require them to pass

### Task 3: Validate configurations by connectivity and absence of denies

**Files:**

- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationCompatibilityValidator.java`
- Modify as required: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfigurationServiceImpl.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/application/service/ConfigurationCompatibilityValidatorTest.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/application/service/ConfigurationServiceImplTest.java`

- [ ] write tests accepting a single component and a connected non-complete chain
- [ ] write a test rejecting two disconnected positive subgraphs
- [ ] write tests rejecting any `DENIED` pair even when the candidate has another `ALLOWED` edge
- [ ] write tests for deterministic conflict details
- [ ] replace the complete-graph loop with connected-graph plus deny validation
- [ ] run the focused configuration tests and require them to pass

### Task 4: Add assembly-aware candidate API without changing intersection

**Files:**

- Modify first: `specs/configurator-api.yaml`
- Regenerate through Gradle lifecycle: backend OpenAPI interfaces/DTOs
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/ConfiguratorServiceImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/facade/ConfiguratorFacadeImpl.java`
- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/api/inbounds/rest/controller/ConfiguratorController.java`
- Modify: related backend mapper and unit-test files

- [ ] add request/response schemas and the assembly-aware endpoint to OpenAPI
- [ ] compile to regenerate backend API code; never edit generated code manually
- [ ] write service tests for at-least-one support, deny precedence, unrelated candidates, empty base, and replacement
- [ ] implement candidate classification using the shared pair evaluator
- [ ] return structured supporting and blocking evidence
- [ ] prove existing intersection behavior and tests remain unchanged
- [ ] run focused controller/facade/service tests and require them to pass

### Task 5: Align local and external integration contracts

**Files:**

- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractConfiguratorControllerContract.groovy`
- Modify: `configurator-integration-tests/src/test/groovy/ru/sultanyarov/configurator/contract/AbstractConfigurationControllerContract.groovy`
- Modify as required: deterministic SQL fixtures under `configurator-integration-tests/src/test/resources/sql/`
- Preserve: `configurator-integration-tests/src/test/resources/testcontainers.properties`

- [ ] add a contract accepting a connected but non-complete configuration
- [ ] add a contract rejecting a disconnected configuration
- [ ] add a contract where one allowed relationship is overridden by a blocking rule against another selected component
- [ ] add candidate endpoint contracts for available, blocked, and unrelated results
- [ ] replace the obsolete `Manual mismatch override` assertion with deny precedence
- [ ] run local integration tests and require them to pass before proceeding

### Task 6: Simplify and strengthen the demo domain

**Files:**

- Modify: `configurator/src/main/java/ru/sultanyarov/configurator/application/service/DemoDomainServiceImpl.java`
- Modify: `configurator/src/test/java/ru/sultanyarov/configurator/application/service/DemoDomainServiceImplTest.java`

- [ ] remove all manual links that exist only to complete the graph
- [ ] keep only real domain relationships
- [ ] add a CPU-to-PSU TDP/capacity constraint using existing attributes or add the minimum missing demo attribute
- [ ] write tests proving the demo assembly is connected through meaningful relations
- [ ] write tests proving an over-limit CPU is not suggested and cannot be saved
- [ ] run focused demo tests and require them to pass

### Task 7: Implement intuitive frontend candidate selection

**Files:**

- Regenerate: `configurator-web/src/shared/api/generated/**` via `npm run api:generate`
- Modify: public API boundary under `configurator-web/src/shared/api/`
- Modify: exact configuration feature files identified in Task 1 under `configurator-web/src/features/<feature>/{api,model,ui}`
- Modify: relevant translation resources and tests

- [ ] generate the frontend client from OpenAPI and verify no drift
- [ ] use query keys containing `domainId` and the normalized current assembly IDs
- [ ] show available candidates based on backend decisions
- [ ] show blocked candidates only in an explicit unavailable view with precise blocking reasons
- [ ] handle empty, loading, error, unrelated, replacement, and stale-selection states
- [ ] add unit/component tests for candidate visibility and explanations
- [ ] add/update Playwright flow for replacing CPU while preserving a valid assembly
- [ ] run `npm ci`, `npm run api:check`, and `npm run check`

### Task 8: Full verification and documentation

**Files:**

- Modify documentation only if public compatibility semantics need explanation
- Update this plan with actual results and deviations

- [ ] run `./gradlew :configurator:test`
- [ ] run `./gradlew :configurator-integration-tests:test`
- [ ] run `./gradlew build`
- [ ] run the required frontend checks from Task 7
- [ ] start the production-like Compose contour and run `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [ ] verify architecture tests and JaCoCo minimum line coverage `0.90`
- [ ] confirm generated backend and frontend code has no drift
- [ ] record every check actually run and every remaining unverified item
- [ ] after completion, move this plan to `docs/plans/completed/`

## Acceptance Criteria

- CPU replacement can be offered based on CPU-to-motherboard compatibility without a CPU-to-case link.
- A candidate with at least one supporting relationship and no blocking relation is available.
- A candidate with any blocking relation is unavailable even if another relation supports it.
- A connected chain or tree of components can be saved without all-pairs links.
- Two disconnected compatible subassemblies cannot be saved as one configuration.
- The first component can start an assembly.
- Replacement evaluates the candidate against the assembly without the old component.
- Existing intersection semantics remain unchanged.
- Save validation and UI suggestions use the same compatibility decision semantics.
- Local and external integration tests exercise the same contract.

## Completion Results

- Implemented a reusable `ALLOWED` / `DENIED` / `UNKNOWN` pair decision with rule-set OR semantics and deny precedence over manual links.
- Replaced complete-graph save validation with connectivity of allowed edges plus rejection of every denied pair.
- Added `POST /domains/{id}/configurator/candidates` and regenerated backend and frontend API artifacts.
- Updated the configurator to show only available candidates in the primary list, keep unrelated candidates hidden, and explain blocked candidates in a separate unavailable section.
- Preserved the existing intersection endpoint and transitive exploration mode; transitive-only paths do not make an assembly saveable.
- Removed artificial demo graph-completion links and added CPU TDP-to-PSU capacity compatibility.
- Added unit, local/external integration, frontend component, Playwright e2e, and accessibility coverage for the new behavior.
- Preserved the unrelated local modification in `configurator-integration-tests/src/test/resources/testcontainers.properties`.

Verification completed:

- `./gradlew :configurator:test`
- `./gradlew :configurator-integration-tests:test`
- `./gradlew build`
- `./gradlew :configurator-integration-tests:externalIntegrationTest` against a clean isolated Compose project
- `npm ci`
- `npm run api:check`
- `npm run check`
- `npm run test:coverage` (90.50% statements, 91.06% lines)
- `npm run test:e2e` (69 tests across Chromium, Firefox, and WebKit)
- `npm run test:accessibility` (34 desktop/mobile checks)
- `git diff --check`

Not run:

- visual regression, because the repository contract requires its pinned Docker image and baseline review;
- delivery e2e, because delivery/runtime contracts were not changed.

Environment note: the pre-existing default Compose PostgreSQL volume contains an out-of-branch Flyway `V7` migration and is incompatible with the current fixtures. It was not modified or deleted. External tests passed on an isolated clean Compose project, whose temporary containers and network were removed afterward.

## Scope Impact

- OpenAPI: yes, for the new assembly-aware candidate operation and structured explanations.
- Database/Flyway/jOOQ: no for the recommended solution.
- Generated code: yes, backend and frontend OpenAPI generation only.
- Architecture boundaries: unchanged.
- Integration contract: required for both local and external transports.
- Security: no change; the trusted-local runtime limitation remains.

## Post-Completion

- Manually verify CPU replacement, blocked CPU-by-PSU, and disconnected-subassembly UX in desktop and mobile layouts.
- If explicit unconditional negative rules are later required, plan a separate `effect = ALLOW | DENY` migration and CRUD/UI extension.
- Aggregate constraints such as total CPU+GPU load versus PSU capacity require a separate assembly-level rule model and are not covered by pairwise compatibility.
