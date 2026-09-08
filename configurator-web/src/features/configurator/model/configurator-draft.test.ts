import { describe, expect, it } from 'vitest';

import {
  addConfiguratorDraftItem,
  configuratorDraftMaxItems,
  readConfiguratorDraft,
  removeConfiguratorDraftItem,
  replaceConfiguratorDraftItem,
  setConfiguratorDraftItemQuantity,
  writeConfiguratorDraft,
} from '@/features/configurator/model/configurator-draft';
import { configuratorDraftStorageKey } from '@/shared/config/preferences';

const domainId = 101;
const first = { componentId: 11, componentTypeId: 1, quantity: 1 };
const replacement = { componentId: 12, componentTypeId: 1, quantity: 1 };
const second = { componentId: 21, componentTypeId: 2, quantity: 1 };

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
        true,
        adapter,
        () => new Date('2026-08-23T12:00:00Z'),
      ),
    ).toEqual({ persisted: true, updatedAt: '2026-08-23T12:00:00.000Z' });

    expect(storage.get(configuratorDraftStorageKey(domainId))).toBe(
      JSON.stringify({
        version: 3,
        updatedAt: '2026-08-23T12:00:00.000Z',
        items: [first, second],
        trackInventory: true,
      }),
    );
    expect(readConfiguratorDraft(domainId, adapter)).toEqual({
      status: 'restored',
      draft: {
        items: [first, second],
        trackInventory: true,
        updatedAt: '2026-08-23T12:00:00.000Z',
      },
    });
    expect(readConfiguratorDraft(202, adapter).status).toBe('empty');
  });

  it.each([
    'not-json',
    JSON.stringify({ version: 4, updatedAt: '2026-08-23T12:00:00Z', items: [] }),
    JSON.stringify({ version: 1, updatedAt: 'invalid', items: [] }),
    JSON.stringify({ version: 1, updatedAt: '2026-08-23T12:00:00Z', items: [first, first] }),
    JSON.stringify({
      version: 2,
      updatedAt: '2026-08-23T12:00:00Z',
      items: [{ ...first, quantity: 0 }],
    }),
  ])('recovers from malformed or incompatible data: %s', (raw) => {
    expect(readConfiguratorDraft(domainId, { getItem: () => raw })).toEqual({
      status: 'invalid',
      draft: { items: [], trackInventory: false, updatedAt: null },
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
      writeConfiguratorDraft(domainId, [first], false, {
        setItem: () => {
          throw new DOMException('Quota exceeded');
        },
      }),
    ).toEqual({ persisted: false, updatedAt: null });
  });

  it('migrates legacy drafts with quantity tracking disabled without losing quantities', () => {
    const legacy = JSON.stringify({
      version: 2,
      updatedAt: '2026-08-23T12:00:00Z',
      items: [{ ...first, quantity: 3 }],
    });

    expect(readConfiguratorDraft(domainId, { getItem: () => legacy })).toEqual({
      status: 'restored',
      draft: {
        items: [{ ...first, quantity: 3 }],
        trackInventory: false,
        updatedAt: '2026-08-23T12:00:00Z',
      },
    });
  });
});

describe('configurator draft operations', () => {
  it('adds models in selection order and increments the same model quantity', () => {
    expect(addConfiguratorDraftItem([], first)).toEqual({ status: 'added', items: [first] });
    expect(addConfiguratorDraftItem([first], first)).toEqual({
      status: 'quantity-increased',
      items: [{ ...first, quantity: 2 }],
    });
    expect(addConfiguratorDraftItem([first], second)).toEqual({
      status: 'added',
      items: [first, second],
    });
  });

  it('allows several models of the same type and preserves quantity on replacement', () => {
    expect(addConfiguratorDraftItem([first, second], replacement)).toEqual({
      status: 'added',
      items: [first, second, replacement],
    });
    expect(replaceConfiguratorDraftItem([first, second], first.componentId, replacement)).toEqual([
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
      quantity: 1,
    }));
    expect(addConfiguratorDraftItem(full, { componentId: 1000, componentTypeId: 1000 })).toEqual({
      status: 'limit-reached',
      items: full,
    });
  });

  it('normalizes an explicitly edited quantity', () => {
    expect(setConfiguratorDraftItemQuantity([first], first.componentId, 4)).toEqual([
      { ...first, quantity: 4 },
    ]);
  });
});
