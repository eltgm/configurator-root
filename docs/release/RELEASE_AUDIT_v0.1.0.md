# Release audit — v0.1.0

Дата: 2026-07-31

Ветка подготовки: `feature/CON1-83` от `develop`

Вердикт: **готово к preview release после PR и настройки GitHub repository settings**.

Версия `v1.0.0` пока не рекомендуется: runtime не реализует authentication/authorization, production deployment и
operations baseline.

## Сверка согласованного функционала

| Функциональность           | Контракт |   Runtime | Автотесты | Итог                                   |
|----------------------------|---------:|----------:|----------:|----------------------------------------|
| Domains                    |       Да |        Да |        Да | Готово                                 |
| Component types            |       Да |        Да |        Да | Готово                                 |
| Attribute definitions      |       Да |        Да |        Да | Готово в объёме create/list/update     |
| Components                 |       Да |        Да |        Да | Готово: create/get/list/update/archive |
| Component images           |       Да | Да, MinIO |        Да | Готово                                 |
| Manual compatibility       |       Да |        Да |        Да | Готово                                 |
| Compatibility graph        |       Да |        Да |        Да | Готово                                 |
| Attribute rules            |       Да |        Да |        Да | Готово для `attribute ↔ attribute`     |
| Compatibility explanations |       Да |        Да |        Да | Готово                                 |
| Transitive compatibility   |       Да |   Да, BFS |        Да | Готово                                 |
| Multi-component search     |       Да |        Да |        Да | Готово                                 |
| Compatibility intersection |       Да |        Да |        Да | Готово                                 |
| Saved configurations       |       Да |        Да |        Да | Готово                                 |
| JSON configuration export  |       Да |        Да |        Да | Готово                                 |
| Register/login/JWT         |       Да |       Нет |       Нет | Осознанно перенесено                   |

## Результаты проверки

- `./gradlew clean build` — успешно;
- backend tests — 345, failures 0;
- local integration contracts — 167, failures 0;
- external integration contracts — 167, failures 0;
- JaCoCo line coverage — 2594/2801, **92.61%** при minimum 90%;
- Spotless и ArchUnit — успешно;
- Docker image с `configurator-0.1.0-SNAPSHOT.jar` — успешно;
- `/v3/api-docs` и `/swagger-ui/index.html` — smoke-check успешно;
- Compose configuration и GitHub YAML — синтаксически валидны.

Во время аудита исправлена фактическая ошибка документации API: legacy `springdoc-openapi-ui:1.8.0` заменён на
совместимый со Spring Boot 3 starter `springdoc-openapi-starter-webmvc-ui:2.8.17`.

## Что подготовлено для GitHub

- актуальные README и AGENTS;
- MIT license, changelog, contributing, security policy и code of conduct;
- CODEOWNERS, PR template и issue forms;
- Dependabot для Gradle, Docker и Actions;
- CI с минимальными token permissions и actions, закреплёнными на commit SHA;
- release notes categories;
- tag workflow, который проверяет commit из `master`, выполняет оба тестовых контура и создаёт draft pre-release с JAR,
  OpenAPI и `SHA256SUMS`.

## Действия перед публикацией

1. Создать PR `feature/CON1-83 -> develop` и дождаться первого реального CI run.
2. Настроить description/topics и rulesets по `RELEASE_CHECKLIST.md`.
3. Включить Dependabot alerts/security updates, secret scanning и private vulnerability reporting.
4. Создать и проверить PR `develop -> master`.
5. После зелёного CI на `master` поставить tag `v0.1.0`.
6. Проверить созданный draft release и опубликовать его как pre-release.

## Технический долг, не блокирующий preview

- В OpenAPI отсутствуют явные `operationId`; generator создаёт нестабильные имена и предупреждения.
- `ComponentMapper` выводит два предупреждения о target properties для элементов attributes; mapping следует сделать
  явным.
- Используются deprecated Gradle APIs, несовместимые с будущим Gradle 9; источник нужно найти через
  `--warning-mode all`.
- OpenAPI Generator помечает поддержку OpenAPI 3.1 как beta.
- В `application.yml` включён `DEBUG` для jOOQ logger; production profile должен снизить уровень.
- Нет Actuator health/readiness, metrics и operational runbook.
- Docker Compose credentials являются development-only; production secrets и TLS не настроены.
- Base images используют теги, а не immutable digests; release supply chain можно усилить SBOM и provenance.

## Блокеры production-ready v1.0.0

- Spring Security/JWT, реальный current user и authorization matrix;
- negative security integration tests;
- production deployment/configuration, secret management и TLS;
- observability, backup/restore и migration runbook;
- формализованная compatibility/support policy.
