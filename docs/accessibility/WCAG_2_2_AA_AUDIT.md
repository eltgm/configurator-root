# WCAG 2.2 AA Internal Audit

## Scope and status model

Матрица относится к `configurator-web` и задачам 9.26–9.27. Она является внутренним implementation audit, а не внешней
сертификацией. Проверка охватывает все критерии WCAG 2.2 уровней A и AA, актуальные для текущего SPA. Runtime auth,
аудио/видео, timed content и пользовательский ввод rich media в текущем продукте отсутствуют.

Статусы:

- `covered` — требование реализовано и имеет code/test/manual evidence;
- `manual` — реализация присутствует, окончательная проверка требует ручного браузерного прохода;
- `n/a` — критерий неприменим к текущему содержимому; при появлении соответствующего сценария он пересматривается;
- `manual` не повышается автоматически только из-за успешного axe scan: критерий требует отдельной ручной проверки.

Normative references: [WCAG 2.2](https://www.w3.org/TR/WCAG22/),
[Quick Reference](https://www.w3.org/WAI/WCAG22/quickref/).

## Audit matrix

| Criterion                             | Applicability and implementation                                                              | Evidence                                   | Status  |
| ------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------ | ------- |
| 1.1.1 Non-text Content                | Значимые изображения получают alt; декоративные icons/previews скрыты от AT.                  | Gallery/component tests, axe, manual audit | manual  |
| 1.2.1–1.2.5 Time-based Media          | Аудио и видео отсутствуют.                                                                    | Content inventory                          | n/a     |
| 1.3.1 Info and Relationships          | Landmarks, headings, labels, tables/lists и form associations выражены семантически.          | App/shared/feature tests, axe route matrix | covered |
| 1.3.2 Meaningful Sequence             | DOM order совпадает с визуальным; mobile перестраивает layout без CSS ordering.               | Keyboard and responsive checks             | manual  |
| 1.3.3 Sensory Characteristics         | Инструкции и actions не зависят только от формы/позиции.                                      | Copy and control audit                     | manual  |
| 1.3.4 Orientation                     | Layout не блокирует portrait/landscape.                                                       | Responsive Playwright                      | covered |
| 1.3.5 Identify Input Purpose          | Текущие предметные поля не являются персональными autofill-полями.                            | Form inventory                             | n/a     |
| 1.4.1 Use of Color                    | State дополняется текстом/icon/role.                                                          | State tests, axe themes, manual audit      | manual  |
| 1.4.2 Audio Control                   | Автовоспроизводимое аудио отсутствует.                                                        | Content inventory                          | n/a     |
| 1.4.3 Contrast (Minimum)              | Mantine tokens и собственные styles проверяются в light/dark.                                 | Axe light/dark scans, manual sampling      | manual  |
| 1.4.4 Resize Text                     | Контент работает при browser zoom 200%.                                                       | Manual zoom checklist                      | manual  |
| 1.4.5 Images of Text                  | Изображения текста не используются.                                                           | Asset inventory                            | covered |
| 1.4.10 Reflow                         | Линейный UI поддерживает 320 CSS px; graph canvas имеет линейную details alternative.         | Responsive CSS and Playwright              | covered |
| 1.4.11 Non-text Contrast              | Focus, controls и meaningful graphics проверяются в обеих темах.                              | Axe themes and manual contrast sampling    | manual  |
| 1.4.12 Text Spacing                   | Layout не фиксирует высоту текстовых containers и переносит длинный контент.                  | Manual text-spacing check                  | manual  |
| 1.4.13 Content on Hover or Focus      | Tooltip/menu content dismissible и не является единственным источником информации.            | Component/manual keyboard audit            | manual  |
| 2.1.1 Keyboard                        | Все business actions работают с клавиатуры; reorder использует кнопки.                        | Component and E2E keyboard paths           | covered |
| 2.1.2 No Keyboard Trap                | Mantine overlays закрываются и возвращают focus.                                              | Modal/drawer tests                         | covered |
| 2.1.4 Character Key Shortcuts         | Одноклавишные shortcuts отсутствуют.                                                          | Code inventory                             | n/a     |
| 2.2.1–2.2.2 Timing Adjustable / Pause | Timed limits, moving and auto-updating content отсутствуют.                                   | Behavior inventory                         | n/a     |
| 2.3.1 Three Flashes                   | Flashing content отсутствует.                                                                 | Content inventory                          | n/a     |
| 2.4.1 Bypass Blocks                   | Первый keyboard control — skip link к `main`.                                                 | App test and E2E                           | covered |
| 2.4.2 Page Titled                     | Каждая route page задаёт локализованный document title.                                       | Existing page tests                        | covered |
| 2.4.3 Focus Order                     | Tab order следует DOM и сохраняется при responsive layout.                                    | Keyboard E2E/manual                        | manual  |
| 2.4.4 Link Purpose (In Context)       | Links имеют предметные labels или доступный context.                                          | Component audit and axe route matrix       | covered |
| 2.4.5 Multiple Ways                   | Основные страницы доступны через navigation; детали — через каталог/списки и browser history. | Route/navigation inventory                 | covered |
| 2.4.6 Headings and Labels             | Один H1 на странице, labels описывают назначение controls.                                    | App/feature tests and axe route matrix     | covered |
| 2.4.7 Focus Visible                   | Global high-contrast `:focus-visible` indicator.                                              | CSS and manual themes                      | covered |
| 2.4.11 Focus Not Obscured (Minimum)   | Shell scroll offsets и overlay focus management не дают полностью скрыть focus.               | Responsive keyboard E2E                    | covered |
| 2.5.1 Pointer Gestures                | Multipoint/path gestures не требуются; graph имеет controls/search.                           | Interaction inventory                      | covered |
| 2.5.2 Pointer Cancellation            | Actions выполняются стандартным click activation/up event.                                    | Component interaction tests                | covered |
| 2.5.3 Label in Name                   | Visible button/link/form labels входят в accessible name.                                     | Component audit and axe interactions       | covered |
| 2.5.4 Motion Actuation                | Motion/device actuation отсутствует.                                                          | Interaction inventory                      | n/a     |
| 2.5.7 Dragging Movements              | Бизнес-функции используют кнопки; graph node dragging отключён.                               | Graph/gallery/rule tests                   | covered |
| 2.5.8 Target Size (Minimum)           | Icon controls минимум 24 px; основные mobile navigation/actions крупнее.                      | CSS/component/Playwright sampling          | covered |
| 3.1.1 Language of Page                | `<html lang>` синхронизирован с ru/en locale.                                                 | App tests                                  | covered |
| 3.1.2 Language of Parts               | Интерфейс переключает locale целиком; смешанные известные фрагменты не используются.          | Content inventory                          | covered |
| 3.2.1 On Focus                        | Focus сам не запускает route/mutation/context change.                                         | Interaction audit                          | covered |
| 3.2.2 On Input                        | Context-changing selections предсказуемы и подписаны; destructive actions подтверждаются.     | Feature tests                              | covered |
| 3.2.3 Consistent Navigation           | Общая desktop/mobile navigation model неизменна между routes.                                 | App tests                                  | covered |
| 3.2.4 Consistent Identification       | Общие actions/icons/labels идентичны между экранами.                                          | Shared UI inventory                        | covered |
| 3.2.6 Consistent Help                 | Постоянного механизма помощи пока нет.                                                        | Product inventory                          | n/a     |
| 3.3.1 Error Identification            | Field и request errors определяются текстом, не одним цветом.                                 | Form/error tests                           | covered |
| 3.3.2 Labels or Instructions          | Формы содержат labels, required/help/format instructions.                                     | Form tests                                 | covered |
| 3.3.3 Error Suggestion                | Исправимые ошибки содержат безопасное объяснение и field details.                             | Form/error tests                           | covered |
| 3.3.4 Error Prevention                | Безвозвратное удаление и значимые destructive actions требуют confirmation.                   | Modal/E2E tests                            | covered |
| 3.3.7 Redundant Entry                 | Повторный ввод персональной информации в текущих сценариях отсутствует.                       | Form inventory                             | n/a     |
| 3.3.8 Accessible Authentication       | Runtime auth UI пока отсутствует; пересмотреть вместе с реализацией auth.                     | Security limitation                        | n/a     |
| 4.1.1 Parsing                         | React/TypeScript создают валидную DOM structure; duplicate IDs проверяются code review/tests. | Build, component tests and axe             | covered |
| 4.1.2 Name, Role, Value               | Native/Mantine semantics, ARIA только для дополнения.                                         | Component tests and axe interactions       | covered |
| 4.1.3 Status Messages                 | Loading, validation, conflict, result и notification используют status/alert/live regions.    | Shared/feature tests                       | covered |

## Manual verification checklist

- [x] Chromium: 320 x 568, 360 x 800, tablet and desktop, light/dark, portrait/landscape.
- [x] Firefox: representative 320 px and desktop keyboard journeys.
- [x] Safari/WebKit: representative 320 px and desktop keyboard journeys.
- [ ] Safari + VoiceOver: navigation, domain selection, one form, graph details, configurator and destructive modal.
- [ ] Browser zoom 200% and equivalent 320 CSS px reflow on every route group.
- [ ] Text spacing: line height 1.5, paragraph spacing 2, letter spacing 0.12 and word spacing 0.16 of font size.
- [ ] Light/dark contrast sampling for text, focus, inputs, badges, alerts, graph nodes and disabled states.
- [ ] Keyboard: skip link, navigation, menus, dialogs/drawers, forms, tables/cards, gallery, graph and configurator.

## Known follow-ups

- Axe automation не заменяет оставшиеся ручные проверки VoiceOver, zoom, text spacing, contrast semantics и focus.
- Authentication criteria must be reopened when runtime authentication/authorization and auth UI are implemented.
- Formal conformance requires an independent manual audit against released content and environment.
