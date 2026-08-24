# План 9.15 — создание, редактирование и карточка компонента

## Цель

Завершить основной CRUD-путь каталога: пользователь создаёт компонент с типизированными атрибутами, открывает полную
карточку и редактирует разрешённые поля. Управление изображениями остаётся отдельной задачей 9.16.

## Границы

- OpenAPI, backend, Flyway и generated API не меняются.
- Используются существующие `GET/POST/PUT /components` и archive/restore mutations.
- Тип существующего компонента неизменяем.
- Архивный компонент доступен только для просмотра и восстановления.
- Локальный `testcontainers.properties` не входит в изменения.

## Реализация

1. Расширить query/mutation слой detail, create и update операциями с domain-scoped cache keys.
2. Добавить маршруты создания, карточки и редактирования.
3. Реализовать переиспользуемую форму с React Hook Form/Zod и controls для всех типов атрибутов.
4. Реализовать карточку с метаданными, атрибутами, preview изображений и archive/restore действиями.
5. Связать каталог с новыми маршрутами и добавить защиту несохранённых изменений.
6. Добавить русскую/английскую локализацию, адаптивные стили, component/API/E2E tests.
7. Выполнить `npm run check`, проверить coverage и состав diff.

## Проверка

- API/query tests: detail key, create/update payload и invalidation.
- Component tests: create, все типы атрибутов, edit, detail, archive/restore, errors и unsaved guard.
- E2E smoke: переход из каталога, создание и открытие карточки на mock API.
- Полный frontend quality gate: `npm run check`.
