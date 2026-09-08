import { describe, expect, it } from 'vitest';

import {
  getInventoryShortages,
  getSelectableQuantity,
} from '@/features/configurations/model/configuration-inventory';

describe('configuration inventory', () => {
  it('detects a shortage against globally available stock', () => {
    expect(
      getInventoryShortages([
        { id: 1, quantity: 3, availableQuantity: 2 },
        { id: 2, quantity: 1, availableQuantity: 1 },
      ]),
    ).toEqual([{ id: 1, quantity: 3, availableQuantity: 2, selectableQuantity: 2 }]);
  });

  it('adds a tracked configurations own allocation to its selectable quantity', () => {
    const component = { id: 1, quantity: 4, availableQuantity: 1 };
    const ownAllocations = new Map([[1, 3]]);

    expect(getSelectableQuantity(component, ownAllocations)).toBe(4);
    expect(getInventoryShortages([component], ownAllocations)).toEqual([]);
  });
});
