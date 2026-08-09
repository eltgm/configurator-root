import { Divider, NavLink, ScrollArea, Stack, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';

import {
  isNavigationItemActive,
  primaryNavigation,
  settingsNavigation,
  type NavigationItem,
} from '@/app/layout/navigation';

function DesktopNavigationItem({ item }: { item: NavigationItem }) {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  const active = isNavigationItemActive(item, pathname);
  const Icon = item.icon;

  return (
    <NavLink
      component={Link}
      to={item.path}
      label={t(item.labelKey)}
      leftSection={<Icon size={20} stroke={1.7} />}
      active={active}
      aria-current={active ? 'page' : undefined}
      variant="light"
    />
  );
}

export function DesktopNavigation() {
  const { t } = useTranslation();

  return (
    <ScrollArea type="auto" h="100%">
      <Stack component="nav" aria-label={t('navigation.label')} gap={4} p="md">
        {primaryNavigation.map((item) => (
          <DesktopNavigationItem key={item.path} item={item} />
        ))}

        <Divider my="sm" />
        <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="sm" mb={4}>
          {t('navigation.settings')}
        </Text>

        {settingsNavigation.map((item) => (
          <DesktopNavigationItem key={item.path} item={item} />
        ))}
      </Stack>
    </ScrollArea>
  );
}
