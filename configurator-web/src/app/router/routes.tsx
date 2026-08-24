import { IconSettings } from '@tabler/icons-react';
import { Navigate, type RouteObject } from 'react-router-dom';

import { AppLayout } from '@/app/layout/AppLayout';
import {
  CompatibilityGraphPage,
  CompatibilityRuleFormPage,
  CompatibilityRulesPage,
  ComponentDetailsPage,
  ComponentFormPage,
  ComponentsPage,
  ComponentTypesPage,
  ConfigurationDetailsPage,
  ConfigurationEditPage,
  ConfigurationsPage,
  ConfiguratorPage,
  DomainSettingsPage,
  ManualCompatibilityPage,
} from '@/app/router/lazy-pages';
import { RequireDomain } from '@/features/domains/ui/RequireDomain';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { RouteErrorPage } from '@/pages/RouteErrorPage';

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
            <ConfigurationsPage />
          </RequireDomain>
        ),
      },
      {
        path: 'configurations/:configurationId',
        element: (
          <RequireDomain>
            <ConfigurationDetailsPage />
          </RequireDomain>
        ),
      },
      {
        path: 'configurations/:configurationId/edit',
        element: (
          <RequireDomain>
            <ConfigurationEditPage />
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
