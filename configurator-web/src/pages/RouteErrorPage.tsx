import { Anchor, Button, Center, Container, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import { IconAlertTriangle, IconHome } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link, useRouteError } from 'react-router-dom';

import { getErrorTranslationKey, normalizeApiError } from '@/shared/api/errors';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';

export function RouteErrorPage() {
  const { t } = useTranslation();
  const routeError = normalizeApiError(useRouteError());
  const title = t('routeError.title');
  const errorDescription = t(getErrorTranslationKey(routeError));
  useDocumentTitle(title, t('app.name'));

  return (
    <Container size="sm" py={{ base: 48, sm: 96 }}>
      <Center>
        <Stack align="center" gap="md">
          <ThemeIcon color="red" variant="light" size={64} radius="xl">
            <IconAlertTriangle size={34} aria-hidden="true" />
          </ThemeIcon>
          <Title order={1} ta="center">
            {title}
          </Title>
          <Text c="dimmed" ta="center">
            {errorDescription}. {t('routeError.description')}
          </Text>
          <Button
            component={Link}
            to="/configurator"
            leftSection={<IconHome size={18} aria-hidden="true" />}
          >
            {t('routeError.action')}
          </Button>
          <Anchor href="/" size="sm">
            {t('routeError.reload')}
          </Anchor>
        </Stack>
      </Center>
    </Container>
  );
}
