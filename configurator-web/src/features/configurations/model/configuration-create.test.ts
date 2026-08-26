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

  it('trims metadata, omits a blank description and snapshots component ids', () => {
    const componentIds = [7, 3];
    const request = toCreateConfigurationRequest(
      { name: '  Home PC  ', description: '  Quiet build  ' },
      componentIds,
    );
    expect(request).toEqual({ name: 'Home PC', description: 'Quiet build', componentIds: [7, 3] });
    expect(toCreateConfigurationRequest({ name: ' PC ', description: '  ' }, componentIds)).toEqual(
      {
        name: 'PC',
        componentIds: [7, 3],
      },
    );

    componentIds.push(9);
    expect(request.componentIds).toEqual([7, 3]);
  });
});
