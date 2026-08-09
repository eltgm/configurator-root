import { notifications } from '@mantine/notifications';

import { getErrorTranslationKey, normalizeApiError } from '@/shared/api/errors';
import { i18n } from '@/shared/i18n/i18n';

export function showErrorNotification(error: unknown) {
  const normalizedError = normalizeApiError(error);
  const localizedTitle = i18n.t(getErrorTranslationKey(normalizedError));
  const description =
    normalizedError.kind === 'api' && normalizedError.publicMessage !== localizedTitle
      ? normalizedError.publicMessage
      : undefined;

  notifications.show({
    color: 'red',
    title: localizedTitle,
    message: description,
  });
}

export function showSuccessNotification(title: string, message?: string) {
  notifications.show({
    color: 'green',
    title,
    message,
  });
}
