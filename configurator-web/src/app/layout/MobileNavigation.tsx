import { UnstyledButton } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';

import { isNavigationItemActive, mobileNavigation } from '@/app/layout/navigation';
import classes from '@/app/layout/app-layout.module.css';

export function MobileNavigation() {
  const { pathname } = useLocation();
  const { t } = useTranslation();

  return (
    <nav className={classes['mobile-navigation']} aria-label={t('navigation.mobileLabel')}>
      {mobileNavigation.map((item) => {
        const active = isNavigationItemActive(item, pathname);
        const Icon = item.icon;

        return (
          <UnstyledButton
            key={item.path}
            component={Link}
            to={item.path}
            className={classes['mobile-navigation-item']}
            data-active={active || undefined}
            aria-current={active ? 'page' : undefined}
          >
            <Icon size={22} stroke={1.8} aria-hidden="true" />
            <span>{t(item.labelKey)}</span>
          </UnstyledButton>
        );
      })}
    </nav>
  );
}
