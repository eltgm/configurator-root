import { Box, Loader, Menu, Text, UnstyledButton } from '@mantine/core';
import {
  IconAlertCircle,
  IconCheck,
  IconChevronDown,
  IconDatabase,
  IconSettings,
} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import classes from '@/app/layout/app-layout.module.css';
import { useDomainContext } from '@/features/domains/model/domain-context';

export function DomainSelector() {
  const { t } = useTranslation();
  const { domains, selectedDomain, selectDomain, isLoading, error, refetch } = useDomainContext();
  const currentName = selectedDomain?.name ?? t('domain.none');
  const accessibleLabel = `${t('domain.label')}: ${currentName}`;

  return (
    <Menu position="bottom-end" width={300} withinPortal>
      <Menu.Target>
        <UnstyledButton className={classes['domain-context']} aria-label={accessibleLabel}>
          <IconDatabase size={20} stroke={1.7} aria-hidden="true" />
          <Box visibleFrom="sm" className={classes['domain-selector-text']}>
            <Text size="xs" c="dimmed" lh={1.1}>
              {t('domain.label')}
            </Text>
            <Text size="sm" fw={600} lh={1.3} truncate>
              {currentName}
            </Text>
          </Box>
          {isLoading ? (
            <Loader size={16} aria-label={t('domains.states.loading')} />
          ) : error ? (
            <IconAlertCircle size={17} color="var(--mantine-color-red-6)" aria-hidden="true" />
          ) : (
            <IconChevronDown size={16} aria-hidden="true" />
          )}
        </UnstyledButton>
      </Menu.Target>

      <Menu.Dropdown>
        <Menu.Label>{t('domains.selector.title')}</Menu.Label>
        {error ? (
          <Menu.Item leftSection={<IconAlertCircle size={18} />} onClick={refetch}>
            {t('domains.selector.retry')}
          </Menu.Item>
        ) : null}
        {!isLoading && !error && domains.length === 0 ? (
          <Menu.Item component={Link} to="/settings/domain">
            {t('domains.selector.empty')}
          </Menu.Item>
        ) : null}
        {domains.map((domain) => (
          <Menu.Item
            key={domain.id}
            rightSection={domain.id === selectedDomain?.id ? <IconCheck size={17} /> : null}
            onClick={() => selectDomain(domain.id)}
          >
            {domain.name}
          </Menu.Item>
        ))}
        <Menu.Divider />
        <Menu.Item component={Link} to="/settings/domain" leftSection={<IconSettings size={18} />}>
          {t('domains.selector.manage')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
