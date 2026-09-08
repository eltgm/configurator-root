import { describe, expect, it } from 'vitest';

import {
  addConfigurationEditorComponent,
  configurationComponentsChanged,
  configurationEditorInitialComponents,
  configurationEditorInitialValues,
  getConfigurationEditorEligibility,
  removeConfigurationEditorComponent,
  replaceConfigurationEditorComponent,
  setConfigurationEditorComponentQuantity,
  toUpdateConfigurationRequest,
  type ConfigurationEditorComponent,
} from '@/features/configurations/model/configuration-editor';
import type { Configuration } from '@/shared/api';

const processor: ConfigurationEditorComponent = {
  id: 1,
  name: 'CPU',
  componentTypeId: 10,
  componentTypeName: 'Processor',
  archived: false,
  quantity: 1,
};
const board: ConfigurationEditorComponent = {
  id: 2,
  name: 'Board',
  componentTypeId: 20,
  componentTypeName: 'Motherboard',
  archived: false,
  quantity: 1,
};
const otherBoard: ConfigurationEditorComponent = { ...board, id: 3, name: 'Other board' };

describe('configuration editor model', () => {
  it('creates independent values and composition from a configuration', () => {
    const configuration: Configuration = {
      id: 7,
      domainId: 1,
      name: ' Workstation ',
      description: ' Quiet ',
      createdAt: '2026-08-23T10:00:00Z',
      trackInventory: true,
      components: [processor],
    };

    expect(configurationEditorInitialValues(configuration)).toEqual({
      name: ' Workstation ',
      description: ' Quiet ',
      trackInventory: true,
    });
    const components = configurationEditorInitialComponents(configuration);
    expect(components).toEqual([processor]);
    expect(components[0]).not.toBe(configuration.components[0]);
  });

  it('adds different models of the same type, removes them and replaces an exact model', () => {
    expect(addConfigurationEditorComponent([processor], board)).toEqual([processor, board]);
    expect(addConfigurationEditorComponent([processor, board], otherBoard)).toEqual([
      processor,
      board,
      otherBoard,
    ]);
    expect(replaceConfigurationEditorComponent([processor, board], 2, otherBoard)).toEqual([
      processor,
      otherBoard,
    ]);
    expect(replaceConfigurationEditorComponent([processor, board], 1, otherBoard)).toEqual([
      otherBoard,
      board,
    ]);
    expect(removeConfigurationEditorComponent([processor, board], 1)).toEqual([board]);
  });

  it('keeps existing positions for a duplicate or missing replacement and normalizes quantities', () => {
    expect(addConfigurationEditorComponent([processor], processor)).toEqual([processor]);
    expect(replaceConfigurationEditorComponent([processor], board.id, board)).toEqual([processor]);
    expect(setConfigurationEditorComponentQuantity([processor], processor.id, 0)).toEqual([
      { ...processor, quantity: 1 },
    ]);
    expect(setConfigurationEditorComponentQuantity([processor], processor.id, 1_000_000)).toEqual([
      { ...processor, quantity: 999_999 },
    ]);
  });

  it('compares composition as a set instead of display order', () => {
    expect(configurationComponentsChanged([processor, board], [board, processor])).toBe(false);
    expect(configurationComponentsChanged([processor, board], [processor, otherBoard])).toBe(true);
  });

  it('allows one active component and a valid connected assembly', () => {
    expect(getConfigurationEditorEligibility([processor], 'idle')).toEqual({ allowed: true });
    expect(getConfigurationEditorEligibility([processor, board], 'valid')).toEqual({
      allowed: true,
    });
  });

  it('blocks a composition above the server limit', () => {
    const components = Array.from({ length: 51 }, (_, index) => ({
      ...processor,
      id: index + 1,
      componentTypeId: index + 1,
    }));
    expect(getConfigurationEditorEligibility(components, 'valid')).toEqual({
      allowed: false,
      reason: 'limit',
    });
  });

  it.each([
    [[], 'idle', 'empty'],
    [[{ ...processor, archived: true }], 'valid', 'archived'],
    [[processor, board], 'idle', 'pending'],
    [[processor, board], 'pending', 'pending'],
    [[processor, board], 'blocked', 'blocked'],
    [[processor, board], 'disconnected', 'disconnected'],
    [[processor, board], 'error', 'error'],
  ] as const)('blocks invalid state %#', (components, state, reason) => {
    expect(getConfigurationEditorEligibility(components, state)).toEqual({
      allowed: false,
      reason,
    });
  });

  it('normalizes a complete update request', () => {
    expect(
      toUpdateConfigurationRequest(
        { name: '  Updated  ', description: '   ', trackInventory: true },
        [processor, board],
      ),
    ).toEqual({
      name: 'Updated',
      components: [
        { componentId: 1, quantity: 1 },
        { componentId: 2, quantity: 1 },
      ],
      trackInventory: true,
    });
  });
});
