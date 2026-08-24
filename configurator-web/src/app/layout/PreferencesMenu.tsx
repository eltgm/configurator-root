import { ActionIcon, Menu, type MantineColorScheme, useMantineColorScheme } from '@mantine/core';
import {
  IconCheck,
  IconDeviceDesktop,
  IconLanguage,
  IconMoon,
  IconSettings,
  IconSun,
} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { type SupportedLocale, supportedLocales } from '@/shared/config/preferences';
import { changeLocale } from '@/shared/i18n/i18n';

const colorSchemes: Array<{
  value: MantineColorScheme;
  labelKey: string;
  icon: typeof IconSun;
}> = [
  { value: 'auto', labelKey: 'preferences.themes.auto', icon: IconDeviceDesktop },
  { value: 'light', labelKey: 'preferences.themes.light', icon: IconSun },
  { value: 'dark', labelKey: 'preferences.themes.dark', icon: IconMoon },
];

export function PreferencesMenu() {
  const { t, i18n } = useTranslation();
  const { colorScheme, setColorScheme } = useMantineColorScheme();
  const currentLocale = i18n.resolvedLanguage?.split('-')[0] ?? 'ru';

  const selectLocale = (locale: SupportedLocale) => {
    void changeLocale(locale);
  };

  return (
    <Menu
      position="bottom-end"
      width={240}
      withinPortal
      trapFocus={false}
      withInitialFocusPlaceholder={false}
    >
      <Menu.Target>
        <ActionIcon variant="subtle" size="lg" aria-label={t('preferences.open')}>
          <IconSettings size={22} stroke={1.8} />
        </ActionIcon>
      </Menu.Target>

      <Menu.Dropdown>
        <Menu.Label>{t('preferences.theme')}</Menu.Label>
        {colorSchemes.map(({ value, labelKey, icon: Icon }) => (
          <Menu.Item
            key={value}
            leftSection={<Icon size={18} />}
            rightSection={colorScheme === value ? <IconCheck size={16} /> : null}
            onClick={() => setColorScheme(value)}
          >
            {t(labelKey)}
          </Menu.Item>
        ))}

        <Menu.Divider />
        <Menu.Label>{t('preferences.language')}</Menu.Label>
        {supportedLocales.map((locale) => (
          <Menu.Item
            key={locale}
            leftSection={<IconLanguage size={18} />}
            rightSection={currentLocale === locale ? <IconCheck size={16} /> : null}
            onClick={() => selectLocale(locale)}
          >
            {t(`preferences.languages.${locale}`)}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  );
}
