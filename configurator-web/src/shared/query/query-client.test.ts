import { describe, expect, it, vi } from 'vitest';

import { AppError } from '@/shared/api/errors';
import { createAppQueryClient, shouldRetryQuery } from '@/shared/query/query-client';

describe('application QueryClient', () => {
  it('retries only the first network or server failure', () => {
    const serverError = new AppError({
      kind: 'api',
      status: 503,
      retryable: true,
      cause: null,
    });
    const clientError = new AppError({
      kind: 'api',
      status: 400,
      retryable: false,
      cause: null,
    });

    expect(shouldRetryQuery(0, new TypeError('offline'))).toBe(true);
    expect(shouldRetryQuery(0, serverError)).toBe(true);
    expect(shouldRetryQuery(1, serverError)).toBe(false);
    expect(shouldRetryQuery(0, clientError)).toBe(false);
    expect(shouldRetryQuery(0, new Error('bug'))).toBe(false);
  });

  it('notifies about mutation failures without automatically retrying them', async () => {
    const notify = vi.fn();
    const queryClient = createAppQueryClient(notify);
    const mutationFn = vi.fn().mockRejectedValue(new TypeError('offline'));
    const mutation = queryClient.getMutationCache().build(queryClient, { mutationFn });

    await expect(mutation.execute(undefined)).rejects.toThrow('offline');

    expect(mutationFn).toHaveBeenCalledTimes(1);
    expect(notify).toHaveBeenCalledOnce();
    expect(notify.mock.calls[0]?.[0]).toMatchObject({ kind: 'network', retryable: true });
  });

  it('keeps initial query errors inline and notifies only when cached data already exists', async () => {
    const notify = vi.fn();
    const queryClient = createAppQueryClient(notify);

    await expect(
      queryClient.fetchQuery({
        queryKey: ['initial'],
        queryFn: () => Promise.reject(new TypeError('offline')),
        retry: false,
      }),
    ).rejects.toThrow('offline');
    expect(notify).not.toHaveBeenCalled();

    queryClient.setQueryData(['background'], { value: 'cached' });
    await expect(
      queryClient.fetchQuery({
        queryKey: ['background'],
        queryFn: () => Promise.reject(new TypeError('offline')),
        retry: false,
        staleTime: 0,
      }),
    ).rejects.toThrow('offline');

    expect(queryClient.getQueryData(['background'])).toEqual({ value: 'cached' });
    expect(notify).toHaveBeenCalledOnce();
  });
});
