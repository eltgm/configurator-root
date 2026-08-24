import { Alert, Button, Group, Loader, Stack, Text } from '@mantine/core';
import {
  IconAlertTriangle,
  IconCircleCheck,
  IconInfoCircle,
  IconRefresh,
} from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

export type AssemblyCompatibilityState =
  'empty' | 'pending' | 'valid' | 'transitive' | 'conflict' | 'blocked' | 'error';

interface AssemblyCompatibilityStatusProps {
  state: AssemblyCompatibilityState;
  conflictCount: number;
  onRetry?: () => void;
  onShowDetails?: () => void;
}

export function AssemblyCompatibilityStatus({
  state,
  conflictCount,
  onRetry,
  onShowDetails,
}: AssemblyCompatibilityStatusProps) {
  const { t } = useTranslation();

  if (state === 'empty') {
    return null;
  }
  if (state === 'pending') {
    return (
      <Alert color="blue" icon={<Loader size="sm" />} title={t('configurator.validation.pending')}>
        {t('configurator.validation.pendingDescription')}
      </Alert>
    );
  }
  if (state === 'valid') {
    return (
      <Alert
        color="green"
        icon={<IconCircleCheck aria-hidden="true" />}
        title={t('configurator.validation.valid')}
        styles={{ label: { color: 'var(--mantine-color-text)' } }}
      >
        <Stack gap="sm">
          <Text size="sm">{t('configurator.validation.validDescription')}</Text>
          {onShowDetails ? (
            <Button size="xs" variant="light" w="fit-content" onClick={onShowDetails}>
              {t('configurator.validation.showDetails')}
            </Button>
          ) : null}
        </Stack>
      </Alert>
    );
  }
  if (state === 'transitive') {
    return (
      <Alert
        color="violet"
        icon={<IconInfoCircle aria-hidden="true" />}
        title={t('configurator.validation.transitive')}
      >
        <Stack gap="sm">
          <Text size="sm">{t('configurator.validation.transitiveDescription')}</Text>
          {onShowDetails ? (
            <Button
              size="xs"
              variant="light"
              color="violet"
              w="fit-content"
              onClick={onShowDetails}
            >
              {t('configurator.validation.showDetails')}
            </Button>
          ) : null}
        </Stack>
      </Alert>
    );
  }
  if (state === 'conflict') {
    return (
      <Alert
        color="red"
        icon={<IconAlertTriangle aria-hidden="true" />}
        title={t('configurator.validation.conflict')}
      >
        <Stack gap="sm">
          <Text size="sm">
            {t('configurator.validation.conflictDescription', { count: conflictCount })}
          </Text>
          {onShowDetails ? (
            <Button size="xs" variant="light" color="red" w="fit-content" onClick={onShowDetails}>
              {t('configurator.validation.showDetails')}
            </Button>
          ) : null}
        </Stack>
      </Alert>
    );
  }
  if (state === 'blocked') {
    return (
      <Alert
        color="orange"
        icon={<IconInfoCircle aria-hidden="true" />}
        title={t('configurator.validation.blocked')}
      >
        {t('configurator.validation.blockedDescription')}
      </Alert>
    );
  }
  return (
    <Alert
      color="orange"
      icon={<IconAlertTriangle aria-hidden="true" />}
      title={t('configurator.validation.error')}
    >
      <Group justify="space-between" align="center">
        <Text size="sm">{t('configurator.validation.errorDescription')}</Text>
        {onRetry ? (
          <Button
            size="xs"
            variant="light"
            leftSection={<IconRefresh size={14} />}
            onClick={onRetry}
          >
            {t('configurator.validation.retry')}
          </Button>
        ) : null}
      </Group>
    </Alert>
  );
}
