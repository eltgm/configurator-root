import { describe, expect, it } from 'vitest';

import {
  addConfiguratorDraftItem,
  configuratorDraftMaxItems,
  readConfiguratorDraft,
  removeConfiguratorDraftItem,
  replaceConfiguratorDraftItem,
  writeConfiguratorDraft,
} from '@/features/configurator/model/configurator-draft';
import { configuratorDraftStorageKey } from '@/shared/config/preferences';

const domainId = 101;
const first = { componentId: 11, componentTypeId: 1 };
const replacement = { componentId: 12, componentTypeId: 1 };
const second = { componentId: 21, componentTypeId: 2 };

describe('configurator draft persistence', () => {
  it('round-trips an ordered versioned draft under a domain-scoped key', () => {
    const storage = new Map<string, string>();
    const adapter = {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
    };
    expect(
      writeConfiguratorDraft(
        domainId,
        [first, second],
        adapter,
        () => new Date('2026-08-23T12:00:00Z'),
      ),
    ).toEqual({ persisted: true, updatedAt: '2026-08-23T12:00:00.000Z' });

    expect(storage.get(configuratorDraftStorageKey(domainId))).toBe(
      JSON.stringify({
        version: 1,
        updatedAt: '2026-08-23T12:00:00.000Z',
        items: [first, second],
      }),
    );
    expect(readConfiguratorDraft(domainId, adapter)).toEqual({
      status: 'restored',
      draft: { items: [first, second], updatedAt: '2026-08-23T12:00:00.000Z' },
    });
    expect(readConfiguratorDraft(202, adapter).status).toBe('empty');
  });

  it.each([
    'not-json',
    JSON.stringify({ version: 2, updatedAt: '2026-08-23T12:00:00Z', items: [] }),
    JSON.stringify({ version: 1, updatedAt: 'invalid', items: [] }),
    JSON.stringify({ version: 1, updatedAt: '2026-08-23T12:00:00Z', items: [first, first] }),
    JSON.stringify({
      version: 1,
      updatedAt: '2026-08-23T12:00:00Z',
      items: [first, replacement],
    }),
  ])('recovers from malformed or incompatible data: %s', (raw) => {
    expect(readConfiguratorDraft(domainId, { getItem: () => raw })).toEqual({
      status: 'invalid',
      draft: { items: [], updatedAt: null },
    });
  });

  it('keeps an in-memory draft usable when storage access fails', () => {
    expect(
      readConfiguratorDraft(domainId, {
        getItem: () => {
          throw new DOMException('Denied');
        },
      }).status,
    ).toBe('unavailable');
    expect(
      writeConfiguratorDraft(domainId, [first], {
        setItem: () => {
          throw new DOMException('Quota exceeded');
        },
      }),
    ).toEqual({ persisted: false, updatedAt: null });
  });
});

describe('configurator draft operations', () => {
  it('adds components in selection order and treats the same component as a no-op', () => {
    expect(addConfiguratorDraftItem([], first)).toEqual({ status: 'added', items: [first] });
    expect(addConfiguratorDraftItem([first], first)).toEqual({
      status: 'already-selected',
      items: [first],
    });
    expect(addConfiguratorDraftItem([first], second)).toEqual({
      status: 'added',
      items: [first, second],
    });
  });

  it('requires an explicit same-type replacement and preserves its position', () => {
    expect(addConfiguratorDraftItem([first, second], replacement)).toEqual({
      status: 'replacement-required',
      items: [first, second],
      replacedItem: first,
    });
    expect(replaceConfiguratorDraftItem([first, second], replacement)).toEqual([
      replacement,
      second,
    ]);
  });

  it('removes one component without reordering the rest', () => {
    expect(removeConfiguratorDraftItem([first, second], first.componentId)).toEqual([second]);
  });

  it('rejects an additional type after the backend-compatible maximum', () => {
    const full = Array.from({ length: configuratorDraftMaxItems }, (_, index) => ({
      componentId: index + 1,
      componentTypeId: index + 101,
    }));
    expect(addConfiguratorDraftItem(full, { componentId: 1000, componentTypeId: 1000 })).toEqual({
      status: 'limit-reached',
      items: full,
    });
  });
});
