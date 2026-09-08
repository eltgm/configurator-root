import type { ConfigurationComponentInput, CreateConfigurationRequest } from '@/shared/api';

export type ConfigurationSaveBlockReason =
  'empty' | 'pending' | 'conflict' | 'disconnected' | 'blocked' | 'inventory' | 'error';

export type ConfigurationCompatibilityState = ConfigurationSaveBlockReason | 'valid';

export type ConfigurationSaveEligibility =
  { allowed: true } | { allowed: false; reason: ConfigurationSaveBlockReason };

export interface ConfigurationFormValues {
  name: string;
  description: string;
}

export function getConfigurationSaveEligibility(
  componentCount: number,
  state: ConfigurationCompatibilityState,
  inventoryValid = true,
): ConfigurationSaveEligibility {
  if (componentCount === 0 || state === 'empty') {
    return { allowed: false, reason: 'empty' };
  }
  if (state !== 'valid') {
    return { allowed: false, reason: state };
  }
  return inventoryValid ? { allowed: true } : { allowed: false, reason: 'inventory' };
}

export function toCreateConfigurationRequest(
  values: ConfigurationFormValues,
  components: ReadonlyArray<ConfigurationComponentInput>,
  trackInventory: boolean,
): CreateConfigurationRequest {
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    ...(description ? { description } : {}),
    components: components.map((component) => ({ ...component })),
    trackInventory,
  };
}
