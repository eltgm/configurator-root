import { Anchor, AppShell, Group, Text, ThemeIcon } from '@mantine/core';
import { IconAssembly } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet } from 'react-router-dom';

import { DesktopNavigation } from '@/app/layout/DesktopNavigation';
import { MobileNavigation } from '@/app/layout/MobileNavigation';
import { PreferencesMenu } from '@/app/layout/PreferencesMenu';
import classes from '@/app/layout/app-layout.module.css';
import { DomainProvider } from '@/features/domains/model/DomainProvider';
import { DomainSelector } from '@/features/domains/ui/DomainSelector';

export function AppLayout() {
  return (
    <DomainProvider>
      <AppLayoutContent />
    </DomainProvider>
  );
}

function AppLayoutContent() {
  const { t } = useTranslation();

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
              <DomainSelector />
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
