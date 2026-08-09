import {
  IconAssembly,
  IconBox,
  IconCategory,
  IconCirclesRelation,
  IconFileDescription,
  IconGitBranch,
  IconListDetails,
  IconSettings,
} from '@tabler/icons-react';
import { Navigate, type RouteObject } from 'react-router-dom';

import { AppLayout } from '@/app/layout/AppLayout';
import { RequireDomain } from '@/features/domains/ui/RequireDomain';
import { DomainSettingsPage } from '@/pages/DomainSettingsPage';
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
            <RoutePlaceholder
              icon={IconAssembly}
              titleKey="pages.configurator.title"
              descriptionKey="pages.configurator.description"
            />
          </RequireDomain>
        ),
      },
      {
        path: 'components',
        element: (
          <RequireDomain>
            <RoutePlaceholder
              icon={IconBox}
              titleKey="pages.components.title"
              descriptionKey="pages.components.description"
            />
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
            <RoutePlaceholder
              icon={IconCategory}
              titleKey="pages.settingsTypes.title"
              descriptionKey="pages.settingsTypes.description"
            />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/manual',
        element: (
          <RequireDomain>
            <RoutePlaceholder
              icon={IconCirclesRelation}
              titleKey="pages.manualCompatibility.title"
              descriptionKey="pages.manualCompatibility.description"
            />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/rules',
        element: (
          <RequireDomain>
            <RoutePlaceholder
              icon={IconListDetails}
              titleKey="pages.compatibilityRules.title"
              descriptionKey="pages.compatibilityRules.description"
            />
          </RequireDomain>
        ),
      },
      {
        path: 'settings/compatibility/graph',
        element: (
          <RequireDomain>
            <RoutePlaceholder
              icon={IconGitBranch}
              titleKey="pages.compatibilityGraph.title"
              descriptionKey="pages.compatibilityGraph.description"
            />
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
