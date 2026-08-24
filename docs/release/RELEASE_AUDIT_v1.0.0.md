# Release audit — v1.0.0

Дата аудита: 2026-08-24. Ветка: `feature/CON1-130` от `develop`.

Вердикт: **репозиторий и локально проверенный release candidate готовы к PR**. Все заявленные функции реализованы,
project-owned automated gates проходят. Публикация `v1.0.0` остаётся заблокирована только внешними owner/native
проверками: GitHub/GHCR settings, tag workflow и clean-machine Windows/macOS smoke.

## Граница продукта

`v1.0.0` — production-ready локальный Windows/macOS продукт для одного доверенного пользователя с Docker Desktop и
единственной точкой входа `127.0.0.1:8080`. LAN/public/server deployment не входит в контракт. Совместимость с данными
и backup до `v1.0.0` не гарантируется; документирован чистый сброс и переустановка.

## Воспроизводимое окружение

| Область | Проверенное состояние |
| --- | --- |
| Git | `feature/CON1-130`; постороннее изменение `testcontainers.properties` сохранено отдельно |
| Java/Gradle | JBR Java 21.0.10, Gradle Wrapper 8.14 |
| Backend | Spring Boot 3.4.11, default version `1.0.0-SNAPSHOT` |
| Frontend | Node 24.10.0, npm 11.6.0, package version `1.0.0` |
| Docker | Engine 29.2.1, Compose 5.1.0 |
| REST | OpenAPI 3.0.3, 44 operations со стабильными `operationId` |
| БД | Flyway V1–V6; schema/Flyway/jOOQ не изменялись |

## Функциональная полнота

| Область | API/backend/storage | UI | Unit/static | External/browser |
| --- | --- | --- | --- | --- |
| Домены и demo domain | Реализовано | Реализовано | PASS | PASS |
| Типы и атрибуты | Реализовано | Реализовано | PASS | PASS |
| Компоненты, archive/restore | Реализовано | Реализовано | PASS | PASS |
| Изображения/MinIO | Реализовано | Реализовано | PASS | PASS |
| Ручная совместимость и граф | Реализовано | Реализовано | PASS | PASS |
| Автоматические правила | Реализовано | Реализовано | PASS | PASS |
| Direct/transitive/batch/intersection | Реализовано | Реализовано | PASS | PASS |
| Конфигурации и JSON export | Реализовано | Реализовано | PASS | PASS |
| Responsive/a11y/localization | N/A | Реализовано | PASS | PASS |
| Start/Stop/Update/Backup/Restore | Delivery | Delivery | Contracts PASS | Docker PASS; native pending |

Необъявленных частично реализованных или недоступных пользовательских функций в release scope не обнаружено.

## Выполненные улучшения

- OpenAPI переведён на совместимый с используемым `nullable` формат 3.0.3; schema переименована в
  `SavedConfiguration`, устранены пустые operationId и generator warnings.
- Backend controllers/tests синхронизированы с generated interfaces; intentional MapStruct mapping и generic jOOQ
  conversions сделаны явными и покрыты тестами.
- Добавлен JUnit Platform launcher, устранена Gradle 9 deprecation; Amplicode inspections изменённых Java-файлов чисты.
- Frontend routes загружаются лениво; entry chunk уменьшен с 567.20 до 232.79 kB, oversized-chunk warning исчез.
- Browser gates работают против production build, отклоняют application/page errors и не допускают Vite proxy
  fallthrough; ожидаемые HTTP-error сценарии остаются явными контрактными тестами.
- Compatible frontend dependencies обновлены; `npm ci` и pinned visual container сообщают 0 vulnerabilities, install
  script MSW разрешён точным lockfile identity.
- Удалён единственный доказанно неиспользуемый `RoutePlaceholder`; tracked build/IDE/OS artifacts и runtime TODO/FIXME
  не обнаружены.
- Поставка переведена с `preview` на `stable`, workflow принимает только финальные `vX.Y.Z`, release больше не
  pre-release; `latest` по-прежнему запрещён.
- README, CHANGELOG, SECURITY, SUPPORT, release notes/runbook/checklist, GitHub templates и agent policy согласованы с
  `v1.0.0`; устаревшие документы `v0.1.0` удалены.

## Фактически выполненные проверки

| Проверка | Результат |
| --- | --- |
| `./gradlew --no-daemon clean build --rerun-tasks --warning-mode all` | PASS, 24 tasks |
| Backend/local integration/ArchitectureTest/Spotless/JaCoCo ≥90% | PASS |
| `npm ci && npm run check && npm run test:coverage` | PASS; 207/207; lines 91.04% |
| Functional Playwright Chromium/Firefox/WebKit | PASS, 69/69 |
| Accessibility desktop/mobile WCAG A/AA | PASS, 34/34 |
| Visual regression in pinned container | PASS, 7/7 |
| External integration through production gateway | PASS |
| Production delivery browser smoke | PASS, 1/1 |
| package/macOS/archive/release-assets/release-workflow contracts | PASS |
| Real Docker lifecycle | PASS: Start, backup/restore, stable Update, strict failed Update |
| Generated API drift, Markdown local links, diff/artifact/TODO/policy scans | PASS |

Нативный Windows PowerShell test и clean-machine Windows/macOS проверки не выполнялись на текущем Mac и не
подменяются cross-platform контрактами.

## Source-of-truth impact

- OpenAPI: изменён; HTTP paths/methods/payload JSON не менялись. Добавлены operationId, schema component получил имя
  `SavedConfiguration`, nullable-контракт теперь корректно отражается в generated TypeScript.
- Database/Flyway/jOOQ: не изменены; новые migrations отсутствуют.
- Generated code: backend генерируется только в `build/generated`; frontend SDK регенерирован штатной командой.
- Architecture: boundary `controller → facade → service → port → infrastructure` сохранён и проверен.
- Данные: reset не требуется для текущей schema, но pre-v1 compatibility не обещается.

## Внешние release blockers

1. Настроить GitHub description/topics/social preview, default branch/rulesets и security settings.
2. Убедиться, что GHCR app/web packages public и доступны через anonymous pull.
3. После merge `develop → master` создать annotated `v1.0.0` и дождаться trusted tag workflow.
4. Проверить checksums, attestations и полный lifecycle на clean Windows 10/11 x86-64, macOS Intel и Apple Silicon.
5. Просмотреть и вручную опубликовать созданный draft release.

Локальное изменение `configurator-integration-tests/src/test/resources/testcontainers.properties` принадлежит
пользователю и не должно попадать в commit/PR.
