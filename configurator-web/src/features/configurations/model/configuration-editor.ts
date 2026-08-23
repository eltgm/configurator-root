import type {
  Configuration,
  ConfigurationComponent,
  UpdateConfigurationRequest,
} from '@/shared/api';

export const configurationComponentLimit = 50;

export interface ConfigurationEditorValues {
  name: string;
  description: string;
}

export type ConfigurationEditorComponent = ConfigurationComponent;

export type ConfigurationEditorValidationState =
  'idle' | 'pending' | 'direct' | 'transitive' | 'conflict' | 'error';

export type ConfigurationEditorBlockReason =
  'empty' | 'limit' | 'archived' | 'pending' | 'transitive' | 'conflict' | 'error';

export type ConfigurationEditorEligibility =
  { allowed: true } | { allowed: false; reason: ConfigurationEditorBlockReason };

export function configurationEditorInitialValues(
  configuration: Configuration,
): ConfigurationEditorValues {
  return {
    name: configuration.name,
    description: configuration.description ?? '',
  };
}

export function configurationEditorInitialComponents(
  configuration: Configuration,
): ConfigurationEditorComponent[] {
  return configuration.components.map((component) => ({ ...component }));
}

export function addConfigurationEditorComponent(
  components: ReadonlyArray<ConfigurationEditorComponent>,
  component: ConfigurationEditorComponent,
) {
  if (
    components.length >= configurationComponentLimit ||
    components.some(
      (candidate) =>
        candidate.id === component.id || candidate.componentTypeId === component.componentTypeId,
    )
  ) {
    return [...components];
  }
  return [...components, component];
}

export function replaceConfigurationEditorComponent(
  components: ReadonlyArray<ConfigurationEditorComponent>,
  replacedComponentId: number,
  component: ConfigurationEditorComponent,
) {
  const replaced = components.find((candidate) => candidate.id === replacedComponentId);
  if (!replaced || replaced.componentTypeId !== component.componentTypeId) {
    return [...components];
  }
  return components.map((candidate) =>
    candidate.id === replacedComponentId ? component : candidate,
  );
}

export function removeConfigurationEditorComponent(
  components: ReadonlyArray<ConfigurationEditorComponent>,
  componentId: number,
) {
  return components.filter((component) => component.id !== componentId);
}

export function configurationComponentIds(components: ReadonlyArray<ConfigurationEditorComponent>) {
  return components.map((component) => component.id);
}

function normalizedComponentIds(componentIds: ReadonlyArray<number>) {
  return [...componentIds].sort((left, right) => left - right);
}

export function configurationComponentsChanged(
  baseline: ReadonlyArray<ConfigurationEditorComponent>,
  current: ReadonlyArray<ConfigurationEditorComponent>,
) {
  const baselineIds = normalizedComponentIds(configurationComponentIds(baseline));
  const currentIds = normalizedComponentIds(configurationComponentIds(current));
  return (
    baselineIds.length !== currentIds.length ||
    baselineIds.some((componentId, index) => componentId !== currentIds[index])
  );
}

export function getConfigurationEditorEligibility(
  components: ReadonlyArray<ConfigurationEditorComponent>,
  validationState: ConfigurationEditorValidationState,
): ConfigurationEditorEligibility {
  if (components.length === 0) {
    return { allowed: false, reason: 'empty' };
  }
  if (components.length > configurationComponentLimit) {
    return { allowed: false, reason: 'limit' };
  }
  if (components.some((component) => component.archived)) {
    return { allowed: false, reason: 'archived' };
  }
  if (components.length === 1) {
    return { allowed: true };
  }
  if (validationState === 'direct') {
    return { allowed: true };
  }
  if (validationState === 'idle' || validationState === 'pending') {
    return { allowed: false, reason: 'pending' };
  }
  return { allowed: false, reason: validationState };
}

export function toUpdateConfigurationRequest(
  values: ConfigurationEditorValues,
  components: ReadonlyArray<ConfigurationEditorComponent>,
): UpdateConfigurationRequest {
  const description = values.description.trim();
  return {
    name: values.name.trim(),
    ...(description ? { description } : {}),
    componentIds: configurationComponentIds(components),
  };
}
