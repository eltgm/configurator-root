import { IconFileDescription, IconSettings } from '@tabler/icons-react';
import { Navigate, type RouteObject } from 'react-router-dom';

import { AppLayout } from '@/app/layout/AppLayout';
import { RequireDomain } from '@/features/domains/ui/RequireDomain';
import { ComponentsPage } from '@/pages/ComponentsPage';
import { ComponentDetailsPage } from '@/pages/ComponentDetailsPage';
import { ComponentFormPage } from '@/pages/ComponentFormPage';
import { ComponentTypesPage } from '@/pages/ComponentTypesPage';
import { CompatibilityGraphPage } from '@/pages/CompatibilityGraphPage';
import { CompatibilityRuleFormPage } from '@/pages/CompatibilityRuleFormPage';
import { CompatibilityRulesPage } from '@/pages/CompatibilityRulesPage';
import { ConfiguratorPage } from '@/pages/ConfiguratorPage';
import { DomainSettingsPage } from '@/pages/DomainSettingsPage';
import { ManualCompatibilityPage } from '@/pages/ManualCompatibilityPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { RouteErrorPage } from '@/pages/RouteErrorPage';
import { RoutePlaceholder } from '@/pages/RoutePlaceholder';

export const appRoutes: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <Navigate to="/configurator" replace /> },
      {
        path: 'configurator',
        element: (
          <RequireDomain>
            <ConfiguratorPage />
          </RequireDomain>
        ),
      },
      {
        path: 'components',
        element: (
          <RequireDomain>
            <ComponentsPage />
          </RequireDomain>
        ),
      },
      {
        path: 'components/new',
        element: (
          <RequireDomain>
            <ComponentFormPage />
          </RequireDomain>
        ),
      },
      {
        path: 'components/:componentId',
        element: (
          <RequireDomain>
            <ComponentDetailsPage />
          </RequireDomain>
        ),
      },
      {
        path: 'components/:componentId/edit',
        element: (
          <RequireDomain>
            <ComponentFormPage />
          </RequireDomain>
        ),
      },
      {
        path: 'configurations',
        element: (
          <RequireDomain>
            <RoutePlaceholder
              icon={IconFileDescription}
              titleKey="pages.configurations.title"
              descriptionKey="pages.configurations.description"
            />
          </RequireDomain>
        ),
      },
      { path: 'settings', element: <Navigate to="/settings/types" replace /> },
      {
        path: 'settings/types',
        element: (
          <RequireDomain>
            <ComponentTypesPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/manual',
        element: (
          <RequireDomain>
            <ManualCompatibilityPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/rules',
        element: (
          <RequireDomain>
            <CompatibilityRulesPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/rules/new',
        element: (
          <RequireDomain>
            <CompatibilityRuleFormPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/rules/:ruleId/edit',
        element: (
          <RequireDomain>
            <CompatibilityRuleFormPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/graph',
        element: (
          <RequireDomain>
            <CompatibilityGraphPage />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/domain',
        element: <DomainSettingsPage />,
      },
      {
        path: '*',
        element: <NotFoundPage icon={IconSettings} />,
      },
    ],
  },
];
