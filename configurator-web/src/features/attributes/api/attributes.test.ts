import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import {
  attributeKeys,
  fetchAttributeCatalog,
  fetchAttributes,
  sortAttributes,
} from '@/features/attributes/api/attributes';
import type { AttributeDefinition } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const attributes: Array<AttributeDefinition> = [
  {
    id: 3,
    domainId: 7,
    componentTypeId: 11,
    name: 'brand',
    label: 'Бренд',
    dataType: 'STRING',
    isRequired: false,
  },
  {
    id: 2,
    domainId: 7,
    componentTypeId: 11,
    name: 'socket',
    label: 'Сокет',
    dataType: 'ENUM',
    enumValues: ['AM4', 'AM5'],
    isRequired: true,
    orderIndex: 2,
  },
  {
    id: 1,
    domainId: 7,
    componentTypeId: 11,
    name: 'cores',
    label: 'Количество ядер',
    dataType: 'NUMBER',
    isRequired: true,
    orderIndex: 1,
  },
];

describe('attributes API', () => {
  it('loads and sorts attributes for the selected component type', async () => {
    let requestedTypeId: string | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/component-types/:id/attributes`, ({ params }) => {
        requestedTypeId = String(params['id']);
        return HttpResponse.json(attributes);
      }),
    );

    const result = await fetchAttributes(11);

    expect(requestedTypeId).toBe('11');
    expect(result.map((attribute) => attribute.id)).toEqual([1, 2, 3]);
  });

  it('includes domain and type IDs in query keys without mutating source arrays', () => {
    const source = [...attributes];

    expect(attributeKeys.byType(7, 11)).toEqual([
      'domains',
      7,
      'attributes',
      'component-types',
      11,
    ]);
    expect(attributeKeys.catalog(7)).toEqual(['domains', 7, 'attributes', 'catalog']);
    expect(sortAttributes(source).map((attribute) => attribute.id)).toEqual([1, 2, 3]);
    expect(source).toEqual(attributes);
  });

  it('loads the attribute catalog for one domain', async () => {
    let requestedDomainId: string | undefined;
    server.use(
      http.get(`${testApiBaseUrl}/domains/:domainId/attributes`, ({ params }) => {
        requestedDomainId = String(params['domainId']);
        return HttpResponse.json(attributes);
      }),
    );

    const result = await fetchAttributeCatalog(7);

    expect(requestedDomainId).toBe('7');
    expect(result.map((attribute) => attribute.id)).toEqual([1, 2, 3]);
  });
});
