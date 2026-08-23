## Что изменено

<!-- Кратко опишите результат и причину изменения. -->

## Проверки

- [ ] `./gradlew build`
- [ ] `./gradlew :configurator-integration-tests:externalIntegrationTest`
- [ ] OpenAPI обновлён либо не затронут
- [ ] Flyway/jOOQ обновлены либо не затронуты
- [ ] Local и external integration contracts эквивалентны
- [ ] Frontend: `npm run check` и `npm run test:coverage` выполнены либо frontend не затронут
- [ ] Frontend browser gates (`test:e2e`, `test:accessibility`, `test:visual`) выполнены либо не затронуты
- [ ] Visual baselines обновлены в pinned Docker image и reviewed либо UI визуально не изменялся
- [ ] Нет generated, IDE, secret или host-specific файлов

## Риски и ограничения

<!-- Укажите migrations, compatibility concerns и то, что не удалось проверить. -->
