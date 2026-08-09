import { Alert, Button, Stack, Text } from '@mantine/core';
import { IconAlertTriangle, IconRefresh } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { getErrorTranslationKey, normalizeApiError } from '@/shared/api/errors';

interface ErrorStateProps {
  error: unknown;
  onRetry?: (() => void) | undefined;
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const { t } = useTranslation();
  const normalizedError = normalizeApiError(error);
  const title = t(getErrorTranslationKey(normalizedError));
  const description =
    normalizedError.kind === 'api' && normalizedError.publicMessage !== title
      ? normalizedError.publicMessage
      : t('errors.safeDescription');

  return (
    <Alert
      color="red"
      variant="light"
      icon={<IconAlertTriangle aria-hidden="true" />}
      title={title}
      role="alert"
    >
      <Stack align="flex-start" gap="md">
        <Text size="sm">{description}</Text>
        {onRetry && normalizedError.retryable ? (
          <Button
            size="xs"
            variant="light"
            color="red"
            leftSection={<IconRefresh size={16} aria-hidden="true" />}
            onClick={onRetry}
          >
            {t('states.retry')}
          </Button>
        ) : null}
      </Stack>
    </Alert>
  );
}
