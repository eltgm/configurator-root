import { Alert, Button, List, Stack, Text } from '@mantine/core';
import { IconAlertTriangle, IconRefresh } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import {
  getErrorDetailTranslations,
  getErrorTranslationKey,
  normalizeApiError,
} from '@/shared/api/errors';

interface ErrorStateProps {
  error: unknown;
  onRetry?: (() => void) | undefined;
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const { t } = useTranslation();
  const normalizedError = normalizeApiError(error);
  const title = t(getErrorTranslationKey(normalizedError));
  const localizedDetails = getErrorDetailTranslations(normalizedError).map(({ key, parameters }) =>
    t(key, parameters),
  );
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
        {localizedDetails.length === 1 ? <Text size="sm">{localizedDetails[0]}</Text> : null}
        {localizedDetails.length > 1 ? (
          <List size="sm">
            {localizedDetails.map((detail, index) => (
              <List.Item key={`${index}-${detail}`}>{detail}</List.Item>
            ))}
          </List>
        ) : null}
        {localizedDetails.length === 0 ? <Text size="sm">{description}</Text> : null}
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
