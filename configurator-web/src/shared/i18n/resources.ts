export const resources = {
  ru: {
    translation: {
      app: {
        name: 'Конфигуратор',
        fullName: 'Конфигуратор компонентов',
      },
      domain: {
        label: 'Предметная область',
        none: 'Не выбрана',
      },
      navigation: {
        label: 'Основная навигация',
        mobileLabel: 'Мобильная навигация',
        skipToContent: 'Перейти к содержимому',
        configurator: 'Конфигуратор',
        components: 'Компоненты',
        configurations: 'Конфигурации',
        settings: 'Настройка',
        types: 'Типы и атрибуты',
        manualCompatibility: 'Ручная совместимость',
        compatibilityRules: 'Автоматические правила',
        compatibilityGraph: 'Граф совместимости',
        domainSettings: 'Параметры области',
      },
      preferences: {
        open: 'Настройки интерфейса',
        theme: 'Тема',
        language: 'Язык',
        themes: {
          auto: 'Системная',
          light: 'Светлая',
          dark: 'Тёмная',
        },
        languages: {
          ru: 'Русский',
          en: 'English',
        },
      },
      pages: {
        status: 'Раздел подготовлен',
        configurator: {
          title: 'Конфигуратор',
          description:
            'Здесь появится свободная сборка конфигурации из компонентов выбранной предметной области.',
        },
        components: {
          title: 'Компоненты',
          description: 'Каталог компонентов с карточками, таблицей, поиском, фильтрами и архивом.',
        },
        configurations: {
          title: 'Конфигурации',
          description:
            'Сохранённые конфигурации, их просмотр, редактирование, копирование и экспорт.',
        },
        settingsTypes: {
          title: 'Типы и атрибуты',
          description:
            'Структура типов компонентов и определения их обязательных и дополнительных атрибутов.',
        },
        manualCompatibility: {
          title: 'Ручная совместимость',
          description: 'Управление явно заданными связями совместимости между компонентами.',
        },
        compatibilityRules: {
          title: 'Автоматические правила',
          description: 'Правила совместимости, которые сравнивают значения атрибутов компонентов.',
        },
        compatibilityGraph: {
          title: 'Граф совместимости',
          description:
            'Интерактивное представление прямых и вычисленных связей предметной области.',
        },
        domainSettings: {
          title: 'Параметры предметной области',
          description: 'Название, описание и служебные действия для текущей предметной области.',
        },
      },
      notFound: {
        title: 'Страница не найдена',
        description: 'Проверьте адрес или вернитесь в конфигуратор.',
        action: 'Вернуться в конфигуратор',
      },
    },
  },
  en: {
    translation: {
      app: {
        name: 'Configurator',
        fullName: 'Component Configurator',
      },
      domain: {
        label: 'Domain',
        none: 'Not selected',
      },
      navigation: {
        label: 'Main navigation',
        mobileLabel: 'Mobile navigation',
        skipToContent: 'Skip to content',
        configurator: 'Configurator',
        components: 'Components',
        configurations: 'Configurations',
        settings: 'Settings',
        types: 'Types and attributes',
        manualCompatibility: 'Manual compatibility',
        compatibilityRules: 'Automatic rules',
        compatibilityGraph: 'Compatibility graph',
        domainSettings: 'Domain settings',
      },
      preferences: {
        open: 'Interface settings',
        theme: 'Theme',
        language: 'Language',
        themes: {
          auto: 'System',
          light: 'Light',
          dark: 'Dark',
        },
        languages: {
          ru: 'Русский',
          en: 'English',
        },
      },
      pages: {
        status: 'Section prepared',
        configurator: {
          title: 'Configurator',
          description:
            'This section will provide free-form configuration building for the selected domain.',
        },
        components: {
          title: 'Components',
          description: 'Component catalog with cards, table, search, filters, and archive.',
        },
        configurations: {
          title: 'Configurations',
          description: 'Saved configurations with viewing, editing, copying, and export.',
        },
        settingsTypes: {
          title: 'Types and attributes',
          description: 'Component type structure and required or optional attribute definitions.',
        },
        manualCompatibility: {
          title: 'Manual compatibility',
          description: 'Manage explicitly defined compatibility links between components.',
        },
        compatibilityRules: {
          title: 'Automatic rules',
          description: 'Compatibility rules that compare component attribute values.',
        },
        compatibilityGraph: {
          title: 'Compatibility graph',
          description: 'Interactive view of direct and computed links in the domain.',
        },
        domainSettings: {
          title: 'Domain settings',
          description: 'Name, description, and service actions for the current domain.',
        },
      },
      notFound: {
        title: 'Page not found',
        description: 'Check the address or return to the configurator.',
        action: 'Return to configurator',
      },
    },
  },
} as const;
