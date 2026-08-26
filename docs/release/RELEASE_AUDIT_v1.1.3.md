# Release audit — v1.1.3

Дата аудита: 2026-08-26. Release scope: CON1-137.

Вердикт: **локальные delivery/release contracts пройдены; публикация заблокирована обязательными PR, полной release
matrix, tag workflow и clean-machine smoke**.

## Причина patch release

Windows PowerShell 5.1 превращал Docker Compose stderr в terminating error при глобальном
`$ErrorActionPreference = 'Stop'`. Docker направляет в stderr не только ошибки, но и обычный progress, поэтому
успешный `docker compose up` мог ложно завершать Start или обязательный backup перед Update.

Обёртка Docker теперь временно допускает stderr, журналирует его и возвращает строгий режим после вызова. Успех
определяется exit code Docker; публичные коды ошибок Start, Backup, Restore и Update не изменены. Windows contract
воспроизводит progress в stderr при exit code `0`.

## Source-of-truth impact

- OpenAPI: endpoint/schema contract не изменён; `info.version` обновлена до `1.1.3`.
- Database/Flyway/jOOQ: без изменений.
- Generated code: не редактировался вручную; API client будет проверен на отсутствие drift.
- Architecture: backend boundaries не затронуты.
- Integration contract: backend local/external contract не изменён.
- Security: без изменений; runtime auth отсутствует, поддерживается только trusted-local loopback deployment.

## Версии release candidate

| Область  | Состояние                                                                               |
|----------|-----------------------------------------------------------------------------------------|
| Backend  | Spring Boot 3.4.11; Gradle default `1.1.3-SNAPSHOT`; tag build `-PreleaseVersion=1.1.3` |
| Frontend | package/lock version `1.1.3`; Node 24 / npm 11 contract                                 |
| REST     | OpenAPI 3.0.3, info version `1.1.3`; endpoint/schema contract без изменений             |
| Database | Flyway V1–V7 без изменений                                                              |
| Delivery | Windows/macOS image-only packages; backup format v1; channel `stable`                   |

## Локальные проверки

| Проверка                                             | Результат |
|------------------------------------------------------|-----------|
| Шесть delivery/release/lifecycle contracts           | PASS      |
| `git diff --check`; Windows CRLF/UTF-8 BOM contract  | PASS      |
| Backend build, external integration и frontend gates | Pending   |

Native Windows PowerShell 5.1 test локально не запускался: `powershell.exe` отсутствует. Clean-machine Windows и
macOS smoke не заменяются локальными контрактами и остаются обязательными перед публикацией draft.

## Release blockers

1. Завершить backend, external integration и frontend release matrix.
2. Влить `bugfix/CON1-137` через PR в `develop`, затем release PR `develop` → `master`.
3. Выполнить tag workflow `v1.1.3` на окончательном commit из `master`.
4. Проверить clean-machine Windows/macOS packages, anonymous pulls, checksums, attestations и вручную опубликовать
   только проверенный draft.
