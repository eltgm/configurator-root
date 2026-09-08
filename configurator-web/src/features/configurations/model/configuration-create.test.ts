import { describe, expect, it } from 'vitest';

import {
  getConfigurationSaveEligibility,
  toCreateConfigurationRequest,
} from '@/features/configurations/model/configuration-create';

describe('configuration create model', () => {
  it('allows only a non-empty valid connected assembly', () => {
    expect(getConfigurationSaveEligibility(1, 'valid')).toEqual({ allowed: true });
    expect(getConfigurationSaveEligibility(2, 'valid')).toEqual({ allowed: true });
    expect(getConfigurationSaveEligibility(0, 'valid')).toEqual({
      allowed: false,
      reason: 'empty',
    });

    for (const reason of ['pending', 'conflict', 'disconnected', 'blocked', 'error'] as const) {
      expect(getConfigurationSaveEligibility(2, reason)).toEqual({ allowed: false, reason });
    }
  });

  it('trims metadata, omits a blank description and snapshots component quantities', () => {
    const components = [
      { componentId: 7, quantity: 2 },
      { componentId: 3, quantity: 1 },
    ];
    const request = toCreateConfigurationRequest(
      { name: '  Home PC  ', description: '  Quiet build  ', trackInventory: true },
      components,
    );
    expect(request).toEqual({
      name: 'Home PC',
      description: 'Quiet build',
      components,
      trackInventory: true,
    });
    expect(
      toCreateConfigurationRequest(
        { name: ' PC ', description: '  ', trackInventory: false },
        components,
      ),
    ).toEqual({ name: 'PC', components, trackInventory: false });

    components.push({ componentId: 9, quantity: 1 });
    expect(request.components).toEqual([
      { componentId: 7, quantity: 2 },
      { componentId: 3, quantity: 1 },
    ]);
  });
});
