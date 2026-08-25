# Release audit — v1.1.1

Дата аудита: 2026-08-25. Release scope: CON1-135.

Вердикт: **локальная автоматизированная release matrix пройдена; публикация заблокирована обязательными PR, tag
workflow и clean-machine smoke**.

## Причина patch release

`Update.command` из macOS-пакета `v1.1.0` передавал каталог `Configurator/backups/*.partial` как host bind mount в
PostgreSQL и MinIO maintenance-контейнеры. Если пакет находился в `Downloads`, не разрешённом Docker Desktop File
Sharing, обязательный pre-Update backup останавливался с `mounts denied` до загрузки новых образов.

Исправление использует отдельный macOS Compose override с внутренним Docker volume. PostgreSQL dump и MinIO objects
передаются между volume и host checksum-protected tar-потоком. Готовый backup по-прежнему сохраняется в
`Configurator/backups`; backup format v1 и строгая остановка после failed Update/Restore сохранены.

Нужен новый `v1.1.1`, потому что dispatcher и Compose override входят в пользовательский архив и не могут быть
доставлены обновлением container images. Тег, exact images и assets `v1.1.0` остаются неизменяемыми.

## Source-of-truth impact

- OpenAPI: runtime contract без изменений; `info.version` обновлена до `1.1.1` для release metadata.
- Database/Flyway/jOOQ: без изменений.
- Generated code: frontend SDK проверяется после обновления OpenAPI metadata; ручных правок generated code нет.
- Architecture: application boundaries не затронуты; изменён только macOS delivery transport maintenance artifacts.
- Security: без изменений; runtime auth отсутствует, поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область  | Состояние                                                                               |
| -------- | --------------------------------------------------------------------------------------- |
| Backend  | Spring Boot 3.4.11; Gradle default `1.1.1-SNAPSHOT`; tag build `-PreleaseVersion=1.1.1` |
| Frontend | package/lock version `1.1.1`; Node 24 / npm 11 contract                                 |
| REST     | OpenAPI 3.0.3, info version `1.1.1`; endpoint/schema contract без изменений            |
| Database | Flyway V1–V7 без изменений                                                              |
| Delivery | macOS Docker-volume maintenance transport; backup format v1; channel `stable`           |

## Локальные проверки

| Проверка                                                                                         | Результат                                      |
| ------------------------------------------------------------------------------------------------ | ---------------------------------------------- |
| `./gradlew --no-daemon clean build -PreleaseVersion=1.1.1 -PspotlessRatchetFrom=origin/develop`  | PASS, 24 tasks                                 |
| `npm ci && npm run check`                                                                        | PASS, 43 suites / 213 tests и production build |
| `npm run test:coverage`                                                                          | PASS, statements 90.11%, lines 90.66%          |
| Functional Playwright E2E                                                                        | PASS, 72/72 Chromium/Firefox/WebKit             |
| Accessibility                                                                                    | PASS, 36/36 desktop/mobile checks               |
| Pinned-container visual regression                                                               | PASS, 8/8                                       |
| Пять non-Docker-lifecycle delivery contracts                                                     | PASS                                           |
| External integration contract                                                                    | PASS                                           |
| Production gateway delivery smoke                                                                | PASS, 1/1                                      |
| Real packaged Docker lifecycle                                                                   | PASS                                           |

Docker lifecycle подтвердил реальный PostgreSQL/MinIO Backup и Restore, успешный Update, а также обязательный backup
и строгую остановку app/gateway после readiness failure. Release-candidate Compose после проверок остановлен без
удаления сохранённых named volumes.

Native Windows PowerShell test локально не запускался: `pwsh` отсутствует. Clean-machine Windows и macOS smoke не
заменяются локальными контрактами и остаются обязательными перед публикацией draft.

## Release blockers

1. Влить `bugfix/CON1-135` через PR в `develop`, затем release PR `develop` → `master`.
2. Выполнить tag workflow `v1.1.1` на окончательном commit из `master`.
3. Проверить clean-machine macOS package из `Downloads` без настройки File Sharing и выполнить Windows smoke.
4. Проверить anonymous pulls, checksums, attestations и вручную опубликовать только проверенный draft.
