import type { ReactNode } from 'react';

import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorState } from '@/shared/ui/ErrorState';
import { LoadingState } from '@/shared/ui/LoadingState';

interface ServerDataStateProps {
  isLoading: boolean;
  error?: unknown;
  isEmpty: boolean;
  children: ReactNode;
  loadingLabel?: string;
  emptyTitle: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  onRetry?: () => void;
}

export function ServerDataState({
  isLoading,
  error,
  isEmpty,
  children,
  loadingLabel,
  emptyTitle,
  emptyDescription,
  emptyAction,
  onRetry,
}: ServerDataStateProps) {
  if (isLoading) {
    return <LoadingState label={loadingLabel} />;
  }
  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />;
  }
  if (isEmpty) {
    return <EmptyState title={emptyTitle} description={emptyDescription} action={emptyAction} />;
  }

  return children;
}
