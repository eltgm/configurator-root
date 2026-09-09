import { Drawer, NavLink, Stack, UnstyledButton } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';

import {
  isNavigationItemActive,
  mobileNavigation,
  settingsNavigation,
} from '@/app/layout/navigation';
import classes from '@/app/layout/app-layout.module.css';

export function MobileNavigation() {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  const [opened, setOpened] = useState(false);
  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 48em)');
    const closeOnDesktop = () => {
      if (desktop.matches) setOpened(false);
    };
    desktop.addEventListener('change', closeOnDesktop);
    return () => desktop.removeEventListener('change', closeOnDesktop);
  }, []);
  return (
    <>
      <nav className={classes['mobile-navigation']} aria-label={t('navigation.mobileLabel')}>
        {mobileNavigation.map((item) => {
          const active = isNavigationItemActive(item, pathname);
          const Icon = item.icon;
          const content = (
            <>
              <Icon size={22} stroke={1.8} aria-hidden="true" />
              <span>{t(item.labelKey)}</span>
            </>
          );
          return item.matchPrefix ? (
            <UnstyledButton
              key={item.path}
              className={classes['mobile-navigation-item']}
              data-active={active || undefined}
              aria-expanded={opened}
              aria-controls="mobile-settings"
              onClick={() => setOpened(true)}
            >
              {content}
            </UnstyledButton>
          ) : (
            <UnstyledButton
              key={item.path}
              component={Link}
              to={item.path}
              className={classes['mobile-navigation-item']}
              data-active={active || undefined}
              aria-current={active ? (pathname === item.path ? 'page' : 'location') : undefined}
            >
              {content}
            </UnstyledButton>
          );
        })}
      </nav>
      <Drawer
        closeButtonProps={{ 'aria-label': t('common.close') }}
        id="mobile-settings"
        opened={opened}
        onClose={() => setOpened(false)}
        title={t('ux.settings')}
        position="bottom"
        size="auto"
      >
        <Stack component="nav" aria-label={t('ux.settings')} gap="xs">
          {settingsNavigation.map((item) => (
            <NavLink
              key={item.path}
              component={Link}
              to={item.path}
              label={t(item.labelKey)}
              active={isNavigationItemActive(item, pathname)}
              onClick={() => setOpened(false)}
            />
          ))}
          <NavLink
            component={Link}
            to="/help"
            label={t('ux.help')}
            onClick={() => setOpened(false)}
          />
        </Stack>
      </Drawer>
    </>
  );
}
