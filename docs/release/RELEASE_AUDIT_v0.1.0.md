# Release audit — v0.1.0

Дата: 2026-08-24

Ветка подготовки: `feature/CON1-129` от `develop`

Вердикт: **репозиторий готов к PR и trusted preview release после настройки GitHub repository/package settings**.
Публикация ещё не выполнена: tag, GHCR manifests, draft GitHub Release и clean-machine проверки требуют merge в
`master` и действий владельца.

Версия `v1.0.0` не рекомендуется: runtime не реализует authentication/authorization, production deployment и
operations baseline. `v0.1.0` — локальный однопользовательский preview для `127.0.0.1`.

## Сверка согласованного функционала

| Функциональность          |     Контракт | Runtime/UI | Автотесты | Итог                                                  |
| ------------------------- | -----------: | ---------: | --------: | ----------------------------------------------------- |
| Domains                   |           Да |         Да |        Да | CRUD, выбор и demo domain готовы                      |
| Component types           |           Да |         Да |        Да | CRUD готов                                            |
| Attribute definitions     |           Да |         Да |        Да | Create/list/update готовы                             |
| Components                |           Да |         Да |        Да | Create/get/list/update/archive/restore готовы         |
| Component images          |           Да |  Да, MinIO |        Да | Upload/read/order/delete готовы                       |
| Manual compatibility      |           Да |         Да |        Да | Links и graph готовы                                  |
| Attribute rules           |           Да |         Да |        Да | CRUD `attribute ↔ attribute` и explanations готовы    |
| Configurator search       |           Да |         Да |        Да | Direct/transitive/BFS/batch/intersection готовы       |
| Saved configurations      |           Да |         Да |        Да | Create/read/update/copy/delete/pagination готовы      |
| JSON configuration export |           Да |         Да |        Да | Готово                                                |
| Adaptive React UI         |           Да |         Да |        Да | Desktop/mobile, light/dark, cards/table готовы        |
| Local delivery            |           Да |         Да |        Да | Windows/macOS Start/Stop/Update/Backup/Restore готовы |
| Register/login/JWT        | OpenAPI only |        Нет |       Нет | Осознанно перенесено                                  |

## Автоматизированная проверка 2026-08-24

- `./gradlew --no-daemon build --rerun-tasks` — успешно, все 21 задачи выполнены заново;
- backend tests — 410, failures/errors/skipped 0;
- local integration contracts — 200, failures/errors/skipped 0;
- external integration contracts — 204, failures/errors/skipped 0;
- backend JaCoCo line coverage — 3059/3259, **93.86%** при minimum 90%;
- `npm ci` и `npm run check` — успешно;
- frontend unit/component tests — 207 в 41 файле, failures 0;
- frontend line coverage — 2004/2206, **90.84%**; statements 90.25%, branches 83.96%, functions 88.79%;
- Playwright functional E2E — 69/69 в Chromium, Firefox и WebKit;
- Playwright accessibility — 34/34; visual regression — 7/7; production delivery smoke — 1/1;
- package, macOS, archive, release-assets и release-workflow shell contracts — успешно;
- реальный Docker lifecycle — Start/Stop, PostgreSQL + MinIO backup/restore, Update success и strict failure успешно;
- app и gateway реально запущены через production same-origin topology, внешний REST-контракт прошёл через gateway;
- app и gateway собраны локально как OCI indexes `linux/amd64` + `linux/arm64`; у каждого обнаружены два BuildKit
  attestation manifests для max provenance/SPDX SBOM;
- backend container работает с numeric UID `10001`, gateway — с unprivileged UID `101`;
- Actionlint, YAML parsing, full-SHA action pins, job permissions, OCI metadata, checksum и archive inventory contracts —
  успешно.

Во время аудита устранены две ошибки browser gate: production delivery spec исключён из mock E2E suite, а кнопкам
замены компонента присвоены уникальные accessible names. Это убрало ложный запуск Compose-сценария и неоднозначный
Firefox locator.

## Release automation

- trusted annotated `vX.Y.Z` tag должен указывать на commit, достижимый из `master`;
- quality gates выполняются до registry mutation;
- публикуются `ghcr.io/eltgm/configurator-app` и `ghcr.io/eltgm/configurator-web` для `linux/amd64` и `linux/arm64`;
- tags: immutable `X.Y.Z`, traceable `sha-*` и mutable `preview`; `latest` не создаётся;
- BuildKit формирует max provenance и SPDX SBOM, GitHub OIDC attestations подписывают image digests и assets;
- release assets: versioned JAR, OpenAPI, Windows ZIP, macOS TAR.GZ, `IMAGE_DIGESTS`, `SHA256SUMS`;
- anonymous image pull/metadata/attestation verification выполняется с чистым Docker config;
- workflow создаёт или обновляет только draft pre-release; публикация остаётся ручной и явной.

## Непроверенное до merge/tag

- фактический GitHub-hosted tag workflow и GitHub Actions schema/runtime;
- public GHCR visibility и anonymous pull из удалённого registry;
- удалённые GitHub OIDC attestations и `gh attestation verify`;
- состав draft GitHub Release после upload;
- нативный Windows PowerShell 5.1 contract текущего commit;
- полный download/extract/lifecycle на чистых Windows 10/11, macOS Intel и macOS Apple Silicon.

Первый image push может создать GHCR packages как private. В этом случае anonymous gate ожидаемо остановит workflow:
владелец переводит оба packages в public, связывает их с repository и повторно запускает workflow. Broad PAT или
автоматическое изменение visibility намеренно не используются.

## Неблокирующий технический долг

- OpenAPI paths не имеют явных `operationId`; generator создаёт имена и предупреждения;
- `ComponentMapper` сообщает о двух unmapped target warnings для attribute elements;
- используются deprecated Gradle APIs, несовместимые с будущим Gradle 9;
- OpenAPI Generator помечает поддержку OpenAPI 3.1 как beta;
- production frontend bundle содержит chunk около 567 kB и выдаёт предупреждение о code splitting;
- Firefox development E2E логирует React/Mantine ResizeObserver/Tooltip warnings, хотя все сценарии проходят;
- backups local preview не шифруются, retention не автоматизирован;
- нет production observability, TLS, secrets strategy и operational SLO.

## Действия владельца перед публикацией

1. Проверить repository settings/rulesets/security по `RELEASE_CHECKLIST.md`.
2. Создать PR `feature/CON1-129 -> develop`, дождаться зелёного CI и не включать local
   `testcontainers.properties`.
3. Создать и проверить release PR `develop -> master`.
4. После зелёного CI на `master` поставить annotated tag `v0.1.0`.
5. При первом запуске при необходимости сделать оба GHCR packages public и rerun workflow.
6. Проверить manifests, digests, checksums, attestations и draft assets.
7. Выполнить clean-machine smoke на Windows/macOS и только затем опубликовать draft как pre-release.

## Блокеры production-ready v1.0.0

- Spring Security/JWT, реальный current user и authorization matrix;
- negative security integration tests;
- production deployment/configuration, secret management и TLS;
- observability, production backup/restore и migration runbook;
- формализованная compatibility/support policy.
