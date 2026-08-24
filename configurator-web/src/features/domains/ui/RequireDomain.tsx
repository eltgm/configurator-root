import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { useDomainContext } from '@/features/domains/model/domain-context';
import { FirstRunState } from '@/features/domains/ui/FirstRunState';
import { ErrorState, LoadingState } from '@/shared/ui';

interface RequireDomainProps {
  children: ReactNode;
}

export function RequireDomain({ children }: RequireDomainProps) {
  const { t } = useTranslation();
  const { selectedDomain, isLoading, error, refetch } = useDomainContext();

  if (isLoading) {
    return <LoadingState label={t('domains.states.loading')} />;
  }
  if (error) {
    return <ErrorState error={error} onRetry={refetch} />;
  }
  if (!selectedDomain) {
    return <FirstRunState />;
  }

  return children;
}
