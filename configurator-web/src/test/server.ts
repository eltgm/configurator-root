import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

import type { DomainPage } from '@/shared/api';

export const testApiBaseUrl = 'http://localhost/api';

const defaultDomainPage: DomainPage = {
  items: [
    {
      id: 101,
      name: 'Сборка ПК',
      description: 'Тестовая предметная область',
      createdAt: '2026-08-09T12:00:00Z',
    },
  ],
  page: 0,
  size: 100,
  totalItems: 1,
};

export const server = setupServer(
  http.get(`${testApiBaseUrl}/domains`, () => HttpResponse.json(defaultDomainPage)),
  http.get(`${testApiBaseUrl}/domains/:domainId/component-types`, () => HttpResponse.json([])),
  http.get(`${testApiBaseUrl}/domains/:domainId/components`, ({ request }) => {
    const url = new URL(request.url);
    return HttpResponse.json({
      items: [],
      page: Number(url.searchParams.get('page') ?? 0),
      size: Number(url.searchParams.get('size') ?? 12),
      totalItems: 0,
    });
  }),
);
