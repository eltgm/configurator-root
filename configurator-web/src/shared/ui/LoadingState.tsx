import { Paper, Skeleton, Stack, VisuallyHidden } from '@mantine/core';
import { useTranslation } from 'react-i18next';

interface LoadingStateProps {
  label?: string | undefined;
  rows?: number;
}

export function LoadingState({ label, rows = 3 }: LoadingStateProps) {
  const { t } = useTranslation();
  const accessibleLabel = label ?? t('states.loading');

  return (
    <Paper p="lg" withBorder role="status" aria-label={accessibleLabel} aria-live="polite">
      <VisuallyHidden>{accessibleLabel}</VisuallyHidden>
      <Stack gap="sm" aria-hidden="true">
        {Array.from({ length: rows }, (_, index) => (
          <Skeleton key={index} height={index === 0 ? 28 : 52} radius="md" />
        ))}
      </Stack>
    </Paper>
  );
}
