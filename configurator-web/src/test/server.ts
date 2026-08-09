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
);
