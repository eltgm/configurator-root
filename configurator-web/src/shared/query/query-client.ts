import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';

import { normalizeApiError, type AppError } from '@/shared/api/errors';
import { showErrorNotification } from '@/shared/notifications/notifications';

const defaultStaleTime = 30_000;
const defaultGarbageCollectionTime = 5 * 60_000;

export type ErrorNotifier = (error: AppError) => void;

export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  return failureCount < 1 && normalizeApiError(error).retryable;
}

export function createAppQueryClient(notifyError: ErrorNotifier = showErrorNotification) {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (query.state.data !== undefined) {
          notifyError(normalizeApiError(error));
        }
      },
    }),
    mutationCache: new MutationCache({
      onError: (error) => {
        notifyError(normalizeApiError(error));
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: defaultStaleTime,
        gcTime: defaultGarbageCollectionTime,
        refetchOnWindowFocus: false,
        retry: shouldRetryQuery,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

export const queryClient = createAppQueryClient();
