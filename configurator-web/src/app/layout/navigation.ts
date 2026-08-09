import {
  IconAssembly,
  IconBox,
  IconCategory,
  IconCirclesRelation,
  IconFileDescription,
  IconGitBranch,
  IconListDetails,
  IconSettings,
  IconAdjustments,
  type TablerIcon,
} from '@tabler/icons-react';

export interface NavigationItem {
  path: string;
  labelKey: string;
  icon: TablerIcon;
  matchPrefix?: boolean;
}

export const primaryNavigation: NavigationItem[] = [
  {
    path: '/configurator',
    labelKey: 'navigation.configurator',
    icon: IconAssembly,
  },
  {
    path: '/components',
    labelKey: 'navigation.components',
    icon: IconBox,
  },
  {
    path: '/configurations',
    labelKey: 'navigation.configurations',
    icon: IconFileDescription,
  },
];

export const settingsNavigation: NavigationItem[] = [
  {
    path: '/settings/types',
    labelKey: 'navigation.types',
    icon: IconCategory,
  },
  {
    path: '/settings/compatibility/manual',
    labelKey: 'navigation.manualCompatibility',
    icon: IconCirclesRelation,
  },
  {
    path: '/settings/compatibility/rules',
    labelKey: 'navigation.compatibilityRules',
    icon: IconListDetails,
  },
  {
    path: '/settings/compatibility/graph',
    labelKey: 'navigation.compatibilityGraph',
    icon: IconGitBranch,
  },
  {
    path: '/settings/domain',
    labelKey: 'navigation.domainSettings',
    icon: IconAdjustments,
  },
];

export const mobileNavigation: NavigationItem[] = [
  ...primaryNavigation,
  {
    path: '/settings/types',
    labelKey: 'navigation.settings',
    icon: IconSettings,
    matchPrefix: true,
  },
];

export function isNavigationItemActive(item: NavigationItem, pathname: string) {
  return item.matchPrefix ? pathname.startsWith('/settings') : pathname === item.path;
}
