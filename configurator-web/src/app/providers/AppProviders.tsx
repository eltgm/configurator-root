import { createTheme, localStorageColorSchemeManager, MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider } from '@tanstack/react-query';
import type { PropsWithChildren } from 'react';
import { I18nextProvider } from 'react-i18next';

import { colorSchemeStorageKey } from '@/shared/config/preferences';
import { i18n } from '@/shared/i18n/i18n';
import { queryClient } from '@/shared/query/query-client';

const theme = createTheme({
  primaryColor: 'indigo',
  defaultRadius: 'md',
  fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, sans-serif',
  headings: {
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, sans-serif',
  },
});

const colorSchemeManager = localStorageColorSchemeManager({
  key: colorSchemeStorageKey,
});

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <I18nextProvider i18n={i18n}>
      <MantineProvider
        theme={theme}
        defaultColorScheme="auto"
        colorSchemeManager={colorSchemeManager}
      >
        <QueryClientProvider client={queryClient}>
          <Notifications position="top-right" limit={3} />
          {children}
        </QueryClientProvider>
      </MantineProvider>
    </I18nextProvider>
  );
}
