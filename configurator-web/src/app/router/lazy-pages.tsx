import { lazy } from 'react';

export const AttributesPage = lazy(() =>
  import('@/pages/AttributesPage').then(({ AttributesPage }) => ({ default: AttributesPage })),
);

export const ComponentsPage = lazy(() =>
  import('@/pages/ComponentsPage').then(({ ComponentsPage }) => ({ default: ComponentsPage })),
);
export const ComponentDetailsPage = lazy(() =>
  import('@/pages/ComponentDetailsPage').then(({ ComponentDetailsPage }) => ({
    default: ComponentDetailsPage,
  })),
);
export const ComponentFormPage = lazy(() =>
  import('@/pages/ComponentFormPage').then(({ ComponentFormPage }) => ({
    default: ComponentFormPage,
  })),
);
export const ComponentTypesPage = lazy(() =>
  import('@/pages/ComponentTypesPage').then(({ ComponentTypesPage }) => ({
    default: ComponentTypesPage,
  })),
);
export const CompatibilityGraphPage = lazy(() =>
  import('@/pages/CompatibilityGraphPage').then(({ CompatibilityGraphPage }) => ({
    default: CompatibilityGraphPage,
  })),
);
export const CompatibilityRuleFormPage = lazy(() =>
  import('@/pages/CompatibilityRuleFormPage').then(({ CompatibilityRuleFormPage }) => ({
    default: CompatibilityRuleFormPage,
  })),
);
export const CompatibilityRulesPage = lazy(() =>
  import('@/pages/CompatibilityRulesPage').then(({ CompatibilityRulesPage }) => ({
    default: CompatibilityRulesPage,
  })),
);
export const ConfiguratorPage = lazy(() =>
  import('@/pages/ConfiguratorPage').then(({ ConfiguratorPage }) => ({
    default: ConfiguratorPage,
  })),
);
export const ConfigurationDetailsPage = lazy(() =>
  import('@/pages/ConfigurationDetailsPage').then(({ ConfigurationDetailsPage }) => ({
    default: ConfigurationDetailsPage,
  })),
);
export const ConfigurationEditPage = lazy(() =>
  import('@/pages/ConfigurationEditPage').then(({ ConfigurationEditPage }) => ({
    default: ConfigurationEditPage,
  })),
);
export const ConfigurationsPage = lazy(() =>
  import('@/pages/ConfigurationsPage').then(({ ConfigurationsPage }) => ({
    default: ConfigurationsPage,
  })),
);
export const DomainSettingsPage = lazy(() =>
  import('@/pages/DomainSettingsPage').then(({ DomainSettingsPage }) => ({
    default: DomainSettingsPage,
  })),
);
export const ManualCompatibilityPage = lazy(() =>
  import('@/pages/ManualCompatibilityPage').then(({ ManualCompatibilityPage }) => ({
    default: ManualCompatibilityPage,
  })),
);
