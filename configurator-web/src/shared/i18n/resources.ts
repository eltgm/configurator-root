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
      common: {
        cancel: 'Отмена',
        save: 'Сохранить',
        create: 'Создать',
      },
      domains: {
        states: {
          loading: 'Загрузка предметных областей',
        },
        selector: {
          title: 'Выберите предметную область',
          retry: 'Не удалось загрузить. Повторить',
          empty: 'Создать первую область',
          manage: 'Управление областями',
        },
        actions: {
          create: 'Новая область',
          createDemo: 'Добавить демо',
          createDemoFull: 'Создать демо «Сборка ПК»',
          edit: 'Редактировать',
          editNamed: 'Редактировать область {{name}}',
          delete: 'Удалить',
          deleteNamed: 'Удалить область {{name}}',
          select: 'Выбрать',
        },
        firstRun: {
          title: 'Начните с предметной области',
          description:
            'Предметная область объединяет типы, компоненты, правила совместимости и сохранённые конфигурации.',
          hint: 'Это не обязательный мастер: позже можно добавить другие области и переключаться между ними в верхнем меню.',
        },
        form: {
          createTitle: 'Новая предметная область',
          editTitle: 'Редактирование области',
          name: 'Название',
          namePlaceholder: 'Например, Сборка рабочего места',
          description: 'Описание',
          descriptionPlaceholder: 'Кратко опишите назначение области',
          validation: {
            nameRequired: 'Введите название',
            nameTooLong: 'Название должно содержать не более 255 символов',
          },
        },
        notifications: {
          created: 'Предметная область создана',
          demoCreated: 'Демо «Сборка ПК» создано',
          updated: 'Изменения сохранены',
          deleted: 'Предметная область удалена',
        },
        management: {
          title: 'Предметные области',
          description: 'Создавайте области, выбирайте текущую и изменяйте их основные параметры.',
          emptyTitle: 'Предметных областей пока нет',
          emptyDescription: 'Создайте пустую область или добавьте готовый демонстрационный пример.',
          current: 'Текущая',
          noDescription: 'Описание не задано',
          createdAt: 'Создана {{date}}',
        },
        delete: {
          title: 'Удалить предметную область?',
          description: 'Область «{{name}}» будет удалена.',
          warning:
            'Действие необратимо. Если в области есть связанные данные, сервер безопасно отклонит удаление.',
        },
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
      states: {
        loading: 'Загрузка данных',
        retry: 'Повторить',
      },
      errors: {
        safeDescription:
          'Попробуйте повторить действие. Если ошибка сохранится, перезапустите приложение.',
        network: 'Нет связи с сервером',
        unknown: 'Не удалось выполнить действие',
        codes: {
          BUSINESS_ERROR: 'Действие невозможно выполнить',
          INTERNAL_ERROR: 'Внутренняя ошибка сервера',
          NOT_FOUND: 'Данные не найдены',
          ENTITY_ALREADY_EXISTS: 'Такая запись уже существует',
          ENTITY_HAS_RELATED_ENTITIES: 'Запись используется другими данными',
          COMPONENT_ARCHIVED: 'Компонент находится в архиве',
          CONFIGURATION_CONFLICT: 'Компоненты конфигурации несовместимы',
          VALIDATION_ERROR: 'Проверьте введённые данные',
          IMAGE_TOO_LARGE: 'Изображение слишком большое',
          UNSUPPORTED_IMAGE_FORMAT: 'Формат изображения не поддерживается',
          EXTERNAL_STORAGE_UNAVAILABLE: 'Хранилище изображений недоступно',
        },
      },
      routeError: {
        title: 'Произошла ошибка',
        description: 'Вернитесь в конфигуратор или перезагрузите приложение.',
        action: 'Вернуться в конфигуратор',
        reload: 'Перезагрузить приложение',
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
      common: {
        cancel: 'Cancel',
        save: 'Save',
        create: 'Create',
      },
      domains: {
        states: {
          loading: 'Loading domains',
        },
        selector: {
          title: 'Select a domain',
          retry: 'Could not load. Try again',
          empty: 'Create the first domain',
          manage: 'Manage domains',
        },
        actions: {
          create: 'New domain',
          createDemo: 'Add demo',
          createDemoFull: 'Create “PC Build” demo',
          edit: 'Edit',
          editNamed: 'Edit domain {{name}}',
          delete: 'Delete',
          deleteNamed: 'Delete domain {{name}}',
          select: 'Select',
        },
        firstRun: {
          title: 'Start with a domain',
          description:
            'A domain groups component types, components, compatibility rules, and saved configurations.',
          hint: 'This is not a required wizard: you can add more domains later and switch between them from the top menu.',
        },
        form: {
          createTitle: 'New domain',
          editTitle: 'Edit domain',
          name: 'Name',
          namePlaceholder: 'For example, Workplace build',
          description: 'Description',
          descriptionPlaceholder: 'Briefly describe the purpose of this domain',
          validation: {
            nameRequired: 'Enter a name',
            nameTooLong: 'The name must contain no more than 255 characters',
          },
        },
        notifications: {
          created: 'Domain created',
          demoCreated: '“PC Build” demo created',
          updated: 'Changes saved',
          deleted: 'Domain deleted',
        },
        management: {
          title: 'Domains',
          description: 'Create domains, select the current one, and edit their main settings.',
          emptyTitle: 'There are no domains yet',
          emptyDescription: 'Create an empty domain or add the ready-to-use demonstration example.',
          current: 'Current',
          noDescription: 'No description',
          createdAt: 'Created {{date}}',
        },
        delete: {
          title: 'Delete domain?',
          description: 'The “{{name}}” domain will be deleted.',
          warning:
            'This action cannot be undone. If the domain has related data, the server will safely reject deletion.',
        },
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
      states: {
        loading: 'Loading data',
        retry: 'Try again',
      },
      errors: {
        safeDescription: 'Try again. If the error persists, restart the application.',
        network: 'Cannot connect to the server',
        unknown: 'The action could not be completed',
        codes: {
          BUSINESS_ERROR: 'The action cannot be completed',
          INTERNAL_ERROR: 'Internal server error',
          NOT_FOUND: 'Data was not found',
          ENTITY_ALREADY_EXISTS: 'This record already exists',
          ENTITY_HAS_RELATED_ENTITIES: 'The record is used by other data',
          COMPONENT_ARCHIVED: 'The component is archived',
          CONFIGURATION_CONFLICT: 'Configuration components are incompatible',
          VALIDATION_ERROR: 'Check the entered data',
          IMAGE_TOO_LARGE: 'The image is too large',
          UNSUPPORTED_IMAGE_FORMAT: 'The image format is not supported',
          EXTERNAL_STORAGE_UNAVAILABLE: 'Image storage is unavailable',
        },
      },
      routeError: {
        title: 'Something went wrong',
        description: 'Return to the configurator or reload the application.',
        action: 'Return to configurator',
        reload: 'Reload application',
      },
      notFound: {
        title: 'Page not found',
        description: 'Check the address or return to the configurator.',
        action: 'Return to configurator',
      },
    },
  },
} as const;
