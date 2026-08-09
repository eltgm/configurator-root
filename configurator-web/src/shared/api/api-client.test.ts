import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { client, getDomains, type DomainPage } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const developmentBaseUrl = '/api';
const testBaseUrl = testApiBaseUrl;

describe('generated API client', () => {
  it('uses the same-origin API prefix by default', () => {
    client.setConfig({ baseUrl: developmentBaseUrl });
    expect(client.getConfig().baseUrl).toBe(developmentBaseUrl);
  });

  it('serializes query parameters and returns a typed response', async () => {
    const response: DomainPage = {
      items: [
        {
          id: 101,
          name: 'Сборка ПК',
          description: 'Тестовая предметная область',
          createdAt: '2026-08-09T12:00:00Z',
        },
      ],
      page: 2,
      size: 10,
      totalItems: 21,
    };

    server.use(
      http.get(`${testBaseUrl}/domains`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('size')).toBe('10');
        return HttpResponse.json(response);
      }),
    );
    client.setConfig({ baseUrl: testBaseUrl });

    const result = await getDomains({
      client,
      query: { page: 2, size: 10 },
    });

    expect(result.error).toBeUndefined();
    expect(result.data).toEqual(response);
  });
});
