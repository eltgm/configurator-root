import {
  IconAdjustments,
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
import { NotFoundPage } from '@/pages/NotFoundPage';
import { RoutePlaceholder } from '@/pages/RoutePlaceholder';

export const appRoutes: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/configurator" replace /> },
      {
        path: 'configurator',
        element: (
          <RoutePlaceholder
            icon={IconAssembly}
            titleKey="pages.configurator.title"
            descriptionKey="pages.configurator.description"
          />
        ),
      },
      {
        path: 'components',
        element: (
          <RoutePlaceholder
            icon={IconBox}
            titleKey="pages.components.title"
            descriptionKey="pages.components.description"
          />
        ),
      },
      {
        path: 'configurations',
        element: (
          <RoutePlaceholder
            icon={IconFileDescription}
            titleKey="pages.configurations.title"
            descriptionKey="pages.configurations.description"
          />
        ),
      },
      { path: 'settings', element: <Navigate to="/settings/types" replace /> },
      {
        path: 'settings/types',
        element: (
          <RoutePlaceholder
            icon={IconCategory}
            titleKey="pages.settingsTypes.title"
            descriptionKey="pages.settingsTypes.description"
          />
        ),
      },
      {
        path: 'settings/compatibility/manual',
        element: (
          <RoutePlaceholder
            icon={IconCirclesRelation}
            titleKey="pages.manualCompatibility.title"
            descriptionKey="pages.manualCompatibility.description"
          />
        ),
      },
      {
        path: 'settings/compatibility/rules',
        element: (
          <RoutePlaceholder
            icon={IconListDetails}
            titleKey="pages.compatibilityRules.title"
            descriptionKey="pages.compatibilityRules.description"
          />
        ),
      },
      {
        path: 'settings/compatibility/graph',
        element: (
          <RoutePlaceholder
            icon={IconGitBranch}
            titleKey="pages.compatibilityGraph.title"
            descriptionKey="pages.compatibilityGraph.description"
          />
        ),
      },
      {
        path: 'settings/domain',
        element: (
          <RoutePlaceholder
            icon={IconAdjustments}
            titleKey="pages.domainSettings.title"
            descriptionKey="pages.domainSettings.description"
          />
        ),
      },
      {
        path: '*',
        element: <NotFoundPage icon={IconSettings} />,
      },
    ],
  },
];
