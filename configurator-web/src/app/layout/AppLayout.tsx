import { Anchor, AppShell, Box, Group, Text, ThemeIcon, Tooltip } from '@mantine/core';
import { IconAssembly, IconDatabase } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet } from 'react-router-dom';

import { DesktopNavigation } from '@/app/layout/DesktopNavigation';
import { MobileNavigation } from '@/app/layout/MobileNavigation';
import { PreferencesMenu } from '@/app/layout/PreferencesMenu';
import classes from '@/app/layout/app-layout.module.css';

export function AppLayout() {
  const { t } = useTranslation();
  const domainLabel = `${t('domain.label')}: ${t('domain.none')}`;

  return (
    <>
      <Anchor className={classes['skip-link']} href="#main-content">
        {t('navigation.skipToContent')}
      </Anchor>

      <AppShell
        header={{ height: 64 }}
        navbar={{ width: 276, breakpoint: 'sm', collapsed: { mobile: true } }}
        footer={{ height: { base: 68, sm: 0 } }}
        padding={{ base: 'md', sm: 'xl' }}
      >
        <AppShell.Header>
          <Group h="100%" px={{ base: 'sm', sm: 'lg' }} justify="space-between" wrap="nowrap">
            <Anchor
              component={Link}
              to="/configurator"
              underline="never"
              c="inherit"
              aria-label={t('app.fullName')}
            >
              <Group gap="sm" wrap="nowrap">
                <ThemeIcon size={38} radius="md" variant="gradient">
                  <IconAssembly size={24} stroke={1.8} />
                </ThemeIcon>
                <Text fw={750} size="lg" visibleFrom="xs">
                  {t('app.name')}
                </Text>
              </Group>
            </Anchor>

            <Group gap="xs" wrap="nowrap">
              <Tooltip label={domainLabel}>
                <Group
                  className={classes['domain-context']}
                  gap="xs"
                  wrap="nowrap"
                  aria-label={domainLabel}
                >
                  <IconDatabase size={20} stroke={1.7} />
                  <Box visibleFrom="sm">
                    <Text size="xs" c="dimmed" lh={1.1}>
                      {t('domain.label')}
                    </Text>
                    <Text size="sm" fw={600} lh={1.3}>
                      {t('domain.none')}
                    </Text>
                  </Box>
                </Group>
              </Tooltip>
              <PreferencesMenu />
            </Group>
          </Group>
        </AppShell.Header>

        <AppShell.Navbar>
          <DesktopNavigation />
        </AppShell.Navbar>

        <AppShell.Main id="main-content" className={classes.main}>
          <Outlet />
        </AppShell.Main>

        <AppShell.Footer hiddenFrom="sm">
          <MobileNavigation />
        </AppShell.Footer>
      </AppShell>
    </>
  );
}
