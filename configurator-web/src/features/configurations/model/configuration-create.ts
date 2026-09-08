import type { ConfigurationComponentInput, CreateConfigurationRequest } from '@/shared/api';

export type ConfigurationSaveBlockReason =
  'empty' | 'pending' | 'conflict' | 'disconnected' | 'blocked' | 'error';

export type ConfigurationCompatibilityState = ConfigurationSaveBlockReason | 'valid';

export type ConfigurationSaveEligibility =
  { allowed: true } | { allowed: false; reason: ConfigurationSaveBlockReason };

export interface ConfigurationFormValues {
  name: string;
  description: string;
  trackInventory: boolean;
}

export function getConfigurationSaveEligibility(
  componentCount: number,
  state: ConfigurationCompatibilityState,
): ConfigurationSaveEligibility {
  if (componentCount === 0 || state === 'empty') {
    return { allowed: false, reason: 'empty' };
  }
  return state === 'valid' ? { allowed: true } : { allowed: false, reason: state };
}

export function toCreateConfigurationRequest(
  values: ConfigurationFormValues,
  components: ReadonlyArray<ConfigurationComponentInput>,
): CreateConfigurationRequest {
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    ...(description ? { description } : {}),
    components: components.map((component) => ({ ...component })),
    trackInventory: values.trackInventory,
  };
}
