import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import {
  componentTypeKeys,
  fetchComponentTypes,
  sortComponentTypes,
} from '@/features/component-types/api/component-types';
import type { ComponentType } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const types: Array<ComponentType> = [
  { id: 3, domainId: 7, name: 'Без порядка' },
  { id: 2, domainId: 7, name: 'Видеокарта', orderIndex: 2 },
  { id: 1, domainId: 7, name: 'Процессор', orderIndex: 1 },
];

describe('component types API', () => {
  it('loads the selected domain and sorts ordered items before unordered ones', async () => {
    let requestedDomainId: string | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/:id/component-types`, ({ params }) => {
        requestedDomainId = String(params['id']);
        return HttpResponse.json(types);
      }),
    );

    const result = await fetchComponentTypes(7);

    expect(requestedDomainId).toBe('7');
    expect(result.map((type) => type.id)).toEqual([1, 2, 3]);
  });

  it('uses domain-scoped query keys and does not mutate source arrays while sorting', () => {
    const source = [...types];

    expect(componentTypeKeys.byDomain(7)).toEqual(['domains', 7, 'component-types']);
    expect(sortComponentTypes(source).map((type) => type.id)).toEqual([1, 2, 3]);
    expect(source).toEqual(types);
  });
});
