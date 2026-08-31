# Release audit — v1.2.0

Дата подготовки: 2026-08-31. Release scope: CON1-138, CON1-139, CON1-140, CON1-141 и CON1-142.

Вердикт: **release candidate подготовлен; публикация зависит от полного фактического test matrix, двух PR, tag workflow
и clean-machine smoke.**

## Source-of-truth impact

- OpenAPI: `info.version` обновлена до `1.2.0`; контракт расширен полями заглавного изображения и ошибкой удаления области.
- Database/Flyway/jOOQ: добавлены V8 (уникальные имена атрибутов и controlled data merge) и V9 (RESTRICT для
  configurations и персистентная очередь очистки изображений). Выпущенные migrations не менялись.
- Generated code: не редактировался вручную; backend и frontend generation/drift проверяются lifecycle.
- Architecture: проверяемый runtime остаётся `controller → facade → service → port → infrastructure`.
- Integration contract: расширен общими local/external сценариями для изображений, миграции и удаления области.
- Security: runtime-аутентификация не реализована; поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область | Состояние |
| --- | --- |
| Backend | Spring Boot 3.4.11; Gradle default `1.2.0-SNAPSHOT`; tag build `-PreleaseVersion=1.2.0` |
| Frontend | package/lock `1.2.0`; Node 24 / npm 11 contract |
| REST | OpenAPI 3.0.3, info version `1.2.0` |
| Database | Flyway V1–V9; V8/V9 добавлены в этом релизе |
| Delivery | Windows/macOS image-only packages; backup format v1; channel `stable` |

## Проверки

| Проверка | Результат |
| --- | --- |
| Metadata/release documentation consistency | PASS: CHANGELOG, OpenAPI, Gradle, frontend package/lock, README, delivery guide и release artifacts согласованы |
| Backend build и local integration | PASS: `clean build -PreleaseVersion=1.2.0`, включая OpenAPI/jOOQ generation, tests и coverage gate |
| External integration через gateway | PASS: full Compose + `externalIntegrationTest -PreleaseVersion=1.2.0` |
| Frontend static/unit/coverage/browser gates | PASS: `npm ci`, check, 261 unit tests, 91% lines coverage, 102 E2E, 46 accessibility, 8 pinned visual и delivery smoke |
| Delivery/release/lifecycle contracts | PASS: package, macOS scripts, archive, release assets, release workflow и Docker lifecycle |

Native Windows PowerShell 5.1 и clean-machine Windows/macOS smoke нельзя заменить локальными contract tests.

## Release blockers

1. Завершить и задокументировать полный test matrix из checklist.
2. Влить `feature/CON1-143` через PR в `develop`, затем release PR `develop` → `master`.
3. Создать `v1.2.0` guarded-скриптом на final commit из `master` и дождаться tag workflow.
4. Проверить packages на чистых Windows/macOS машинах, anonymous GHCR pulls, checksums и attestations; только затем
   вручную опубликовать draft.
