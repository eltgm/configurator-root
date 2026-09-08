# Release audit — v1.3.0

Дата подготовки: 2026-09-08. Release scope: CON1-144 и CON1-145.

Вердикт: **release candidate прошёл локальный автоматизированный test matrix; публикация зависит от двух PR, tag
workflow и clean-machine smoke.**

## Source-of-truth impact

- OpenAPI: `info.version` — 1.3.0; добавлены количество, остатки, режим учёта, фильтр и новый request item. Старый
  `componentIds` помечен deprecated и остаётся совместимым.
- Database/Flyway/jOOQ: добавлена V10 с additive columns и indexes; выпущенные migrations не менялись.
- Generated code: frontend client создан из OpenAPI; backend code создаётся Gradle lifecycle, ручных правок generated
  code нет.
- Architecture: runtime сохраняет `controller → facade → service → port → infrastructure`.
- Integration contract: единые local/external сценарии расширены количеством, остатками и обратной совместимостью.
- Security: runtime-аутентификация не реализована; поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область | Состояние |
| --- | --- |
| Backend | Spring Boot 3.4.11; Gradle default `1.3.0-SNAPSHOT`; tag build `-PreleaseVersion=1.3.0` |
| Frontend | package/lock 1.3.0; Node 24 / npm 11 contract |
| REST | OpenAPI 3.0.3, info version 1.3.0 |
| Database | Flyway V1–V10; V10 добавлена в этом релизе |
| Delivery | Windows/macOS image-only packages; backup format v1; channel `stable` |

## Проверки

| Проверка | Результат |
| --- | --- |
| Metadata/release documentation consistency | PASS: Gradle/OpenAPI/package/lock/README/CHANGELOG/release artifacts согласованы |
| Backend build и local integration | PASS: `clean build -PreleaseVersion=1.3.0`, 24 Gradle-задачи |
| Frontend static/unit/coverage/browser gates | PASS: 263 unit, 90.61% lines, 102 E2E, 46 accessibility, 8 pinned visual tests |
| Frontend dependency audit | PASS: чистый `npm ci`; `npm audit --audit-level=high` — 0 уязвимостей |
| Delivery/release contracts | PASS: package, macOS scripts, archive, release assets и release workflow |
| External integration и production gateway | PASS: full Compose, external contracts и 1 delivery smoke |
| Packaged Docker lifecycle | PASS: Start, Backup, Restore, successful Update, failed-readiness safe stop и Stop |

Native Windows PowerShell 5.1 и clean-machine Windows/macOS smoke нельзя заменить локальными contract tests.

## Release blockers

1. Отправить `codex/release-1.3.0`, влить PR в `develop`, затем release PR `develop` → `master`.
2. Создать `v1.3.0` guarded-скриптом на final commit из `master` и дождаться tag workflow.
3. Проверить packages на чистых Windows/macOS, native Windows PowerShell 5.1, anonymous GHCR pulls, checksums и
   attestations; затем вручную
   опубликовать draft.
