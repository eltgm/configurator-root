import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { fetchAllDomains } from '@/features/domains/api/domains';
import type { DomainPage } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

describe('domain API queries', () => {
  it('loads every API page with the maximum supported page size', async () => {
    const requestedPages: Array<string | null> = [];
    server.use(
      http.get(`${testApiBaseUrl}/domains`, ({ request }) => {
        const url = new URL(request.url);
        const page = url.searchParams.get('page');
        requestedPages.push(page);
        const response: DomainPage = {
          items: [
            {
              id: page === '0' ? 1 : 2,
              name: page === '0' ? 'Первая' : 'Вторая',
              createdAt: '2026-08-09T12:00:00Z',
            },
          ],
          page: Number(page),
          size: 100,
          totalItems: 2,
        };
        expect(url.searchParams.get('size')).toBe('100');
        return HttpResponse.json(response);
      }),
    );

    await expect(fetchAllDomains()).resolves.toEqual([
      expect.objectContaining({ id: 1, name: 'Первая' }),
      expect.objectContaining({ id: 2, name: 'Вторая' }),
    ]);
    expect(requestedPages).toEqual(['0', '1']);
  });

  it('stops safely if an inconsistent page is empty', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/domains`, () =>
        HttpResponse.json({ items: [], page: 0, size: 100, totalItems: 10 }),
      ),
    );

    await expect(fetchAllDomains()).resolves.toEqual([]);
  });
});
