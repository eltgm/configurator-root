# Release audit — v1.1.2

Дата аудита: 2026-08-26. Release scope: CON1-136.

Вердикт: **локальная release matrix пройдена; публикация заблокирована обязательными PR, tag workflow и clean-machine
smoke**.

## Причина patch release

Создание конфигурации уже проверяло совместимость всего состава как связный граф разрешённых отношений, но редактор
использовал legacy batch-проверку каждой пары. Поэтому отсутствие отдельного решения (`UNKNOWN`) ошибочно считалось
несовместимостью, хотя тот же состав можно было создать и сохранить.

Редактор переведён на assembly-aware candidates contract. Он принимает связные сборки с `UNKNOWN`-парами, отдельно
обрабатывает блокирующий `DENIED` и несвязный `DISCONNECTED`, а также разрешает добавление компонента, способного
восстановить связность. Основной конфигуратор и редактор используют общую модель статусов и объяснений.

## Source-of-truth impact

- OpenAPI: endpoint/schema contract не расширен; уточнено описание assembly semantics, `info.version` обновлена до
  `1.1.2`, frontend SDK regenerated.
- Database/Flyway/jOOQ: без изменений.
- Generated code: обновлён только frontend SDK из OpenAPI source of truth; backend generated code не редактировался.
- Architecture: backend boundaries не затронуты; frontend compatibility model и UI приведены к единому контракту.
- Integration contract: добавлены проверки конфигурации со связным графом и `UNKNOWN`-парами.
- Security: без изменений; runtime auth отсутствует, поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область  | Состояние                                                                               |
| -------- | --------------------------------------------------------------------------------------- |
| Backend  | Spring Boot 3.4.11; Gradle default `1.1.2-SNAPSHOT`; tag build `-PreleaseVersion=1.1.2` |
| Frontend | package/lock version `1.1.2`; Node 24 / npm 11 contract                                 |
| REST     | OpenAPI 3.0.3, info version `1.1.2`; assembly semantics документирована                |
| Database | Flyway V1–V7 без изменений                                                              |
| Delivery | Windows/macOS image-only packages; backup format v1; channel `stable`                   |

## Локальные проверки

| Проверка                                                                                         | Результат                                      |
| ------------------------------------------------------------------------------------------------ | ---------------------------------------------- |
| `./gradlew --no-daemon clean build -PreleaseVersion=1.1.2 -PspotlessRatchetFrom=origin/develop`  | PASS                                           |
| `npm ci && npm run check`                                                                        | PASS, 43 suites / 215 tests и production build |
| `npm run test:coverage`                                                                          | PASS, statements 90.13%, lines 90.72%          |
| Functional Playwright E2E                                                                        | PASS, 72/72 Chromium/Firefox/WebKit             |
| Accessibility                                                                                    | PASS, 36/36 desktop/mobile checks               |
| Pinned-container visual regression                                                               | PASS, 8/8                                       |
| Шесть delivery/release/lifecycle contracts                                                       | PASS                                           |
| External integration contract                                                                    | PASS                                           |

External integration выполнялся через отдельный loopback gateway и подтвердил единый local/external контракт.
Существующая пользовательская Compose-инсталляция и её volumes в тестовый контур не включались.

Native Windows PowerShell test локально не запускался: `pwsh` отсутствует. Clean-machine Windows и macOS smoke не
заменяются локальными контрактами и остаются обязательными перед публикацией draft.

## Release blockers

1. Влить `bugfix/CON1-136` через PR в `develop`, затем release PR `develop` → `master`.
2. Выполнить tag workflow `v1.1.2` на окончательном commit из `master`.
3. Проверить clean-machine Windows/macOS packages, включая редактирование связной конфигурации с `UNKNOWN`-парами.
4. Проверить anonymous pulls, checksums, attestations и вручную опубликовать только проверенный draft.
