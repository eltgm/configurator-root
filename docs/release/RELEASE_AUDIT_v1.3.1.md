# Release audit — v1.3.1

Дата подготовки: 2026-09-09. Release scope: CON1-146, CON1-147 и CON1-148.

Вердикт: **release candidate прошёл локальную автоматизированную release matrix. Публикация по-прежнему зависит от
двух PR, tag workflow и clean-machine smoke.**

## Source-of-truth impact

- OpenAPI: `info.version` — 1.3.1; добавлены необязательные параметры поиска и сортировки конфигураций и
  необязательные характеристики в ответах кандидатов. Generated frontend client получен из спецификации.
- Database/Flyway/jOOQ: изменений нет; выпущенные migrations не менялись.
- Architecture: runtime сохраняет `controller → facade → service → port → infrastructure`.
- Integration contract: единые local/external сценарии покрывают поиск, сортировку, характеристики кандидатов и
  сохранение inventory-инвариантов.
- Security: runtime-аутентификация не реализована; поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область | Состояние |
| --- | --- |
| Backend | Spring Boot 3.4.11; Gradle default `1.3.1-SNAPSHOT`; tag build `-PreleaseVersion=1.3.1` |
| Frontend | package/lock 1.3.1; Node 24 / npm 11 contract |
| REST | OpenAPI 3.0.3, info version 1.3.1 |
| Database | Flyway V1–V10; без новой migration |
| Delivery | Windows/macOS image-only packages; backup format v1; channel `stable` |

## Проверки

| Проверка | Результат |
| --- | --- |
| Metadata/release documentation consistency | PASS: Gradle/OpenAPI/package/lock/README/CHANGELOG/release artifacts согласованы; `git diff --check` чист |
| Backend build и local integration | PASS: `./gradlew --no-daemon build -PreleaseVersion=1.3.1 -PspotlessRatchetFrom=origin/develop` |
| Frontend static/unit/coverage/browser gates | PASS: 285 unit, 90.67% lines, 138 E2E, 52 accessibility, 8 pinned visual tests |
| Frontend dependency audit | PASS: чистый `npm ci`; `npm audit --audit-level=high` — 0 уязвимостей |
| Delivery/release contracts | PASS: package, macOS scripts, archive, release assets и release workflow |
| External integration и production gateway | PASS: full Compose, external contracts и 1 delivery smoke через gateway |
| Packaged Docker lifecycle | PASS: Start, Backup, Restore, successful Update, failed-readiness safe stop и Stop |

Native Windows PowerShell 5.1 и clean-machine Windows/macOS smoke нельзя заменить локальными contract tests.

## Release blockers

1. Отправить `codex/release-1.3.1`, влить PR в `develop`, затем release PR `develop` → `master`.
2. Создать `v1.3.1` guarded-скриптом на final commit из `master` и дождаться tag workflow.
3. Проверить packages на чистых Windows/macOS, native Windows PowerShell 5.1, anonymous GHCR pulls, checksums и
   attestations; затем вручную опубликовать draft.
