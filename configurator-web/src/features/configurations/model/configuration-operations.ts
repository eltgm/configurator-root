import type { Configuration, ConfigurationExport } from '@/shared/api';

import type { ConfigurationFormValues } from './configuration-create';

export const configurationNameMaxLength = 255;

export function createConfigurationCopyName(sourceName: string, suffix: string): string {
  if (suffix.length >= configurationNameMaxLength) {
    return suffix.slice(-configurationNameMaxLength);
  }

  return `${sourceName.slice(0, configurationNameMaxLength - suffix.length)}${suffix}`;
}

export function getConfigurationCopyInitialValues(
  configuration: Configuration,
  suffix: string,
): ConfigurationFormValues {
  return {
    name: createConfigurationCopyName(configuration.name, suffix),
    description: configuration.description ?? '',
  };
}

export function canCopyConfiguration(configuration: Configuration): boolean {
  return configuration.components.every((component) => !component.archived);
}

export function configurationExportFileName(configurationId: number): string {
  return `configuration-${configurationId}.json`;
}

export function serializeConfigurationExport(configurationExport: ConfigurationExport): string {
  return `${JSON.stringify(configurationExport, null, 2)}\n`;
}
