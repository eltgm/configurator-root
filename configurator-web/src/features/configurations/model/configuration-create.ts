import type { CreateConfigurationRequest } from '@/shared/api';

export type ConfigurationSaveBlockReason =
  'empty' | 'pending' | 'transitive' | 'conflict' | 'blocked' | 'error';

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
): ConfigurationSaveEligibility {
  if (componentCount === 0 || state === 'empty') {
    return { allowed: false, reason: 'empty' };
  }
  return state === 'valid' ? { allowed: true } : { allowed: false, reason: state };
}

export function toCreateConfigurationRequest(
  values: ConfigurationFormValues,
  componentIds: ReadonlyArray<number>,
): CreateConfigurationRequest {
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    ...(description ? { description } : {}),
    componentIds: [...componentIds],
  };
}
