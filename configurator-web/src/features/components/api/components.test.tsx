import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import {
  componentKeys,
  fetchComponents,
  normalizeComponentCatalogFilters,
  useArchiveComponentMutation,
  useRestoreComponentMutation,
} from '@/features/components/api/components';
import {
  isComponentCatalogView,
  readComponentCatalogView,
  saveComponentCatalogView,
  toComponentImageUrl,
} from '@/features/components/model/catalog-preferences';
import { componentCatalogViewStorageKey } from '@/shared/config/preferences';
import { server, testApiBaseUrl } from '@/test/server';

const filters = {
  componentTypeId: 11,
  name: '  Ryzen  ',
  archived: false,
  page: 2,
  size: 12,
} as const;

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('component catalog API', () => {
  it('normalizes filters, sends every server-side filter and uses a scoped query key', async () => {
    let requestedUrl: URL | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/components`, ({ request }) => {
        requestedUrl = new URL(request.url);
        return HttpResponse.json({ items: [], page: 2, size: 12, totalItems: 0 });
      }),
    );

    await fetchComponents(7, filters);

    expect(requestedUrl?.pathname).toBe('/api/domains/7/components');
    expect(Object.fromEntries(requestedUrl?.searchParams ?? [])).toEqual({
      componentTypeId: '11',
      name: 'Ryzen',
      archived: 'false',
      page: '2',
      size: '12',
    });
    expect(componentKeys.catalog(7, filters)).toEqual([
      'domains',
      7,
      'components',
      normalizeComponentCatalogFilters(filters),
    ]);
  });

  it('omits blank search and an unset type from query parameters', () => {
    expect(
      normalizeComponentCatalogFilters({
        name: '   ',
        archived: true,
        page: 0,
        size: 12,
      }),
    ).toEqual({ archived: true, page: 0, size: 12 });
  });

  it('invalidates all component views in the domain after archive and restore', async () => {
    server.use(
      http.delete(
        `${testApiBaseUrl}/components/:id`,
        () => new HttpResponse(null, { status: 204 }),
      ),
      http.post(`${testApiBaseUrl}/components/:id/restore`, ({ params }) =>
        HttpResponse.json({
          id: Number(params['id']),
          componentTypeId: 11,
          name: 'Ryzen',
          archived: false,
          createdAt: '2026-08-09T12:00:00Z',
        }),
      ),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    const wrapper = createWrapper(queryClient);
    const archive = renderHook(() => useArchiveComponentMutation(), { wrapper });
    const restore = renderHook(() => useRestoreComponentMutation(), { wrapper });

    archive.result.current.mutate({ domainId: 7, id: 101 });
    await waitFor(() => expect(archive.result.current.isSuccess).toBe(true));
    restore.result.current.mutate({ domainId: 7, id: 101 });
    await waitFor(() => expect(restore.result.current.isSuccess).toBe(true));

    expect(invalidate).toHaveBeenNthCalledWith(1, { queryKey: componentKeys.byDomain(7) });
    expect(invalidate).toHaveBeenNthCalledWith(2, { queryKey: componentKeys.byDomain(7) });
  });
});

describe('component catalog preferences', () => {
  it('persists valid modes and falls back to cards for unknown values', () => {
    window.localStorage.setItem(componentCatalogViewStorageKey, 'unknown');
    expect(readComponentCatalogView()).toBe('cards');

    saveComponentCatalogView('table');
    expect(readComponentCatalogView()).toBe('table');
    expect(isComponentCatalogView('cards')).toBe(true);
    expect(isComponentCatalogView('grid')).toBe(false);
  });

  it('keeps image content behind the frontend API boundary', () => {
    expect(toComponentImageUrl('/component-images/5/content')).toBe(
      '/api/component-images/5/content',
    );
    expect(toComponentImageUrl('component-images/5/content')).toBe(
      '/api/component-images/5/content',
    );
    expect(toComponentImageUrl('/api/component-images/5/content')).toBe(
      '/api/component-images/5/content',
    );
  });
});
