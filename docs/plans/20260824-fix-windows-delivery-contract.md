# Fix Windows Delivery Contract Fault Injection

## Overview

The Windows delivery contract expects a failed image pull during Update to return exit code `60`, stop app/gateway,
and retain the mandatory pre-update backup. The GitHub Actions run on Windows Server 2022 reached the successful Update
path instead because the fake Docker command did not inject the requested pull failure.

This change makes the test fault deterministic and improves failure diagnostics without changing the packaged Windows
lifecycle script or its public behavior.

## Context

- Files involved: `delivery/tests/WindowsScripts.Tests.ps1` and this plan.
- The production script already converts a non-zero Docker pull exit into Update exit code `60`.
- The current test controls the fake command through a mutable child-process environment variable and matches the full
  command line with `findstr`.
- The first hardened Windows run proved that the marker and command matching worked, but Windows PowerShell reported
  `0` when `exit /b 1` was followed by another command in the same parenthesized batch block.
- Windows PowerShell 5.1 and CRLF/UTF-8 BOM compatibility must be preserved.
- OpenAPI, Flyway/jOOQ, generated code, backend architecture, and integration contracts are not affected.

## Development Approach

- **Testing approach:** regular (harden the test double, then run contract suites).
- Keep the change limited to delivery test infrastructure unless verification exposes a production defect.
- Use a marker file for deterministic pull-failure injection.
- Report the actual operation exit code and fake Docker invocation log when an assertion fails.
- Preserve unrelated local changes and exclude them from the commit.

## Solution Overview

The fake Docker command will derive a failure-marker path from its existing log path. The PowerShell test creates that
marker immediately before the failed Update scenario and removes it in cleanup. The fake command will parse arguments
without relying on `findstr` over a path containing spaces and non-ASCII characters, and it will return `1` only for
the configured pull operation. Batch handlers use labels so the shared non-zero `exit /b` is not followed by another
command in the same compound block when invoked from PowerShell.

The failed Update scenario will verify exit code, command sequence, retained backup, and absence of the success message.

## Implementation Steps

### Task 1: Make fake Docker pull failure deterministic

**Files:**

- Modify: `delivery/tests/WindowsScripts.Tests.ps1`

- [x] replace mutable `FAKE_FAIL_PULL` switching with a marker file
- [x] make fake Docker operation detection independent of `findstr`
- [x] preserve Windows PowerShell 5.1 and CRLF/UTF-8 BOM contracts
- [x] add actual exit code and fake Docker log to assertion diagnostics

### Task 2: Strengthen the failed Update contract

**Files:**

- Modify: `delivery/tests/WindowsScripts.Tests.ps1`

- [x] assert failed Update returns exactly `60`
- [x] assert pull and strict app/gateway stop commands were invoked
- [x] assert the pre-update backup remains available
- [x] assert Update did not emit its success message

### Task 3: Verify delivery contracts

- [x] run `delivery/tests/package-contract.sh`
- [x] run `delivery/tests/macos-scripts-test.sh`
- [x] run `delivery/tests/archive-contract.sh`
- [x] run `delivery/tests/release-assets-contract.sh`
- [x] run `delivery/tests/release-workflow-contract.sh`
- [x] run `delivery/tests/docker-lifecycle-contract.sh`
- [ ] verify the Windows PowerShell 5.1 contract in GitHub Actions
- [x] inspect the final diff and commit only related files

## Post-Completion

The Windows PowerShell 5.1 contract must be confirmed on a Windows runner after the branch is pushed. Local macOS
verification cannot execute `powershell.exe`.
