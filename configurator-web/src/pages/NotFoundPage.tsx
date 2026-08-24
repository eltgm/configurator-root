import { Button, Center, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import type { TablerIcon } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';

export function NotFoundPage({ icon: Icon }: { icon: TablerIcon }) {
  const { t } = useTranslation();
  const title = t('notFound.title');
  useDocumentTitle(title, t('app.name'));

  return (
    <Center mih="60vh">
      <Stack align="center" ta="center" gap="md">
        <ThemeIcon size={64} radius="xl" variant="light" color="gray">
          <Icon size={34} stroke={1.5} />
        </ThemeIcon>
        <Text fw={800} size="xl" c="dimmed">
          404
        </Text>
        <Title order={1}>{title}</Title>
        <Text c="dimmed">{t('notFound.description')}</Text>
        <Button component={Link} to="/configurator">
          {t('notFound.action')}
        </Button>
      </Stack>
    </Center>
  );
}
