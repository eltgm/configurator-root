import { describe, expect, it } from 'vitest';

import {
  canCopyConfiguration,
  configurationExportFileName,
  createConfigurationCopyName,
  getConfigurationCopyInitialValues,
  serializeConfigurationExport,
} from '@/features/configurations/model/configuration-operations';
import type { Configuration, ConfigurationExport } from '@/shared/api';

const configuration: Configuration = {
  id: 41,
  domainId: 7,
  name: 'Home PC',
  description: 'Quiet build',
  createdAt: '2026-08-23T10:00:00Z',
  components: [
    {
      id: 3,
      name: 'Ryzen',
      componentTypeId: 11,
      componentTypeName: 'CPU',
      archived: false,
    },
  ],
};

describe('configuration operations', () => {
  it('creates localized copy metadata and preserves the suffix at the name limit', () => {
    const suffix = ' — copy';
    const longName = 'A'.repeat(255);

    expect(createConfigurationCopyName('Home PC', suffix)).toBe('Home PC — copy');
    expect(createConfigurationCopyName(longName, suffix)).toHaveLength(255);
    expect(createConfigurationCopyName(longName, suffix).endsWith(suffix)).toBe(true);
    expect(getConfigurationCopyInitialValues(configuration, suffix)).toEqual({
      name: 'Home PC — copy',
      description: 'Quiet build',
    });
  });

  it('blocks only archived snapshots from entering the copy flow', () => {
    expect(canCopyConfiguration(configuration)).toBe(true);
    expect(
      canCopyConfiguration({
        ...configuration,
        components: [{ ...configuration.components[0]!, archived: true }],
      }),
    ).toBe(false);
  });

  it('serializes the exact server export as pretty JSON with a trailing newline', () => {
    const exported: ConfigurationExport = {
      schemaVersion: 1,
      exportedAt: '2026-08-23T12:00:00Z',
      configuration,
    };

    expect(configurationExportFileName(41)).toBe('configuration-41.json');
    expect(serializeConfigurationExport(exported)).toBe(`${JSON.stringify(exported, null, 2)}\n`);
  });
});
